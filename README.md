## Brief 2-3 line introduction about why Cassandra and Alpakka could be interesting to users. Maybe mention a typical use case of that. (e.g.) You had to use Cassandra for scaling, and use 

## What Cassandra is, and why it matters the in stream processing context

- Assume readers would are somewhat familiar with Cassandra, so don’t go into too much detail about Cassandra itself
- Maybe the reader already has akka-based backend systems, which you want to connect Cassandra to, in a streaming fasion

## What Alpakka is

- Community-based effort
- Implements EIP patterns with streams

## Example

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

### CasandraFlow example

- Same 4 points as CassandraSource
- We should not use UNLOGGED batch? It doesn't improve performance unless you are SURE your batch has the same partition key
  - http://batey.info/cassandra-anti-pattern-misuse-of.html

### CassandraSink example

- Same 4 points as CassandraSource
- mapAsync to improve performance, as Cassandra is a distributed store


## Integration with akka-persistence-cassandra??

- Maybe 1. write into Cassandra via akka-persistence-cassandra, and 2. read from Cassandra with CassandraSource
 - https://www.beyondthelines.net/computing/akka-persistence/
- Probably goes beyond the scope of this article