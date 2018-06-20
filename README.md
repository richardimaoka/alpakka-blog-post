## Background

Enterprise Integration Patterns, EIP, have been a great success in the enterprise world,
and there have been many systems written in Java following or extending from patterns described in EIP.

However, in recent years there has been a trend to rewrite systems in more stream-based fashion,
given a massive scale of data such enterprise systems need to process every day,
and an increasing business demand for real-time analysis.

Alpakka, which is a Reactive Enterprise Integration library for Java and Scala, based on Reactive Streams and Akka,
is a great fit in this area. It allows you easily connect your systems with other external systems and services, and perform stream-based operations.

In this article, we introduce Alpakka's Cassandra connector as an example, and see what Alpakka gives you over Cassandra's plain Java driver.

## About Alpakka

Alpakka is a community based effort collaborating with Akka maintainers at Lightbend,
and provides a large, and ever increasing number of connectors for files, queues including AMQP and Kafka, AWS/GCP/Azure services, and more.

**************************************
**************************************
QUESTION: Do we want to show the contrast in the number of Alpakka plugins in 2016 abd 2018?
**************************************
**************************************

https://github.com/akka/alpakka
https://akka.io/blog/news/2018/05/02/alpakka-team

Since Alpakka provides the connectors as Akka Stream operators, it's not just easy to connect to these other systems and services,
but you can also benefit from Akka Stream's back-pressure support and fine-grained control over the stream at any level you want.
Akka Stream's flexible DSL makes it easy to combine different operators to perform buffering, throttling, branching, pub/sub, and you can even create your own operators.

## About Cassandra

Cassandra is a database product originally created by Facebook, and known for its great write performance and distributed nature by design.

https://academy.datastax.com/planet-cassandra/what-is-apache-cassandra
http://cassandra.apache.org/

Although Cassandra is different from relational databases in many aspects, its query language CQL has some similarities to SQL,
and Cassandra indeed has a concept of tables.

If you already have existing data stored in Cassandra and want to introduce stream-based operations in your system,
or you have Akka-based or Akka Stream-based systems and looking for a database with great scalability and fault tolerance,
this blog post can be useful for you.

## Example

****************************************************************************
****************************************************************************
TODO

As said earlier, do buffering, throttling, etc to show benefits of
Akka streams. The current examples are just as boring as plain Cassandra
Java drives.
****************************************************************************
****************************************************************************


### Bring up Cassandra

In case you already have a Cassandra cluster up and running, you can skip this section.
Otherwise, to see how the example works, you can set up a single-node Cassandra cluster on your local machine as follows.

If you are familiar with Docker, the easiest way is to use docker:

```
docker pull cassandra
docker run -p 127.0.0.1:9042:9042 -d cassandra
```

Otherwise, go to http://cassandra.apache.org/download/, download Cassandra and set the PATH to it.

Next, you should set up a keyspace and a table.
Run CassandraSetup in this repository to insert 1000 rows into the `akka_stream_java_test.users` table.

Othrwise, follow the below steps in cqlsh shell.

```
cqlsh> CREATE KEYSPACE IF NOT EXISTS akka_stream_java_test WITH REPLICATION = { 'class' :  'SimpleStrategy', 'replication_factor': 1 };
```

```
cqlsh> CREATE TABLE akka_stream_java_test.users (
   ...  id UUID,
   ...  name text,
   ...  age int,
   ...  PRIMARY KEY (id)
   ... );
```

// Link to explanation about replication strategies
// you should not use simplestrategy in production

- https://docs.datastax.com/en/cql/3.3/cql/cql_reference/cqlCreateKeyspace.html
- datastax video courses

to upload data, you can use cqlsh copy.

### Further resources about bulk uploading

 Probably don't need to mention in the article though

  ResultSet in Cassandra Driver pages, no need to worry about OOM
  http://batey.info/streaming-large-payloads-over-http-from.html

  Distributed Data Show Episode 49: Bulk Loading with Brian Hess
  https://www.youtube.com/watch?v=CAH7Mlg_rVI

### CassandraSource example

- Animation
- Code snippet
- Typical cases where this is useful
  - filtering
- Describe how it improves, compared to simply using cassandra query
  - Chris Batey's picture
  - http://2.bp.blogspot.com/-7HGmZMvPUMo/VNi-PTMHztI/AAAAAAAAAXQ/9IXNl2Pz-pM/s1600/Screenshot%2B2015-02-09%2B14.03.16.png

https://www.slideshare.net/doanduyhai/cassandra-drivers-and-tools
https://docs.datastax.com/en/developer/java-driver/3.2/manual/paging/

****************************************************************************
****************************************************************************
TODO

For CassandraSource, maybe a realistic example is a batch operation.
Introduce throttling, etc for more production-look examples
****************************************************************************
****************************************************************************

### CassandraSink example

- Same 4 points as CassandraSource
- mapAsync to improve performance, as Cassandra is a distributed store

****************************************************************************
****************************************************************************
TODO

For CassandraSink, real-time stream processing can be a good example.
Use parallelism, etc to form a more realistic example
****************************************************************************
****************************************************************************

### CasandraFlow example

- Same 4 points as CassandraSource
- We should not use UNLOGGED batch? It doesn't improve performance unless you are SURE your batch has the same partition key
  - http://batey.info/cassandra-anti-pattern-misuse-of.html



## Integration with akka-persistence-cassandra??

- Maybe 1. write into Cassandra via akka-persistence-cassandra, and 2. read from Cassandra with CassandraSource
 - https://www.beyondthelines.net/computing/akka-persistence/
- Probably goes beyond the scope of this article