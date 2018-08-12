package com.example;


// Alpakka Cassandra connector
import akka.stream.alpakka.cassandra.javadsl.CassandraSource;

// For Akka and Akka Streams
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;

// For Java Cassandra driver
import com.datastax.driver.core.*;

// For setupCassandraByCassandraSink()
import akka.Done;
import akka.event.Logging;
import akka.stream.Attributes;
import akka.stream.alpakka.cassandra.javadsl.CassandraSink;
import akka.stream.javadsl.StreamConverters;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

public class CassandraSourceExample {
  public static void main(String args[]) {
    // Make sure you already brought up Cassandra, which is accessible via the host and port below.
    // The host and port would be driven from a config in a production environment
    // but hardcoding them here for simplicity.

    // ActorSystem and Materializer are necessary as underlying infrastructure to run Akka Streams
    final ActorSystem system = ActorSystem.create();
    final Materializer materializer = ActorMaterializer.create(system);

    try (Session session = Cluster.builder()
            .addContactPoint("127.0.0.1").withPort(9042)
            .build().connect()) {

      setupCassandra(session);
      // you can also call this instead, which have the same result as setupCassandra()
      //setupCassandraByCassandraSink(session, materializer);

      // https://docs.datastax.com/en/developer/java-driver/3.2/manual/paging/
      final Statement stmt =
        new SimpleStatement("SELECT * FROM akka_stream_java_test.users").setFetchSize(100);

      // Read the 1,000 rows inserted by the setupCassandra() method
      // and print them one by one
      final RunnableGraph<NotUsed> runnableGraph =
        CassandraSource.create(stmt, session)
          .to(Sink.foreach(row -> System.out.println(row)));

      runnableGraph.run(materializer);

      // Let the stream run for a while, before terminating ActorSystem
      Thread.sleep(5000);

    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    finally {
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

    // Step 2: Make sure the target table exists, and empty before the step 3
    // Dropping and creating the table is the easiest way to make sure the table is empty
    final Statement dropTable = new SimpleStatement(
      "DROP TABLE IF EXISTS akka_stream_java_test.users;"
    );
    final Statement createTable = new SimpleStatement(
      "CREATE TABLE akka_stream_java_test.users (" +
        "id int, " + // Typically in Cassandra, UUID type is used for id, but we use int for simplicity
        "name text, " +
        "age int, " +
        "PRIMARY KEY (id)" +
        ");"
    );
    session.execute(dropTable);
    session.execute(createTable);

    // Step 3: Insert the data, 1000 rows into the table
    final PreparedStatement prepared = session.prepare(
      "insert into akka_stream_java_test.users( id, name, age ) values ( ?, ?, ? )"
    );

    IntStream.range(1, 1000).forEach(i -> {
      // For simplicity we use the same name and age in this example
      String name = "John";
      int age = 35;

      // Prepared statement is typical in parameterized queries in CQL (Cassandra Query Language).
      // In production systems, it can be used to guard the statement from injection attacks, similar to SQL prepared statement.
      BoundStatement bound = prepared.bind(i, name, age);
      session.execute(bound);
    });
  }

  /**
   * It does the same thing as setupCassandra(), but using CassandraSink
   */
  private static void setupCassandraByCassandraSink(Session session, Materializer materializer){
    // Setup step 1: Firstly make sure the keyspace exists
    // Cassandra keyspace is something that holds tables inside, and defines replication strategies
    final Statement createKeyspace = new SimpleStatement(
      "CREATE KEYSPACE IF NOT EXISTS akka_stream_java_test WITH REPLICATION = { 'class' :  'SimpleStrategy', 'replication_factor': 1 };"
    );
    session.execute(createKeyspace);

    // Step 2: Make sure the target table exists, and empty before the step 3
    // Dropping and creating the table is the easiest way to make sure the table is empty
    final Statement dropTable = new SimpleStatement(
      "DROP TABLE IF EXISTS akka_stream_java_test.users;"
    );
    final Statement createTable = new SimpleStatement(
      "CREATE TABLE akka_stream_java_test.users (" +
        "id int, " + // Typically in Cassandra, UUID type is used for id, but we use int for simplicity
        "name text, " +
        "age int, " +
        "PRIMARY KEY (id)" +
        ");"
    );
    session.execute(dropTable);
    session.execute(createTable);

    final PreparedStatement insertTemplate = session.prepare(
      "insert into akka_stream_java_test.users( id, name, age ) values ( ?, ?, ? )"
    );

    // A function to create a BoundStatement, from:
    //  - UserComment, input data
    //  - PreparedStatement, template to generate BoundStatement by supplying UserComment
    BiFunction<UserData, PreparedStatement, BoundStatement> statementBinder =
      (userData, preparedStatement) -> preparedStatement.bind(userData.id, userData.name, userData.age);

    final Sink<UserData, CompletionStage<Done>> cassandraSink =
      CassandraSink.create(2, insertTemplate, statementBinder, session);

    // Step 3: Insert the data, 1000 rows into the table
    StreamConverters
      .fromJavaStream(() -> IntStream.range(1, 1000))
      .map(i -> new UserData(i, "John", 35))
      .to(cassandraSink)
      .run(materializer);
  }

  public static class UserData {
    int    id;
    String name;
    int    age;

    UserData(int id, String name, int age) {
      this.id = id;
      this.name = name;
      this.age = age;
    }

    public String toString() {
      return "UserData(" + id  + ", " + name + ", " + age + ")";
    }
  }

}
