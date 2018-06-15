package com.example;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.alpakka.cassandra.javadsl.CassandraSource;
import akka.stream.javadsl.Sink;
import com.datastax.driver.core.*;

import java.util.List;
import java.util.concurrent.CompletionStage;

public class Main {
  public static void main(String args[]) {
    final Session session = Cluster.builder()
      .addContactPoint("127.0.0.1").withPort(9042)
      .build().connect();

    final ActorSystem system = ActorSystem.create();
    final Materializer materializer = ActorMaterializer.create(system);

    final Statement stmt = new SimpleStatement("SELECT * FROM akka_stream_java_test.test").setFetchSize(20);

    final CompletionStage<List<Row>> rows = CassandraSource.create(stmt, session)
      .runWith(Sink.seq(), materializer);

    System.out.print("yeah");
    System.out.print("yeah");
    System.out.print("yeah");
    System.out.print("yeah");
    System.out.print("yeah");
    System.out.print("yeah");
    System.out.print("yeah");
    System.out.print("yeah");
    System.out.print("yeah");
    System.out.print("yeah");
    System.out.print("yeah");
    System.out.print("yeah");
    System.out.print("yeah");
    System.out.print("yeah");
    System.out.print("yeah");
    System.out.print("yeah");
    System.out.print("yeah");
    System.out.print("yeah");
    System.out.print("yeah");
    System.out.print("yeah");
    System.out.print("yeah");
    System.out.print("yeah");
    System.out.print("yeah");
    System.out.print("yeah");
    System.out.print("yeah");
    System.out.print("yeah");
    System.out.print("yeah");
    System.out.print("yeah");
    System.out.print("yeah");
    System.out.print("yeah");
    System.out.print("yeah");
    System.out.print("yeah");
    System.out.print("yeah");
    System.out.print("yeah");
    System.out.print("yeah");
    System.out.print("yeah");
    System.out.print("yeah");
    System.out.print("yeah");
  }
}
