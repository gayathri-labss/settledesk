# Apache Kafka – Learning Notes

## 1. What is Apache Kafka?

Apache Kafka is a distributed event streaming platform used for communication between different applications and services.

It allows applications to publish, store and consume events asynchronously.

An event is a notification that something has happened in an application.

Examples:
- TradeCreated
- TradeMatched
- SettlementCompleted
- PaymentProcessed
- OrderPlaced

### Why do we use Kafka?

Consider a banking application where a customer makes a payment.

After the payment succeeds, multiple operations need to happen:
- Update transaction history.
- Send an SMS notification.
- Generate an audit record.
- Update reporting information.

Instead of making the Payment Service directly call every other service, it can publish an event to Kafka.

Other services can consume the event independently.

### Benefits of Kafka

- **Decoupling:** Services don't need to communicate directly with one another.
- **Scalability:** Multiple consumers can process events in parallel.
- **Asynchronous processing:** The producer doesn't need to wait for all consumers to finish.
- **Reliability:** Events can remain available for consumption according to configured retention policies.
- **Fault tolerance:** Replication across brokers can help protect data against broker failures.

## 2. Event-Driven Architecture (EDA)

Event-Driven Architecture is a software architecture in which applications communicate through events.

An event represents something that has already happened.

Example:

When a user creates a trade, the Trade Service publishes a `TradeCreated` event.

The Matching Service receives the event and starts matching.

### Traditional synchronous communication

In synchronous communication, one service directly calls another service and waits for its response.

Example:

Trade Service → Matching Service → Settlement Service.

Problems:
- Services become tightly coupled.
- The original request may take longer.
- Downstream failures can affect the calling service.

### Asynchronous communication

In asynchronous communication, the producer publishes an event and does not need to wait for the consumer to finish processing it.

Example:

Trade Service → Kafka → Matching Service.

Benefits:
- Services are more loosely coupled.
- Different consumers can process events independently.
- Processing can happen in the background.

**Important:** Kafka transports and stores events. It does not execute the application's business logic. The consumer application executes that logic.

## 3. Kafka Architecture

Kafka's basic message flow involves:

1. Producer
2. Topic
3. Broker
4. Partition
5. Consumer

The general flow is:

Producer → Kafka Broker → Topic/Partition → Consumer.

### Kafka Cluster

A Kafka cluster is a group of Kafka brokers working together.

For example:

- Broker 1
- Broker 2
- Broker 3

Multiple brokers help distribute data and processing.

For local development, a single broker is sufficient for learning, but it does not provide multi-broker redundancy.

### Broker

A broker is an individual Kafka server.

Responsibilities:
- Receives messages from producers.
- Stores messages in topic partitions.
- Serves messages to consumers.
- Participates in replication when configured.

### KRaft

Modern Apache Kafka supports KRaft mode for managing metadata and cluster coordination.

KRaft removes the need for a separate ZooKeeper cluster.

## 4. Important Kafka Terminology

### 4.1 Producer

A Producer is an application or component that sends events to Kafka.

Example:

Trade Service creates Trade 101 and publishes a `TradeCreated` event.

Therefore, Trade Service acts as the Producer.

In Spring Boot, we commonly use `KafkaTemplate` to send messages.

### 4.2 Topic

A Topic is a named stream where related events are stored.

Think of a topic as a category for messages.

Examples:

| Topic | Events |
|---|---|
| trade-events | TradeCreated, TradeMatched |
| settlement-events | SettlementCompleted, SettlementFailed |
| reconciliation-events | ReconciliationCompleted, BreakDetected |

A producer publishes messages to a topic, and consumers subscribe to topics.

A topic is not a Java class or SQL Server table.

### 4.3 Broker

A Broker is an individual Kafka server responsible for receiving, storing and serving messages.

A Kafka Cluster consists of one or more brokers.

### 4.4 Partition

A Partition is an ordered log of records within a topic.

Kafka divides topics into partitions to support:
- Parallel processing.
- Scalability.
- Distribution across brokers.
- Ordering within a partition.

Example:

Topic: `trade-events`

- Partition 0
- Partition 1
- Partition 2

Each partition maintains its own ordering.

Kafka does not guarantee a single global order across different partitions.

### 4.5 Message Key

A message key helps Kafka decide which partition receives a message.

Example:

Trade ID 101 can be used as the key for trade events.

When using the default keyed partitioner with a stable partition count, records with the same key are routed to the same partition.

This helps maintain ordering for events belonging to the same trade.

### 4.6 Consumer

A Consumer is an application or component that reads and processes events from Kafka.

Example:

Matching Service subscribes to `trade-events`.

When it receives a `TradeCreated` event, it calls its matching business logic.

In Spring Boot, we commonly use `@KafkaListener` to consume messages.

### 4.7 Consumer Group

A Consumer Group is a group of consumers that share the work of reading messages from a topic.

Example:

Suppose `trade-events` has three partitions.

A consumer group named `matching-group` has three consumer instances.

Kafka can assign:
- Consumer 1 → Partition 0
- Consumer 2 → Partition 1
- Consumer 3 → Partition 2

Each partition is assigned to only one consumer within the same group at a time.

Why do we use consumer groups?
- Distribute processing.
- Scale consumers horizontally.
- Process events in parallel.

**Important distinction:**

- Same consumer group: Consumers share the processing workload.
- Different consumer groups: Each group independently consumes the topic's event stream.

For example:
- `matching-group` processes trade matching.
- `audit-group` records audit information.

Both groups can independently consume the same `TradeCreated` event.

A consumer group cannot actively use more consumers than the number of partitions assigned to it.

### 4.8 Offset

An Offset is the position of a record within a Kafka partition.

Example:

| Offset | Message |
|---|---|
| 0 | TradeCreated – 101 |
| 1 | TradeCreated – 102 |
| 2 | TradeCreated – 103 |
| 3 | TradeMatched – 101 |
| 4 | TradeCreated – 104 |

Offsets help consumers track their reading progress.

Suppose a consumer has successfully processed records at offsets 0, 1 and 2.

It can commit its next position as offset 3.

If the consumer restarts, it can resume from its committed position.

Important points:
- Offsets are specific to partitions.
- Consumer groups maintain their own committed progress.
- Offsets are not trade IDs or event IDs.
- Incorrect offset management can cause duplicate processing or skipped processing.

## 5. Consumer Group vs Offset

These two concepts are related but have different responsibilities.

| Concept | Meaning |
|---|---|
| Consumer Group | Identifies consumers sharing processing work |
| Offset | Tracks the group's reading progress in a partition |

Example:

Suppose two groups consume `trade-events`:

- `matching-group`
- `audit-group`

Matching may have processed up to offset 10, while Audit has processed up to offset 6.

Each group maintains its own progress independently.

## 6. Serialization and Deserialization

Kafka works with bytes, not Java objects directly.

### Serialization

Serialization means converting a Java object into bytes so it can be sent and stored.

Example:

Java object:

`TradeCreatedEvent(101, "AAPL", "USD")`

It can be serialized into JSON and then represented as bytes.

### Deserialization

Deserialization is the reverse process.

The consumer receives the message bytes and converts them back into a Java object.

### Complete flow

Producer Java Object → Serializer → Bytes → Kafka → Bytes → Deserializer → Consumer Java Object.

### Common serialization formats

| Format | Description |
|---|---|
| JSON | Human-readable and easy to understand |
| Avro | Compact binary format with schema support |
| Protobuf | Structured binary serialization format |
| String | Simple text messages |

For SettleDesk, we will initially use JSON serialization.

In Spring Kafka:
- `JsonSerializer` converts Java objects into JSON bytes.
- `JsonDeserializer` converts JSON bytes back into Java objects.
- `StringSerializer` handles string keys.
- `StringDeserializer` handles string keys.

## 7. Kafka Producer in Spring Boot

A Producer publishes messages to a Kafka topic.

Spring Kafka provides `KafkaTemplate` for sending messages.

Example event:

`TradeCreatedEvent`

Example producer:

```java
@Service
@RequiredArgsConstructor
public class TradeEventProducer {

    private final KafkaTemplate<String, TradeCreatedEvent>
            kafkaTemplate;

    public void publishTradeCreated(TradeCreatedEvent event) {

        kafkaTemplate.send(
                "trade-events",
                event.tradeId().toString(),
                event
        );
    }
}
```

Understanding the send operation:

```java
kafkaTemplate.send("trade-events", "101", event);
```

- First argument: Topic name.
- Second argument: Message key.
- Third argument: Message value.

The message key helps Kafka select a partition.

## 8. Kafka Consumer in Spring Boot

A Consumer reads and processes events from Kafka.

Spring Kafka provides the `@KafkaListener` annotation.

Example:

```java
@Service
@RequiredArgsConstructor
public class TradeEventConsumer {

    private final MatchingService matchingService;

    @KafkaListener(
        topics = "trade-events",
        groupId = "matching-group"
    )
    public void consume(TradeCreatedEvent event) {

        matchingService.matchTrade(event.tradeId());
    }
}
```

Understanding the code:

- `@KafkaListener` makes the method listen to a Kafka topic.
- `topics` specifies which topic to consume.
- `groupId` specifies the consumer group.
- The method receives the deserialized event.
- The Matching Service executes the actual business logic.

Spring Kafka manages the polling process for an annotated listener.

**Important:** Kafka delivers the event. The application service performs the business operation.

## 9. Kafka Reliability

Reliability is important because events should not be lost or incorrectly processed.

### 9.1 Acknowledgements (acks)

An acknowledgement is confirmation from Kafka about the result of a producer's write request.

Common settings:

| Setting | Meaning |
|---|---|
| acks=0 | Producer does not wait for a broker acknowledgement |
| acks=1 | Producer waits for the partition leader to acknowledge |
| acks=all | Producer waits for all currently in-sync replicas to acknowledge |

`acks=all` provides stronger durability when replication and broker configuration are appropriate.

### 9.2 Retries

Retries mean attempting an operation again after a temporary failure.

Example:

1. Producer sends an event.
2. Kafka is temporarily unavailable.
3. The send fails.
4. Producer retries.
5. The next attempt succeeds.

Retries should have sensible limits and backoff.

### 9.3 Duplicate Messages

A message may be delivered or processed more than once.

Example:

1. Kafka receives an event.
2. The acknowledgement is lost.
3. Producer retries.
4. The same logical event may be published again.

Consumers must be designed to handle duplicates safely.

### 9.4 Idempotency

Idempotency means performing the same operation multiple times produces the same intended business result as performing it once.

Example:

A consumer receives event `EVT101`.

It checks whether the event has already been processed.

- If new: Process it and record the event ID.
- If duplicate: Avoid repeating the business operation.

Idempotency is important for trade matching, payments and settlement processing.

### 9.5 Delivery Guarantees

Kafka commonly discusses three delivery semantics.

| Delivery semantic | Meaning |
|---|---|
| At-most-once | Event is processed zero or one time; loss is possible |
| At-least-once | Event is processed one or more times; duplicates are possible |
| Exactly-once | Processing effects are coordinated within a supported transactional scope |

For SettleDesk, we will initially design for at-least-once delivery with idempotent consumer processing.

Exactly-once behaviour across Kafka, SQL Server and external systems requires additional coordination.

### 9.6 Consumer Failure

Suppose a consumer processes an event successfully but crashes before committing its offset.

After restarting, it may receive the same event again.

This is why:
- Offset management is important.
- Consumers should be idempotent.
- Business processing should succeed before the corresponding offset is committed.

### 9.7 Dead Letter Topic (DLT)

A Dead Letter Topic stores messages that cannot be processed successfully after the configured retry and recovery process.

Example:

```text
trade-events
     |
     v
Matching Consumer
     |
     v
Processing Failure
     |
     v
Retry Attempts
     |
     v
Still Failing
     |
     v
trade-events.DLT
```

The failed events can then be investigated and recovered separately.

## 10. Transactional Outbox Pattern

### Problem: Dual Write

In SettleDesk, two operations must happen when creating a trade:

1. Save the trade in SQL Server.
2. Publish `TradeCreated` to Kafka.

What happens if SQL Server succeeds but Kafka fails?

The trade exists in the database, but Matching Service never receives the event.

This is called the Dual Write Problem.

### Solution: Transactional Outbox Pattern

The pattern stores the business record and the event record in the same database transaction.

Two records are saved:

1. Trade record in the `trade` table.
2. Event record in the `outbox_event` table.

Both commit successfully or both roll back.

### Complete flow

```text
Trade Creation Service
          |
          v
SQL Server Transaction
          |
          |---- Trade Table
          |
          |---- Outbox Event Table
          |
          v
Outbox Publisher
          |
          v
Kafka Producer
          |
          v
Kafka Topic
          |
          v
Matching Consumer
```

### Outbox Event Table

An illustrative table:

| event_id | event_type | aggregate_id | status |
|---|---|---|---|
| EVT101 | TradeCreated | 101 | PUBLISHED |
| EVT102 | TradeCreated | 102 | PENDING |

Possible fields:
- Event ID.
- Event type.
- Aggregate ID.
- Event payload.
- Publication status.
- Created timestamp.
- Published timestamp.

### Outbox Publisher

A background component periodically reads unpublished events from the outbox table and publishes them to Kafka.

After Kafka acknowledges publication, the publisher marks the record as published.

### What if Kafka is down?

The event remains pending in SQL Server.

When Kafka becomes available, the publisher can retry.

### Duplicate Publishing

Suppose Kafka receives an event, but the publisher crashes before updating the outbox status.

The event may be published again after recovery.

Therefore, consumers must use event IDs and idempotent processing to avoid duplicate business effects.

**Important:** The Outbox Pattern solves the database-to-Kafka dual-write problem. It does not automatically guarantee exactly-once delivery.

## 11. SettleDesk Kafka Architecture

The following is a proposed architecture for our project.

### Proposed Topics

| Topic | Purpose |
|---|---|
| trade-events | Trade lifecycle events |
| settlement-events | Settlement events |
| reconciliation-events | Reconciliation events |

### Proposed Events

Trade events:
- TradeCreated
- TradeConfirmed
- TradeMatched
- TradeMatchingFailed

Settlement events:
- SettlementInstructionCreated
- SettlementCompleted
- SettlementFailed

Reconciliation events:
- ReconciliationCompleted
- ReconciliationBreakDetected

### Proposed Producer and Consumer Mapping

| Producer | Topic | Consumer |
|---|---|---|
| Trade Service | trade-events | Matching Service |
| Trade Service | trade-events | Audit Consumer Group |
| Settlement Service | settlement-events | Reconciliation Service |
| Reconciliation Service | reconciliation-events | Audit Consumer Group |

This mapping is a proposed design and will be finalized as we implement the workflows.

### Important Design Principle

Services should publish meaningful business events after the relevant business changes have been successfully persisted.

Consumers should process events independently and safely handle duplicate delivery.

## 12. Kafka Setup for SettleDesk

Our local development setup uses:

| Component | Technology |
|---|---|
| Programming language | Java 25 |
| Backend framework | Spring Boot 4.1.1 |
| Kafka | Apache Kafka 4.4.0 |
| Kafka runtime | Docker |
| Database | Microsoft SQL Server |
| Kafka integration | Spring Kafka |
| API testing | Postman |

Kafka is running in KRaft mode, so a separate ZooKeeper installation is not required.

### Planned Kafka Implementation Steps

1. Add Spring Kafka dependency to `pom.xml`.
2. Configure the Kafka broker connection.
3. Create Kafka topics.
4. Implement the Trade Event Producer.
5. Implement the Trade Event Consumer.
6. Publish `TradeCreated`.
7. Consume the event in Matching Service.
8. Implement settlement events.
9. Add retry and error handling.
10. Implement the Transactional Outbox Pattern.
11. Add idempotent consumer processing.
12. Write Kafka integration tests.

## 13. Quick Revision – Kafka Glossary

| Term | Meaning |
|---|---|
| Kafka | Distributed event streaming platform |
| Event | Notification that something happened |
| Producer | Sends events |
| Topic | Named stream of events |
| Broker | Kafka server |
| Cluster | Group of brokers |
| Partition | Ordered log within a topic |
| Message Key | Helps select a partition |
| Consumer | Reads and processes events |
| Consumer Group | Consumers sharing processing work |
| Offset | Position of a record within a partition |
| Serialization | Converts Java objects into bytes |
| Deserialization | Converts bytes back into Java objects |
| Acknowledgement | Broker confirmation of a write |
| Retry | Attempts an operation again |
| Idempotency | Prevents repeated business effects |
| DLT | Stores messages that could not be processed |
| Outbox Pattern | Reliably connects database transactions with event publishing |

## 14. Key Takeaways

1. Kafka enables asynchronous communication between services.
2. Producers publish events; consumers process them.
3. Topics organize related events.
4. Partitions enable parallel processing and maintain ordering within each partition.
5. Consumer groups distribute workload.
6. Offsets track consumer progress.
7. Serialization and deserialization enable communication between Java applications and Kafka.
8. Retries and acknowledgements help handle failures.
9. Idempotent consumers protect against duplicate business processing.
10. Transactional Outbox helps prevent the database-to-Kafka dual-write problem.

---

**Learning status:** Kafka fundamentals covered at a high level. Advanced concepts and practical implementation will be learned progressively while developing SettleDesk.
