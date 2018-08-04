package com.example;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.stream.ActorMaterializer;
import akka.stream.Attributes;
import akka.stream.Materializer;
import akka.stream.OverflowStrategy;
import akka.stream.alpakka.cassandra.javadsl.CassandraFlow;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.datastax.driver.core.*;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.function.BiFunction;

public class CassandraFlowExample {
  public static class UserComment {
    int    userId;
    String comment;

    UserComment(int userId, String comment) {
      this.userId = userId;
      this.comment = comment;
    }

    public String toString() {
      return "UserComment(" + userId + ", " + comment + ")";
    }
  }

  public static void main(String args[]) {
    // Make sure you already brought up Cassandra, which is accessible via the host and port below.
    // The host and port would be driven from a config in a production environment
    // but hardcoding them here for simplicity.

    final ActorSystem system = ActorSystem.create();
    final Materializer materializer = ActorMaterializer.create(system);

    try (Session session = Cluster.builder()
            .addContactPoint("127.0.0.1").withPort(9042)
            .build().connect()) {
      setupCassandra(session);

      final PreparedStatement insertTemplate = session.prepare(
        "INSERT INTO akka_stream_java_test.user_comments (id, user_id, comment) VALUES (uuid(), ?, ?)"
      );

      // A function to create a BoundStatement, from:
      //  - UserComment, input data
      //  - PreparedStatement, template to generate BoundStatement by supplying UserComment
      BiFunction<UserComment, PreparedStatement, BoundStatement> statementBinder =
        (userData, preparedStatement) -> preparedStatement.bind(userData.userId, userData.comment);

      final Flow<UserComment, UserComment, NotUsed> flow =
        CassandraFlow.createWithPassThrough(2, insertTemplate, statementBinder, session, system.dispatcher());

      // OverflowStrategy.fail() might not be appropriate in production, as it makes the entire stream fail on overflow.
      // However, for this example, it highlights an issue quickly when there is something going wrong
      final Source<UserComment, ActorRef> source = Source.actorRef(4, OverflowStrategy.fail());

      // The leftmost materialized value (i.e.) ActorRef from source is returned,
      // due to to() and run() as described below
      final ActorRef actorRef =
        source
          .log("processing").withAttributes(
            Attributes.logLevels(
              Logging.InfoLevel(), //on each element
              Logging.InfoLevel(), //on completion of the stream
              Logging.ErrorLevel() //on failure of the stream
            )
          )
          .via(flow)          //via() takes the left Materialized value
          .to(Sink.ignore())  //to()  takes the left Materialized value
          .run(materializer); //run() takes the left Materialized value

      // In production systems, you can pass around the above `actorRef` to connect the CassandraSink stream to
      // whatever input you like, (e.g.) an HTTP endpoint which forwards UserComment per HTTP request.
      // In this example, the actorRef is connected to a static source locally here, which looks stupid, but easy to understand.
      Source.from(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
        // throttling the stream so that the Source.actorRef() does not overflow
        .throttle(1, Duration.of(50, ChronoUnit.MILLIS))
        .map(i -> new UserComment(i, "some comment"))
        .to(Sink.actorRef(actorRef, "stream completed"))
        .run(materializer);

      // Sleep for 5 seconds, so that the stream finishes running
      Thread.sleep(5000);
      System.out.println("finished");

    } catch(InterruptedException e) {
      System.out.println("Application exited unexpectedly while sleeping.");
      e.printStackTrace();
    } finally {
      system.terminate();
    }
  }

  private static void setupCassandra(Session session){
    // Setup step 1: Firstly make sure the keyspace exists
    // Cassandra keyspace is something that holds tables inside, and defines replication strategies
    final Statement createKeyspace = new SimpleStatement(
      "CREATE KEYSPACE IF NOT EXISTS akka_stream_java_test WITH REPLICATION = { 'class' :  'SimpleStrategy', 'replication_factor': 1 };"
    );
    session.execute(createKeyspace);

    // Step 2: Make sure the target table exists, and empty
    // Dropping and creating the table is the easiest way to make sure the table is empty
    final Statement dropTable = new SimpleStatement(
      "DROP TABLE IF EXISTS akka_stream_java_test.user_comments;"
    );
    final Statement createTable = new SimpleStatement(
      "CREATE TABLE akka_stream_java_test.user_comments (" +
        "id uuid, " +         // same as users table in CassandraSourceExample, use int as simplicity
        // this user_id should match with users.id but there is no concept of foreign key in Cassandra
        // unlike SQL, so no constraint is put in place
        "user_id int, " +
        "comment text, " +
        "PRIMARY KEY (id)" +
        ");"
    );
    session.execute(dropTable);
    session.execute(createTable);
  }
}
