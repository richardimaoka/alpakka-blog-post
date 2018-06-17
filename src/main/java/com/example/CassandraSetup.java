package com.example;

import com.datastax.driver.core.*;

public class CassandraSetup {
  public static void main(String args[]) {
    final Session session = Cluster.builder()
      .addContactPoint("127.0.0.1").withPort(9042)
      .build().connect();

    final Statement createKeyspace = new SimpleStatement(
      "CREATE KEYSPACE IF NOT EXISTS akka_stream_java_test WITH REPLICATION = { 'class' :  'SimpleStrategy', 'replication_factor': 1 };"
    );
    session.execute(createKeyspace);

    final Statement createTable = new SimpleStatement(
      " CREATE TABLE akka_stream_java_test.users (" +
        "id int, " +
        "name text, " +
        "age int, " +
        "PRIMARY KEY (id)" +
        ");"
    );
    session.execute(createTable);

    final PreparedStatement prepared = session.prepare(
      "insert into akka_stream_java_test.users( id, name, age ) values ( ?, ?, ? )"
    );

    for(int i = 1; i <= 1000; i++){
      // For simplicity we use the same name and age for this example
      String name = "John";
      int age = 35;

      // Prepared statement is typical in parameterized queries in CQL (Cassandra Query Language).
      // In production systems, it can be used to guard the statement from injection attacks, similar to SQL prepared statement.
      BoundStatement bound = prepared.bind(i, name, age);
      session.execute(bound);
      System.out.println(String.format("%d / 1000: done", i));
    }

    //https://docs.datastax.com/en/developer/java-driver/3.2/manual/paging/
    final Statement select = new SimpleStatement("SELECT * FROM akka_stream_java_test.users").setFetchSize(100);
    ResultSet rows = session.execute(select);
    for (Row row : rows) {
      System.out.println(row);
    }

    session.close();
  }
}
