## Background

Enterprise Integration Patterns, EIP, provide reusable architecture patterns to construct complicated systems from simple components.
It has been a great success in the enterprise world, and although ETP itself is language-agnostic,
there have been many systems written in Java following or extending from EIP.

However, in recent years there has been a trend to rewrite systems in more stream-based fashion,
given a massive scale of data enterprise systems need to process every day,
and an increasing business demand for real-time analysis.

Alpakka is a great fit in this area - it is a Reactive Enterprise Integration library for Java and Scala, based on Reactive Streams and Akka.
It allows you easily connect your systems with other external systems and services, and perform stream-based operations.

In this article, we introduce Alpakka's Cassandra connector as an example, and see what Alpakka gives you over Cassandra's plain Java driver.

## About Alpakka

[Alpakka](https://github.com/akka/alpakka) is a community based effort collaborating with Akka maintainers at Lightbend,
and provides a large, and ever increasing number of connectors for files, queues including AMQP and Kafka, AWS/GCP/Azure services, and more.

You can see how Alpakka grew over time from Akka team's [blog post](https://akka.io/blog/news/2018/05/02/alpakka-team),
and the increased number of connectors as in the below screenshot.

![Alpakka Growth](alpakka-growth.png)

Since Alpakka provides the connectors as Akka Stream operators, it's not just easy to connect to these other systems and services,
but you can also get benefit from Akka Stream's back-pressure support and fine-grained control over the stream at any level you want.
Akka Stream's flexible DSL makes it easy to combine different operators to perform things like buffering, throttling, branching, pub/sub, etc.
What's more, you can even create your own operators.

## About Cassandra

Cassandra is a database product originally created by Facebook, and known for its great write performance and distributed nature by design.

- https://academy.datastax.com/planet-cassandra/what-is-apache-cassandra
- http://cassandra.apache.org/

Although Cassandra is different from relational databases in many aspects, its query language CQL has some similarities to SQL,
and Cassandra indeed has a concept of tables.

If you already have existing data stored in Cassandra and want to introduce stream-based operations in your system,
or you have Akka-based or Akka Stream-based systems and looking for a database with great scalability and fault tolerance,
this blog post can be useful for you.

## Prerequisites for running Examples

The full code example can be found [here](https://github.com/richardimaoka/alpakka-blog-post/tree/master/src/main/java/com/example).

### Bring up Cassandra

To follow the examples in this article, you should firstly bring up Cassandra.
If you already have Cassandra up and running, you can skip this section.

The easiest way to bring up Cassandra, if you already have Docker installed, is run it via Docker.
Make sure you expose the port 9042 so that the example code can connect to Cassandra via the port.

```
docker pull cassandra
docker run -p 127.0.0.1:9042:9042 -d cassandra
```

If you are not familiar with Docker, [download Cassandra](http://cassandra.apache.org/download/),
unarchive it, set PATH to the Cassandra bin directory

### Dependency

To run the examples, you must add the following dependency to your project.

```
libraryDependencies += "com.lightbend.akka" %% "akka-stream-alpakka-cassandra" % "0.19"
```

## CassandraSource example

Alpakka Cassandra has three different connectors, `CassandraSource`, `CassandraSink` and `CasssandraFlow`.

The first example we talk about is `CassandraSource`, which could be useful when you perform a batch-like operation
against a very large data set.

![CassandraSourceExample](CassandraSourceExample.gif)

As you see in the animation, `CassandraSource` lets you run a CQL query, which fetches `ResultSet` from Cassandra,
and passes each `Row` from the `ResultSet` as an element going through Akka Stream.
Note that the CQL query is only run once. It is not something that keeps polling Cassandra given some filtering criteria,
and that's why it is suitable for batch-like operations.

In a nutshell, you can create and run a stream with Alpakka Cassandra connector like below:

```java
final Statement stmt =
  new SimpleStatement("SELECT * FROM akka_stream_java_test.users").setFetchSize(100);

final RunnableGraph<NotUsed> runnableGraph =
  CassandraSource.create(stmt, session)
    .to(Sink.foreach(row -> System.out.println(row)));

runnableGraph.run(materializer);
```

but we'll see how it works in more detail as follows.

### Details of the example

Again the full code example can be found [here](https://github.com/richardimaoka/alpakka-blog-post/tree/master/src/main/java/com/example).
Again the full code example can be found [here](https://github.com/richardimaoka/alpakka-blog-post/tree/master/src/main/java/com/example).

To go through the example code, you firstly need to add following import statements,

```java
// Alpakka Cassandra connector
import akka.stream.alpakka.cassandra.javadsl.CassandraSource;

// For Akka an Akka Stream
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;

// For Java Cassandra driver
import com.datastax.driver.core.*;
```

and you need to initialize the following stuff before running `CassandraSource` to connect to Cassandra.

```java
// Make sure you already brought up Cassandra, which is accessible via the host and port below.
// The host and port would be driven from a config in a production environment
// but hardcoding them here for simplicity.
final Session session = Cluster.builder()
  .addContactPoint("127.0.0.1").withPort(9042)
  .build().connect();

// ActorSystem and Materializer are necessary as the underlying infrastructure to run Akka Stream
final ActorSystem system = ActorSystem.create();
final Materializer materializer = ActorMaterializer.create(system);
```

If you are not familiar with `ActorSystem` and `Materializer`, you can assume that
they are like the underlying infrastructure to run Akka Stream.
Typically there is only one instance of `ActorSystem` and only one instance of `Materializer` in your application, more precisely,
in your (OS) process.

In a production environment, you should already have a data set in Cassandra, but in this example,
we prepare a data set by ourselves before running Akka Stream with `CassandraSource`.
So let's create a keyspace and a table in Cassandra as follows:

```
final Statement createKeyspace = new SimpleStatement(
  "CREATE KEYSPACE IF NOT EXISTS akka_stream_java_test WITH REPLICATION = "
   + "{ 'class' :  'SimpleStrategy', 'replication_factor': 1 };"
);
session.execute(createKeyspace);

final Statement createTable = new SimpleStatement(
  "CREATE TABLE akka_stream_java_test.users (" +
    "id int, " +
    "name text, " +
    "age int, " +
    "PRIMARY KEY (id)" +
    ");"
);
session.execute(createTable);
```

In the above example code, we use the Cassandra Java driver to execute them so that you don't need to install
CQL client yourself to connect to Cassandra. Keyspace is what contains Cassandra tables, and you need to declare a replication
strategy when you create a keyspace. After creating the keyspace, you can create a table under it.

Now you can insert data into the table:

```java
for(int i = 1; i <= 1000; i++){
  // For simplicity we use the same name and age in this example
  String name = "John";
  int age = 35;

  // Prepared statement is typical for parameterized queries in CQL (Cassandra Query Language).
  // In production systems, it can be used to guard the statement from injection attacks, similar to SQL prepared statement.
  BoundStatement bound = prepared.bind(i, name, age);
  session.execute(bound);
}
```

Here, if you execute the following CQL query,

```
select * FROM akka_stream_java_test.users ;
```

you will get the result set like below,

```
 id  | age | name
-----+-----+------
 769 |  35 | John
  23 |  35 | John
 114 |  35 | John
 660 |  35 | John
 893 |  35 | John
  53 |  35 | John
 987 |  35 | John
 878 |  35 | John
 110 |  35 | John
 ...
 ...
```

but we will execute this query using `CassandraSource`.

To supply the query to `CassandraSource`, you should create a Statement beforehand,
using setFetchSize to set the paging size.

```java
//https://docs.datastax.com/en/developer/java-driver/3.2/manual/paging/
final Statement stmt =
  new SimpleStatement("SELECT * FROM akka_stream_java_test.users").setFetchSize(100);
```

Cassandra Java driver already has a [paging feature](https://docs.datastax.com/en/developer/java-driver/3.2/manual/paging/), (the below picture is cited from the reference Cassandra article)

![Cassandra Paging](cassandra-paging.png)

so that you don't need to be afraid of your Cassandra client going out of memory by fetching a huge data set in one go.
Cassandra's paging works nicely with Akka Stream, and on top of it, Akka Stream allows fully non-blocking execution
without Cassandra driver's imperative [async-paging interface](https://docs.datastax.com/en/developer/java-driver/3.2/manual/async/#async-paging).

Finally, you can run the stream like below:

```java
final RunnableGraph<NotUsed> runnableGraph =
  CassandraSource.create(stmt, session)
    .to(Sink.foreach(row -> System.out.println(row)));

runnableGraph.run(materializer);
```

and get the following output.

```
...
Row[829, 35, John]
Row[700, 35, John]
Row[931, 35, John]
Row[884, 35, John]
Row[760, 35, John]
Row[628, 35, John]
Row[498, 35, John]
Row[536, 35, John]
...
```

### More realistic `CassandraSource` examples

This section is in progress. Should it be omitted as the article is getting too long??

- filtering, when you cannot express filtering criteria as CQL (e.g.) it changes by user

```java
CassandraSource
  .create(stmt, session)
  .map(row -> new User(
    row.getInt("id"),
    row.getString("name"),
    row.getInt("age")
  ))
  .mapAsync(1, user -> {
    ... //make an external service call
  })
  .filter(serviceResult -> {
    ... //perform complicated filtering
  })
  .to(Sink.foreach(row -> System.out.println(row)));
```

- aggregation, CQL doesn't have native support for group by, unlike SQL. So you can do this:
  FYI
   - [How to perform similar group by operations in CQL described at Chris Batey's blog](http://christopher-batey.blogspot.com/2015/05/cassandra-aggregates-min-max-avg-group.html)
   - [Akka Stream's groupBy operator](https://doc.akka.io/docs/akka/2.5/stream/operators/Source-or-Flow/groupBy.html)

```java
CassandraSource
  .create(stmt, session)
  .map(row -> new User(
    row.getInt("id"),
    row.getString("name"),
    row.getInt("age")
  ))
  .groupBy(200, user -> user.age) //group by user's age
  .fold(
    akka.japi.Pair.create(0, 0),
    (accumulated, user) -> akka.japi.Pair.create(user.age, accumulated.second() + 1)
  )
  .to(Sink.foreach(accumulated ->
     System.out.println("age: " + accumulated.first() + " count: " + accumulated.second()
   )));
```

- throttling, if flow/sink connected to `CassandraSource` cannot perform appropriate back pressuring

```java
CassandraSource
  .create(stmt, session)
  .map(row -> new User(
    row.getInt("id"),
    row.getString("name"),
    row.getInt("age")
  ))
  .throttle(10, java.time.Duration.ofSeconds(1))
  .to(externalSink); //Sink representing external system, like RDB, ElasticSearch, HTTP API, etc
```

## CassandraSink example

The full code example can be found [here](https://github.com/richardimaoka/alpakka-blog-post/tree/master/src/main/java/com/example).

The next example we see is CassandraSink, which lets you insert `Row`s into Cassandra at the end of the stream.

This is useful for more like a real-time system that keeps running where your data goes from a data source and
eventually written into Cassandra.

![CassandraSinkExample](CassandraSinkExample.gif)

To run CassandraSink, the code would look like below:

```java
final PreparedStatement insertTemplate = session.prepare(
  "INSERT INTO akka_stream_java_test.user_comments (id, user_id, comment) VALUES (uuid(), ?, ?)"
);

BiFunction<UserComment, PreparedStatement, BoundStatement> statementBinder =
  (userData, preparedStatement) -> preparedStatement.bind(userData.userId, userData.comment);

final Sink<UserComment, CompletionStage<Done>> cassandraSink =
  CassandraSink.create(2, insertTemplate, statementBinder, session);

source.to(cassandraSink).run(materializer);
```

### Details of the example

In this example, we use a different table from what we used in the `CassandraSource` example.

```
final Statement createTable = new SimpleStatement(
  "CREATE TABLE akka_stream_java_test.user_comments (" +
    "id uuid, " +
    "user_id int, " +
    "comment text, " +
    "PRIMARY KEY (id)" +
  ");"
);

session.execute(createTable);
```

This table is associated with previous `users` table, where `user_comments.user_id` is reference to `users.id`.
However, there is no concept of foreign keys in Cassandra, so your application code needs to make sure the association
is kept tight (i.e. every `user_id` value in `user_comments` must also exist in `users.id`).
Anyway, that is beyond the scope of this article, so let's come back to the `CassandraSink` stuff.

As you have the table in Cassandra, you can now define an associated model class in Java.

```java
public static class UserComment {
  int    userId;
  String comment;

  UserComment(int userId, String comment) {
    this.userId = userId;
    this.comment = comment;
  }
}
```

You need to create a prepared statement, to insert parameterized rows into Cassandra.
Prepared statements in Cassandra is similar to that of SQL for relational databases, and they are strong against injection attacks.

```java
final PreparedStatement insertTemplate = session.prepare(
  "INSERT INTO akka_stream_java_test.user_comments (id, user_id, comment) VALUES (uuid(), ?, ?)"
);
```

Next, you need this (probably) unfamiliar-looking `BiFunction`.

```java
BiFunction<UserComment, PreparedStatement, BoundStatement> statementBinder =
  (userComment, preparedStatement) -> preparedStatement.bind(userData.userId, userData.comment);
```

The signature of this `BiFunction` is bit complicated, but it means:
 - take `UserComment` as input
 - "bind" it to `PreparedStatement`
 - so that the bound CQL statement can be executed

Using `statementBinder`, now you can create `CassandraSink`.

```
final Sink<UserComment, CompletionStage<Done>> cassandraSink =
  CassandraSink.create(2, insertTemplate, statementBinder, session);

```

The parameter `2` in `CassandraSink.create(2, ...)` specifies the parallelism on writing into Cassandra.
We'll discuss about the parallelism bit later in this article.

For easiness, we can provide a data source as simple as below and run the stream:

```java
Source<UserComment, NotUsed> source =
  Source.from(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
    .map(i -> new UserComment(1, "some comment"))

source.to(cassandraSink).run(materializer);
```

however, let's do something smarter and more useful here.

You can use `Source.actorRef` to connect a `Sink` to an `Actor`,

![Source ActorRef](Source-actorRef.gif)

```java
final Source<UserComment, ActorRef> source = Source.actorRef(4, OverflowStrategy.fail());

// Stream 1
final ActorRef actorRef =
  source
  .to(cassandraSink)  //to() takes the left materialized value, (i.e.) source's ActorRef
  .run(materializer);
```

and pass this `ActorRef` to provide input from whatever data source you like.

![CassandraSinkExampleB](CassandraSinkExampleB.jpg)

```java
// Stream 2
Source.from(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
  // throttling the stream so that the Source.actorRef() does not overflow
  .throttle(1, Duration.of(50, ChronoUnit.MILLIS))
  .map(i -> new UserComment(1, "some comment"))
  //actorRef below is connected to CassandraSink
  .to(Sink.actorRef(actorRef, "stream completed"))
  .run(materializer);
```

For example, instead of sending elements from such a simple `int` list,
your data source can be HTTP requests from an (Akka) HTTP server,
and you can pass requests to this `ActorRef` after transforming requests to `UserComment`.

Or it could be a task queue like RabbitMQ or Kafka, which works as the data source,
and you can perform necessary operations on items from the queue and persist them to Cassandra.

### Note on parallelism

One thing to note about this example is that you can use the `parallelism` parameter of `CassandraSink` to improve throughput of the stream.
As discussed previously, Cassandra is known for its great write performance, and is distributed by nature so that your
writes are balanced across different nodes in the Cassandra cluster, not hammering a single node, as long as your table
defines the appropriate Cassandra persistence key.

So, chances are that you can insert into Cassandra parallelly to achieve faster `CassandraSink` than your data source,
which is a good thing and contributes to the stability of your entire stream.

## More realistic examples

- persist elements to multiple external sinks, by `alsoTo`
  - be careful on how each sink handles failure, failure on a single element might make the entire stream stuck with back-pressure

```java
source
  .alsoTo(elasticSearchSink)
  .alsoTo(jdbcSink)
  .to(cassandraSink)
  .run(materializer);
```

![CassandraSink alsoTo](CassandraSink-alsoTo.jpg)


## CasandraFlow example

The full code example can be found [here](https://github.com/richardimaoka/alpakka-blog-post/tree/master/src/main/java/com/example).

The last example we see is `CassandraFlow`. `CassandraFlow` allows you persist each element coming through
the `CassandraFlow` operator similar to `CassandraSink`, but the difference is that it also emits the element
after the CQL insert statement is finished.

![CassandraFlowExample](CassandraFlowExample.gif)

In short, you can run `CassandraFlow` like below.

```java
final PreparedStatement insertTemplate = session.prepare(
  "INSERT INTO akka_stream_java_test.user_comments (id, user_id, comment) VALUES (uuid(), ?, ?)"
);

BiFunction<UserComment, PreparedStatement, BoundStatement> statementBinder =
  (userData, preparedStatement) -> preparedStatement.bind(userData.userId, userData.comment);

final Flow<UserComment, UserComment, NotUsed> cassandraFlow =
  CassandraFlow.createWithPassThrough(2, insertTemplate, statementBinder, session, system.dispatcher());

source.via(cassandraFlow).to(sink).run(materializer);
```

The above example is similar to `CassandraSink`, so we are not going too much detail about the example again.
Also, what we discussed in the note about the `CassandraSink` parallelism applies to `CassandraFlow` too.

### More realistic example

- Replacement to DB polling

One good use case of `CassandraFlow` is replacement of DB polling. It is a common requirement that
you want to perform a certain operation whenever there is a new row inserted into a database.

A traditional way to achieve this is to periodical DB polling - query the database (e.g.) every X minutes,
and if you find new rows inserted, perform operations on them. To see if there are new rows inserted,
the client which polls the database remembers the last element processed, and only fetches rows which
are newer than that timestamp.

Using `CassandraFlow`, you can achieve such an operation "triggered by new insertion" in a more straightforward manner.
Whenever insertion to Cassandra succeeds, you can perform the operation.

```java
source
  .via(cassandraFlow)
  .via(someOperation)
  .to(sink)
  .run(materializer);
```
