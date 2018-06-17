package com.example;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.alpakka.cassandra.javadsl.CassandraSource;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.Statement;

public class CassandraSourceExample {
  public static void main(String args[]) {
    final Session session = Cluster.builder()
      .addContactPoint("127.0.0.1").withPort(9042)
      .build().connect();

    final ActorSystem system = ActorSystem.create();
    final Materializer materializer = ActorMaterializer.create(system);

    final Statement stmt = new SimpleStatement("SELECT * FROM akka_stream_java_test.users").setFetchSize(100);

    final RunnableGraph<NotUsed> runnableGraph =
      CassandraSource.create(stmt, session)
        .to(Sink.foreach(row -> System.out.println(row)));

    runnableGraph.run(materializer);
  }
}
