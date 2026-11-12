# SettleDesk – Complete Project Testing Reference

## 1. Testing Scope

This document covers manual and automated testing of the SettleDesk application.

The application is a Spring Boot backend using Java 25, Spring Data JPA, Microsoft SQL Server, Apache Kafka, and Testcontainers.

Testing is divided into:

1. Application and infrastructure testing
2. Unit testing
3. Database and repository testing
4. REST API testing
5. Business logic testing
6. Integration testing
7. Kafka producer and consumer testing
8. Outbox and idempotency testing
9. End-to-end workflow testing
10. Data integrity and transaction testing
11. Error handling and negative testing
12. Performance and concurrency testing
13. Security testing
14. CI/CD testing

---

# 2. Application and Infrastructure Testing

## 2.1 Application startup

**Objective:** Verify that the Spring Boot application starts correctly and initializes its dependencies.

Test procedure:

1. Start SQL Server.
2. Start Kafka.
3. Start the Spring Boot application.
4. Inspect application startup logs.
5. Verify that the application connects to SQL Server.
6. Verify Kafka producer and consumer initialization.
7. Verify that JPA entities and repositories initialize successfully.

Expected result:

- Application starts without exceptions.
- Database connection succeeds.
- Kafka connection succeeds.
- No bean initialization failures.
- No database schema initialization errors.

## 2.2 Docker infrastructure

The repository contains a Docker Compose configuration with three services:

| Service | Container | Port |
|---|---|---|
| SQL Server | settledesk-sqlserver | 1433 |
| Kafka | settledesk-kafka | 9092 |
| Application | settledesk-app | 8080 |

Testing procedure:

```bash
docker compose up -d
docker compose ps
```

Expected result:

All three services should reach a running state.

Inspect logs:

```bash
docker compose logs database
docker compose logs kafka
docker compose logs app
```

Verify the application responds on its configured port.

```bash
curl -i http://localhost:8080/api/v1/trades
```

Expected result:

HTTP 200 with a paginated trade response, assuming the database is initialized and the application is available.

## 2.3 Dependency failure testing

Test application behavior when:

- SQL Server is unavailable during startup.
- Kafka is unavailable during startup.
- SQL Server becomes unavailable during an API operation.
- Kafka becomes unavailable during event publishing.
- A dependency becomes available again after failure.

Expected result:

The application should produce understandable errors, avoid silently corrupting business data, and recover according to its configured dependency and retry behavior.

Do not assume automatic recovery without verifying the actual application configuration.

---

# 3. Automated Unit Testing

## 3.1 Execute the complete test suite

From the repository root:

```bash
./mvnw clean test
```

On Windows:

```bash
mvnw.cmd clean test
```

Expected result:

- Maven build succeeds.
- No test failures.
- No test errors.
- No unexpected skipped tests.

Review the report:

```bash
open target/surefire-reports
```

The exact test count will depend on the current repository revision.

## 3.2 Service unit testing

Service-level tests should verify business rules independently of the database and web layer.

| Service | Testing scope |
|---|---|
| TradeService | Trade creation, updates, cancellation, status transitions |
| TradeConfirmationService | Confirmation creation and validation |
| MatchingService | Matching rules, outcomes, retries |
| SettlementService | Settlement lifecycle and eligibility |
| ReconciliationService | Comparison rules, result creation, break generation |
| BreakService | Assignment, update, resolution, summary |
| TradeLifecycleService | Lifecycle event recording |
| TradeAuditService | Audit event creation and retrieval |
| OutboxService | Outbox event persistence |
| ProcessedEventService | Duplicate event prevention |

### TradeService

Test:

- Valid trade creation.
- Invalid trade input.
- Updating an existing trade.
- Updating a non-existing trade.
- Valid status transition.
- Invalid status transition.
- Cancellation.
- Repeated cancellation.
- Audit event creation.
- Lifecycle event creation.
- Outbox event creation.
- Transaction rollback if a dependent operation fails.

Expected result:

The service should enforce the trade business rules and persist only valid state changes.

### MatchingService

Test:

- Matching records with identical values.
- Instrument mismatch.
- Quantity mismatch.
- Price mismatch.
- Buyer mismatch.
- Seller mismatch.
- Empty trade ID list.
- Invalid trade IDs.
- Duplicate trade IDs.
- Retry of eligible unmatched results.
- Retry when no eligible results exist.

Expected result:

Matching outcomes should reflect the configured comparison rules. A completed matching run does not necessarily mean every individual trade matched.

### SettlementService

Test:

- Creating settlement for a confirmed trade.
- Creating settlement for an unconfirmed trade.
- Creating duplicate settlement for one trade.
- Submitting a created settlement.
- Confirming a submitted settlement.
- Rejecting invalid state transitions.
- Handling `SETTLED`, `FAILED`, and `REJECTED` outcomes.
- Retrying failed settlements.
- Preventing retry of rejected settlements.

Expected result:

Settlement transitions must follow the implemented lifecycle rules.

### ReconciliationService

Test:

- Matching external and internal records.
- Mismatched instrument.
- Mismatched quantity.
- Mismatched price.
- Mismatched settlement date.
- Mismatched status.
- Missing internal record.
- Missing external record.
- Duplicate external reference.
- Duplicate settlement reference.
- Automatic break creation.

Expected result:

Every non-matched reconciliation result should be handled according to the reconciliation business rules.

---

# 4. Database and Repository Testing

## 4.1 SQL Server connectivity

The project uses SQL Server and Spring Data JPA.

The repository includes `SqlServerIntegrationTest`, which uses an MSSQL Testcontainer.

Test:

- Database container starts.
- Application connects to the container.
- SQL queries execute.
- JPA repositories can access their tables.

Example connectivity query:

```sql
SELECT 1;
```

Expected output:

```text
1
```

## 4.2 Persistence testing

Verify that the following entities are correctly persisted:

| Entity | What to verify |
|---|---|
| Trade | Trade attributes and status |
| TradeConfirmation | Confirmation data and trade relationship |
| MatchingRun | Matching execution details |
| MatchingResult | Individual matching outcomes |
| SettlementInstruction | Settlement data and status |
| SettlementLifecycleEvent | Settlement status history |
| SettlementAuditEvent | Settlement audit records |
| ReconciliationRun | Reconciliation execution |
| ReconciliationResult | Individual reconciliation outcomes |
| ReconciliationBreak | Break details and status |
| TradeLifecycleEvent | Trade lifecycle history |
| TradeAuditEvent | Trade audit records |
| OutboxEvent | Pending and published events |
| ProcessedEvent | Consumed event tracking |

## 4.3 SQL verification examples

The following queries use table names present in the repository's SQL Server integration tests or corresponding business entities.

Trade records:

```sql
SELECT *
FROM trade
ORDER BY trade_id DESC;
```

Outbox records:

```sql
SELECT *
FROM outbox_event
ORDER BY created_at DESC;
```

Trade lifecycle:

```sql
SELECT *
FROM trade_lifecycle
ORDER BY created_at DESC;
```

Trade audit:

```sql
SELECT *
FROM trade_audit
ORDER BY created_at DESC;
```

Reconciliation runs:

```sql
SELECT *
FROM reconciliation_run
ORDER BY run_id DESC;
```

Reconciliation results:

```sql
SELECT *
FROM reconciliation_result
ORDER BY run_id DESC;
```

Break records:

```sql
SELECT *
FROM reconciliation_break
ORDER BY break_id DESC;
```

Processed events:

```sql
SELECT *
FROM processed_event;
```

Note: Confirm the exact column names against the current entity mappings before running queries that use specific ordering columns.

## 4.4 Transaction rollback testing

The repository already contains transaction rollback scenarios in `SqlServerIntegrationTest`.

### Scenario A: Trade creation rollback

The test simulates an audit creation failure during trade creation.

Expected result:

- Trade record is not persisted.
- Outbox event is not persisted.
- Lifecycle record is not persisted.
- Audit record is not persisted.

### Scenario B: Reconciliation rollback

The test simulates an outbox failure during reconciliation.

Expected result:

- Reconciliation run is not persisted.
- Reconciliation results are not persisted.
- Reconciliation breaks are not persisted.

These scenarios validate transaction boundaries across multiple entities.

---

# 5. REST API Manual Testing

## 5.1 Postman environment

Create a Postman environment named `SettleDesk Local`.

| Variable | Value |
|---|---|
| `baseUrl` | `http://localhost:8080` |
| `tradeId` | Generated trade ID |
| `matchingRunId` | Generated matching run ID |
| `settlementId` | Generated settlement ID |
| `reconciliationRunId` | Generated reconciliation run ID |
| `breakId` | Generated break ID |

Common headers:

```http
Content-Type: application/json
Accept: application/json
```

All endpoint paths below are prefixed by:

```text
http://localhost:8080
```

For example:

```text
POST http://localhost:8080/api/v1/trades
```

---

# 6. Trade API Testing

## 6.1 Create trade

**Request**

```http
POST {{baseUrl}}/api/v1/trades
```

Headers:

```http
Content-Type: application/json
```

Body:

```json
{
  "instrument": "NIFTY",
  "quantity": 10,
  "price": 25000,
  "buyer": "BUYER_A",
  "seller": "SELLER_B"
}
```

Expected HTTP status:

```text
201 Created
```

Expected output:

A Trade object containing:

- Generated `tradeId`
- Instrument: `NIFTY`
- Quantity: `10`
- Price: `25000`
- Buyer: `BUYER_A`
- Seller: `SELLER_B`
- Initial status: `NEW`

Save the returned trade ID as `tradeId`.

## 6.2 Get trade by ID

```http
GET {{baseUrl}}/api/v1/trades/{{tradeId}}
```

Expected HTTP status:

```text
200 OK
```

Expected output:

The previously created trade with the same ID and attributes.

## 6.3 Search trades

```http
GET {{baseUrl}}/api/v1/trades?page=0&size=10&sortBy=tradeId&direction=asc
```

Expected HTTP status:

```text
200 OK
```

Expected output:

A Spring paginated response containing:

- `content`
- `pageable`
- `totalElements`
- `totalPages`
- `size`
- `number`
- `sort`
- `first`
- `last`

Filter by instrument:

```http
GET {{baseUrl}}/api/v1/trades?instrument=NIFTY
```

Filter by buyer:

```http
GET {{baseUrl}}/api/v1/trades?buyer=BUYER_A
```

## 6.4 Search validation

Invalid page:

```http
GET {{baseUrl}}/api/v1/trades?page=-1
```

Invalid page size:

```http
GET {{baseUrl}}/api/v1/trades?size=101
```

Invalid sorting field:

```http
GET {{baseUrl}}/api/v1/trades?sortBy=invalidField
```

Invalid direction:

```http
GET {{baseUrl}}/api/v1/trades?direction=random
```

Expected HTTP status:

```text
400 Bad Request
```

## 6.5 Update trade

```http
PUT {{baseUrl}}/api/v1/trades/{{tradeId}}
```

Body:

```json
{
  "instrument": "NIFTY",
  "quantity": 20,
  "price": 25100,
  "buyer": "BUYER_A",
  "seller": "SELLER_B"
}
```

Expected HTTP status:

```text
200 OK
```

Expected output:

Updated Trade object with quantity `20` and price `25100`, provided the trade is eligible for update.

## 6.6 Update trade status

```http
PATCH {{baseUrl}}/api/v1/trades/{{tradeId}}/status
```

Body:

```json
{
  "status": "CONFIRMED"
}
```

Expected HTTP status:

```text
200 OK
```

Expected output:

Trade object with status `CONFIRMED`, provided the transition is permitted by the service.

## 6.7 Bulk trade creation

```http
POST {{baseUrl}}/api/v1/trades/bulk
```

Body:

```json
[
  {
    "instrument": "NIFTY",
    "quantity": 10,
    "price": 25000,
    "buyer": "BUYER_A",
    "seller": "SELLER_B"
  },
  {
    "instrument": "BANKNIFTY",
    "quantity": 5,
    "price": 52000,
    "buyer": "BUYER_C",
    "seller": "SELLER_D"
  }
]
```

Expected HTTP status:

```text
201 Created
```

Expected output:

An array of created Trade objects with unique IDs.

Negative test:

```json
[]
```

Expected result:

Request should be rejected because at least one trade is required.

## 6.8 Cancel trade

```http
POST {{baseUrl}}/api/v1/trades/{{tradeId}}/cancel
```

Expected HTTP status:

```text
200 OK
```

Expected output:

Updated Trade object with cancellation reflected in its status.

Also test cancellation of:

- Non-existing trade.
- Already cancelled trade.
- Trade in a restricted lifecycle state.

Expected result:

Invalid cancellation operations should be rejected by the service.

## 6.9 Trade lifecycle

```http
GET {{baseUrl}}/api/v1/trades/{{tradeId}}/lifecycle
```

Expected HTTP status:

```text
200 OK
```

Expected output:

An array of lifecycle events showing recorded trade transitions.

## 6.10 Trade audit

```http
GET {{baseUrl}}/api/v1/trades/{{tradeId}}/audit
```

Expected HTTP status:

```text
200 OK
```

Expected output:

An array of audit events for the trade.

---

# 7. Trade Confirmation API Testing

The confirmation controller is mounted under:

`/api/v1/trades/{tradeId}/confirmations`

## 7.1 Create confirmation

```http
POST {{baseUrl}}/api/v1/trades/{{tradeId}}/confirmations
```

Body:

```json
{
  "counterpartyReference": "CONF-001",
  "instrument": "NIFTY",
  "quantity": 10,
  "price": 25000,
  "buyer": "BUYER_A",
  "seller": "SELLER_B"
}
```

Expected HTTP status:

```text
201 Created
```

Expected output:

A TradeConfirmation object containing a generated confirmation ID and the submitted confirmation details.

## 7.2 Get confirmations

```http
GET {{baseUrl}}/api/v1/trades/{{tradeId}}/confirmations
```

Expected HTTP status:

```text
200 OK
```

Expected output:

An array containing the confirmation records associated with the trade.

## 7.3 Confirmation negative testing

Submit:

- Missing counterparty reference.
- Missing instrument.
- Missing quantity.
- Missing price.
- Zero quantity.
- Negative quantity.
- Zero price.
- Negative price.
- Non-existing trade ID.

Expected result:

Invalid requests are rejected, and no invalid confirmation is persisted.

## 7.4 Confirmation mismatch

Submit a confirmation with a different quantity:

```json
{
  "counterpartyReference": "CONF-MISMATCH-001",
  "instrument": "NIFTY",
  "quantity": 15,
  "price": 25000,
  "buyer": "BUYER_A",
  "seller": "SELLER_B"
}
```

Expected result:

The confirmation is recorded if it passes validation, but matching must identify the quantity discrepancy.

---

# 8. Matching API Testing

## 8.1 Start matching run

```http
POST {{baseUrl}}/api/v1/matching/runs
```

Body:

```json
{
  "tradeIds": [
    {{tradeId}}
  ]
}
```

Expected HTTP status:

```text
201 Created
```

Expected output:

A MatchingRun object containing:

- Generated `runId`
- Run status
- Matched count
- Unmatched count
- Execution details

Save `runId` as `matchingRunId`.

## 8.2 Get all matching runs

```http
GET {{baseUrl}}/api/v1/matching/runs
```

Expected HTTP status:

```text
200 OK
```

Expected output:

An array of matching runs.

## 8.3 Get matching run

```http
GET {{baseUrl}}/api/v1/matching/runs/{{matchingRunId}}
```

Expected HTTP status:

```text
200 OK
```

Expected output:

The matching run and its execution counts.

## 8.4 Get matching results

```http
GET {{baseUrl}}/api/v1/matching/runs/{{matchingRunId}}/results
```

Expected HTTP status:

```text
200 OK
```

Expected output:

An array of MatchingResult objects.

For a matching confirmation, the result outcome should be:

```text
MATCHED
```

For a discrepancy, the result should be:

```text
UNMATCHED
```

## 8.5 Get matching exceptions

```http
GET {{baseUrl}}/api/v1/matching/exceptions
```

Expected HTTP status:

```text
200 OK
```

Expected output:

An array of unmatched matching results.

## 8.6 Retry matching

```http
POST {{baseUrl}}/api/v1/matching/runs/{{matchingRunId}}/retry
```

Expected HTTP status:

```text
201 Created
```

Expected output:

A new MatchingRun object for eligible retry records.

Negative cases:

- Non-existing run ID.
- Run with no eligible unmatched results.
- Duplicate trade IDs in a matching request.
- Empty trade ID list.
- Null or non-positive trade IDs.

Expected result:

Invalid requests are rejected. A completed run can still contain unmatched records, so verify individual outcomes rather than relying only on run status.

---

# 9. Settlement API Testing

## 9.1 Create settlement

Prerequisite: The trade must be in `CONFIRMED` status.

```http
POST {{baseUrl}}/api/v1/settlements
```

Body:

```json
{
  "tradeId": {{tradeId}},
  "settlementDate": "2026-11-13"
}
```

Expected HTTP status:

```text
201 Created
```

Expected output:

A SettlementInstruction object containing:

- Generated settlement ID.
- Associated trade ID.
- Settlement date.
- Status `CREATED`.

Save the generated settlement ID as `settlementId`.

## 9.2 Get all settlements

```http
GET {{baseUrl}}/api/v1/settlements
```

Expected HTTP status:

```text
200 OK
```

Expected output:

An array of settlement instructions.

## 9.3 Get settlement by ID

```http
GET {{baseUrl}}/api/v1/settlements/{{settlementId}}
```

Expected HTTP status:

```text
200 OK
```

Expected output:

The settlement instruction corresponding to the supplied ID.

## 9.4 Submit settlement

```http
POST {{baseUrl}}/api/v1/settlements/{{settlementId}}/submit
```

Expected HTTP status:

```text
200 OK
```

Expected output:

SettlementInstruction with status:

```text
SUBMITTED
```

The associated trade should move to:

```text
SETTLEMENT_PENDING
```

## 9.5 Confirm successful settlement

```http
POST {{baseUrl}}/api/v1/settlements/{{settlementId}}/confirm
```

Body:

```json
{
  "outcome": "SETTLED",
  "failureReason": null
}
```

Expected HTTP status:

```text
200 OK
```

Expected output:

SettlementInstruction with status:

```text
SETTLED
```

The associated trade should reach:

```text
SETTLED
```

## 9.6 Confirm failed settlement

Use a separate settlement that has reached `SUBMITTED`.

```http
POST {{baseUrl}}/api/v1/settlements/{{settlementId}}/confirm
```

Body:

```json
{
  "outcome": "FAILED",
  "failureReason": "Simulated settlement processing failure"
}
```

Expected HTTP status:

```text
200 OK
```

Expected output:

Settlement status:

```text
FAILED
```

## 9.7 Confirm rejected settlement

Use another submitted settlement.

```http
POST {{baseUrl}}/api/v1/settlements/{{settlementId}}/confirm
```

Body:

```json
{
  "outcome": "REJECTED",
  "failureReason": "Counterparty rejected settlement"
}
```

Expected output:

Settlement status:

```text
REJECTED
```

## 9.8 Retry settlement

Retry is permitted for failed settlements.

```http
POST {{baseUrl}}/api/v1/settlements/{{settlementId}}/retry
```

Expected HTTP status:

```text
200 OK
```

Expected output:

Settlement status:

```text
RETRY_PENDING
```

Test retry on:

- `SETTLED`
- `REJECTED`
- `SUBMITTED`
- `CREATED`
- Non-existing settlement

Expected result:

Only eligible failed settlements should be retried.

## 9.9 Settlement status history

```http
GET {{baseUrl}}/api/v1/settlements/{{settlementId}}/status-history
```

Expected HTTP status:

```text
200 OK
```

Expected output:

An array containing settlement lifecycle events in chronological order.

## 9.10 Settlement audit history

```http
GET {{baseUrl}}/api/v1/settlements/{{settlementId}}/audit
```

Expected HTTP status:

```text
200 OK
```

Expected output:

An array of settlement audit events.

---

# 10. Reconciliation API Testing

## 10.1 Start reconciliation

Prerequisite: Prepare an internal settlement and corresponding external settlement data.

```http
POST {{baseUrl}}/api/v1/reconciliation/runs
```

Body:

```json
{
  "source": "EXTERNAL_SETTLEMENT_SYSTEM",
  "settlementDate": "2026-11-13",
  "externalRecords": [
    {
      "externalReference": "EXT-001",
      "settlementReference": "SETTLE-001",
      "instrument": "NIFTY",
      "quantity": 10,
      "price": 25000,
      "settlementDate": "2026-11-13",
      "status": "SETTLED"
    }
  ]
}
```

Expected HTTP status:

```text
201 Created
```

Expected output:

A ReconciliationRun object containing:

- Generated run ID.
- Source.
- Settlement date.
- Run status.
- Total record count.
- Matched count.
- Mismatched count.
- Missing internal count.
- Missing external count.

Save the generated run ID as `reconciliationRunId`.

Important: The example references must correspond to actual internal settlement records for a `MATCHED` outcome. Replace `SETTLE-001` with the reference used by the internal settlement in your test environment.

## 10.2 Get reconciliation runs

```http
GET {{baseUrl}}/api/v1/reconciliation/runs
```

Expected HTTP status:

```text
200 OK
```

Expected output:

An array of reconciliation runs.

## 10.3 Get reconciliation run

```http
GET {{baseUrl}}/api/v1/reconciliation/runs/{{reconciliationRunId}}
```

Expected HTTP status:

```text
200 OK
```

Expected output:

The selected reconciliation run with its counts and status.

## 10.4 Get reconciliation results

```http
GET {{baseUrl}}/api/v1/reconciliation/runs/{{reconciliationRunId}}/results
```

Expected HTTP status:

```text
200 OK
```

Expected output:

An array of ReconciliationResult objects.

Expected result statuses:

```text
MATCHED
MISMATCHED
MISSING_INTERNAL
MISSING_EXTERNAL
```

## 10.5 Reconciliation mismatch

Change the external quantity:

```json
{
  "source": "EXTERNAL_SETTLEMENT_SYSTEM",
  "settlementDate": "2026-11-13",
  "externalRecords": [
    {
      "externalReference": "EXT-002",
      "settlementReference": "ACTUAL_INTERNAL_SETTLEMENT_REFERENCE",
      "instrument": "NIFTY",
      "quantity": 20,
      "price": 25000,
      "settlementDate": "2026-11-13",
      "status": "SETTLED"
    }
  ]
}
```

Expected result:

```text
MISMATCHED
```

A corresponding reconciliation break should be generated.

## 10.6 Missing external record

Run reconciliation with an empty external record list only if the request and implementation permit that input. The current request DTO requires a non-empty list, so this particular scenario cannot be initiated through an empty list.

Instead, supply external records for other settlements while leaving the selected internal settlement absent.

Expected result for the omitted internal record:

```text
MISSING_EXTERNAL
```

## 10.7 Missing internal record

Supply an external record with a settlement reference that does not exist internally.

Expected result:

```text
MISSING_INTERNAL
```

## 10.8 Reconciliation negative tests

Test:

- Missing source.
- Empty source.
- Missing settlement date.
- Empty external record list.
- Missing external reference.
- Missing settlement reference.
- Missing instrument.
- Invalid quantity.
- Invalid price.
- Duplicate external references.
- Duplicate settlement references.
- Non-existing reconciliation run ID.

Expected result:

Invalid requests should be rejected, and failed reconciliation should not leave partial run, result, or break records.

---

# 11. Break Management API Testing

## 11.1 Retrieve breaks

```http
GET {{baseUrl}}/api/v1/breaks
```

Expected HTTP status:

```text
200 OK
```

Expected output:

A paginated response containing break records.

## 11.2 Filter breaks

By status:

```http
GET {{baseUrl}}/api/v1/breaks?status=OPEN
```

By category:

```http
GET {{baseUrl}}/api/v1/breaks?category=MISMATCH
```

By reconciliation run:

```http
GET {{baseUrl}}/api/v1/breaks?runId={{reconciliationRunId}}
```

Pagination:

```http
GET {{baseUrl}}/api/v1/breaks?page=0&size=20
```

Expected result:

Only records matching the supplied filters should be returned.

## 11.3 Get break by ID

```http
GET {{baseUrl}}/api/v1/breaks/{{breakId}}
```

Expected HTTP status:

```text
200 OK
```

Expected output:

A BreakResponse containing the selected break's information.

## 11.4 Assign break

```http
POST {{baseUrl}}/api/v1/breaks/{{breakId}}/assign
```

Body:

```json
{
  "assignee": "operations-user"
}
```

Expected HTTP status:

```text
200 OK
```

Expected output:

BreakResponse with the assigned user.

## 11.5 Update break

```http
PATCH {{baseUrl}}/api/v1/breaks/{{breakId}}
```

Body:

```json
{
  "priority": "HIGH"
}
```

Expected HTTP status:

```text
200 OK
```

Expected output:

BreakResponse reflecting the updated priority.

## 11.6 Resolve break

```http
POST {{baseUrl}}/api/v1/breaks/{{breakId}}/resolve
```

Body:

```json
{
  "resolutionNotes": "External settlement details verified and discrepancy resolved."
}
```

Expected HTTP status:

```text
200 OK
```

Expected output:

BreakResponse with resolved status and resolution details.

## 11.7 Break summary

```http
GET {{baseUrl}}/api/v1/breaks/summary
```

Expected HTTP status:

```text
200 OK
```

Expected output:

BreakSummaryResponse containing summary counts.

Verify that summary counts agree with the actual break records.

---

# 12. Kafka Testing

The application uses Kafka for business event publishing and consumption.

The primary topic is:

```text
trade-events
```

Consumer groups:

```text
trade-consumer-group
settlement-consumer-group
reconciliation-consumer-group
```

## 12.1 Verify Kafka availability

Using Docker:

```bash
docker exec -it settledesk-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 --list
```

Expected result:

Kafka responds successfully and the expected topic is available after initialization.

## 12.2 Verify event publishing

Perform a trade creation through Postman.

```http
POST {{baseUrl}}/api/v1/trades
```

Expected event:

```text
TradeCreated
```

Verify the event on the Kafka topic.

Example:

```bash
docker exec -it settledesk-kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic trade-events \
  --from-beginning
```

Expected result:

A serialized business event containing event metadata, event type, entity information, timestamp, version, and payload.

## 12.3 Events to verify

| Operation | Expected event |
|---|---|
| Trade creation | TradeCreated |
| Trade confirmation | TradeConfirmed |
| Successful matching | TradeMatched |
| Matching failure | TradeMatchingFailed |
| Settlement creation | SettlementInstructionCreated |
| Settlement completion | SettlementCompleted |
| Settlement failure | SettlementFailed |
| Reconciliation completion | ReconciliationCompleted |
| Break detection | ReconciliationBreakDetected |

Verify the event payload against the actual business operation.

## 12.4 Consumer testing

The repository includes `TradeEventConsumerIntegrationTest`.

Test:

- TradeCreated event consumption.
- Processed event persistence.
- Duplicate event handling.
- Complete trade-to-settlement workflow.
- Reconciliation event processing.

Expected result:

The appropriate consumer processes the event and records the event ID in processed-event storage.

## 12.5 Duplicate event testing

Publish the same event twice using the same event ID.

Expected result:

- Only one processed-event record exists for the relevant consumer group and event ID.
- Duplicate processing is prevented.

## 12.6 Retry and dead-letter testing

The Kafka configuration uses:

```java
FixedBackOff(1000L, 2L)
```

The configured behavior allows the initial delivery and two retry attempts.

Test:

- Retryable exception.
- Non-retryable `IllegalArgumentException`.
- Failure after retry exhaustion.
- Dead-letter routing.

The configured dead-letter topic naming is the original topic plus `.DLT`.

Example:

```text
trade-events.DLT
```

Expected result:

Retryable failures follow the configured retry policy. Non-retryable failures are not retried. Exhausted messages are routed to the configured dead-letter topic.

---

# 13. Outbox Testing

The outbox pattern is used to persist business events before publishing them to Kafka.

## 13.1 Successful event publishing

Procedure:

1. Create a trade.
2. Verify an outbox record is created.
3. Verify its initial status is `PENDING`.
4. Allow the publisher to process it.
5. Verify the event appears on Kafka.
6. Verify the outbox record changes to `PUBLISHED`.

Expected result:

The event is published and the outbox record reflects successful publishing.

## 13.2 Publishing failure

Procedure:

1. Make Kafka unavailable in a controlled environment.
2. Trigger a business operation that generates an event.
3. Inspect the outbox record.
4. Restore Kafka.
5. Observe publisher recovery.

Expected result:

A failed publish must not be incorrectly recorded as successfully published. The publisher should follow its implemented retry/recovery behavior.

## 13.3 Outbox unit testing

The repository contains `OutboxPublisherTest`.

Verify:

- Pending event retrieval.
- Successful publish.
- Status update after publishing.
- Failed publish handling.
- Correct event serialization.

---

# 14. End-to-End System Testing

These workflows validate the complete business system.

## 14.1 Successful trade lifecycle

Use separate generated IDs for each test run.

| Step | Operation | Expected result |
|---|---|---|
| 1 | Create trade | NEW |
| 2 | Create matching confirmation | Confirmation created |
| 3 | Start matching run | MATCHED |
| 4 | Update trade status | CONFIRMED |
| 5 | Create settlement | CREATED |
| 6 | Submit settlement | SUBMITTED |
| 7 | Confirm settlement | SETTLED |
| 8 | Retrieve trade | SETTLED |
| 9 | Retrieve settlement history | Full status history |
| 10 | Retrieve audit | Audit records present |

Expected final state:

```text
Trade: SETTLED
Settlement: SETTLED
Matching outcome: MATCHED
```

## 14.2 Matching failure workflow

1. Create trade.
2. Submit a confirmation with different quantity.
3. Start matching.
4. Retrieve matching results.
5. Retrieve matching exceptions.

Expected final state:

```text
Matching outcome: UNMATCHED
```

Verify that the trade has not incorrectly been marked as matched.

## 14.3 Settlement failure and recovery

1. Create trade.
2. Confirm trade.
3. Match trade.
4. Set trade status to `CONFIRMED`.
5. Create settlement.
6. Submit settlement.
7. Confirm with outcome `FAILED`.
8. Retry settlement.
9. Submit again.
10. Confirm with outcome `SETTLED`.

Expected final state:

```text
Trade: SETTLED
Settlement: SETTLED
```

Verify the full settlement history.

## 14.4 Reconciliation mismatch workflow

1. Create and settle a trade.
2. Submit an external settlement record with a different quantity.
3. Start reconciliation.
4. Retrieve reconciliation results.
5. Retrieve the generated break.
6. Assign the break.
7. Update priority.
8. Resolve the break.
9. Retrieve the break again.

Expected result:

```text
Reconciliation: MISMATCHED
Break initial status: OPEN
Break final status: RESOLVED
```

## 14.5 Missing internal record workflow

1. Prepare an external settlement record for a reference that does not exist internally.
2. Start reconciliation.
3. Retrieve reconciliation results.
4. Retrieve generated breaks.

Expected result:

```text
Reconciliation: MISSING_INTERNAL
Break category: MISSING_INTERNAL
```

---

# 15. Error Handling and Negative Testing

Run these tests across the relevant modules.

## 15.1 Validation errors

Test:

- Missing mandatory fields.
- Null fields.
- Empty strings.
- Invalid data types.
- Invalid enum values.
- Empty request bodies.
- Malformed JSON.
- Invalid dates.
- Zero and negative numeric values.

Expected result:

HTTP 400 for validation or request errors where handled by the application.

## 15.2 Missing resource errors

Test requests using:

- Non-existing trade ID.
- Non-existing matching run ID.
- Non-existing settlement ID.
- Non-existing reconciliation run ID.
- Non-existing break ID.

Expected result:

A suitable not-found response, normally HTTP 404 if mapped by the application's exception handling.

## 15.3 Invalid lifecycle operations

Test:

- Submit an already submitted settlement.
- Confirm a settlement before submission.
- Retry a settled settlement.
- Retry a rejected settlement.
- Create a settlement for an unconfirmed trade.
- Create duplicate settlement for the same trade.
- Resolve an already resolved break.
- Retry a matching run with no eligible records.

Expected result:

Business-rule violations should be rejected without changing the existing valid state.

---

# 16. Data Integrity Testing

Verify that:

- Trade IDs remain unique.
- Settlement IDs remain unique.
- Matching run IDs remain unique.
- Reconciliation run IDs remain unique.
- A trade does not receive multiple settlement instructions where prohibited.
- Matching results refer to valid trades.
- Reconciliation results refer to the correct reconciliation run.
- Break records refer to the correct reconciliation result/run.
- Audit records are associated with the correct entity.
- Lifecycle records reflect actual transitions.
- Processed event IDs are unique per consumer group.
- Outbox records are not marked published before successful publishing.

Compare API responses with persisted SQL Server records.

---

# 17. Performance and Concurrency Testing

These tests should be executed in a controlled environment, not against production.

## 17.1 Concurrent trade creation

Send multiple concurrent POST requests to:

```text
POST /api/v1/trades
```

Use different trade data for each request.

Expected result:

- Each valid request is handled.
- Trade IDs are unique.
- No unexpected database constraint violations.
- No lost records.

## 17.2 Concurrent settlement creation

Attempt to create two settlement instructions for the same trade concurrently.

Expected result:

Only one settlement should be created for that trade.

## 17.3 Concurrent settlement submission

Submit the same settlement multiple times concurrently.

Expected result:

The settlement must not enter an invalid state or produce duplicate business effects.

## 17.4 Bulk API testing

Use the bulk trade creation endpoint with increasing payload sizes.

Observe:

- Response time.
- Application memory.
- Database write time.
- Error rate.
- Persisted record count.

## 17.5 Load testing

A load testing tool such as k6 or JMeter can be used to evaluate:

- Trade creation throughput.
- Trade retrieval latency.
- Matching run execution time.
- Settlement processing time.
- Reconciliation processing time.
- Database behavior under concurrent requests.

Do not define performance thresholds until expected workload and infrastructure limits have been established.

---

# 18. Security Testing

The repository dependencies and controllers reviewed for this document do not establish a complete authentication/authorization contract. Do not assume that protected endpoints or specific roles exist.

Security testing should verify the actual deployed configuration.

Test:

- Whether endpoints require authentication, if authentication is configured.
- Whether users can access operations outside their intended permissions.
- SQL injection attempts in search parameters.
- Malformed and oversized JSON payloads.
- Sensitive information in error responses.
- Secrets in logs and application responses.
- Database credentials and Kafka configuration exposure.
- Dependency vulnerability scanning.

Example search input:

```http
GET {{baseUrl}}/api/v1/trades?instrument=' OR '1'='1
```

Expected result:

The input must be treated as data, not executable SQL.

---

# 19. CI/CD Testing

## 19.1 Maven test execution

```bash
./mvnw clean test
```

Expected result:

All configured unit and web-layer tests pass.

## 19.2 Integration testing

The repository includes Testcontainers-based integration tests.

Relevant classes include:

```text
KafkaIntegrationTest
SqlServerIntegrationTest
TradeEventConsumerIntegrationTest
```

These tests use containerized infrastructure to validate actual integration behavior.

Verify:

- Kafka container startup.
- SQL Server container startup.
- Spring application context initialization.
- Producer publishing.
- Consumer processing.
- Database persistence.
- Transaction rollback.
- Duplicate event prevention.
- End-to-end lifecycle processing.

## 19.3 Full build

```bash
./mvnw clean verify
```

Expected result:

The Maven verification lifecycle completes successfully, including the configured test phases.

Review:

```text
target/surefire-reports/
target/failsafe-reports/
```

The actual reports available depend on the Maven plugin configuration.

## 19.4 Docker build

```bash
docker build -t settledesk:testing .
```

Expected result:

The Docker image builds successfully.

Run the containerized application with the required database and Kafka dependencies and verify the same business workflows.

---

# 20. Test Result Interpretation

Use these outcomes when reviewing test execution.

| Result | Meaning |
|---|---|
| PASS | Actual result matches expected behavior |
| FAIL | Actual result differs from expected behavior |
| BLOCKED | Test cannot execute because a dependency or prerequisite is unavailable |
| NOT APPLICABLE | Scenario does not apply to the implemented feature |
| NEEDS INVESTIGATION | Behavior is unclear or requires code-level verification |

For each failed test, record:

- Test name.
- Module.
- Request or operation.
- Expected result.
- Actual result.
- Application logs.
- Relevant database state.
- Related event information.
- Root cause.
- Fix or follow-up.

---

# 21. Complete Project Testing Exit Criteria

A complete testing cycle should establish that:

- Application starts and connects to required infrastructure.
- Unit tests validate individual business rules.
- Repository and database operations persist correct data.
- REST endpoints accept valid requests and reject invalid requests.
- Trade lifecycle transitions are correct.
- Matching produces correct outcomes.
- Settlement processing and retry rules work.
- Reconciliation identifies mismatches and missing records.
- Breaks are generated and managed correctly.
- Kafka events are published and consumed correctly.
- Duplicate event processing is prevented.
- Outbox records reflect publishing outcomes.
- Database transactions roll back on critical failures.
- End-to-end workflows complete successfully.
- Concurrent operations preserve business constraints.
- Error responses are consistent.
- CI build and automated tests pass.

---

# 22. Repository Test Reference

The following existing test classes are useful starting points for reviewing automated coverage:

| Test class | Main responsibility |
|---|---|
| `KafkaIntegrationTest` | Kafka event publishing |
| `SqlServerIntegrationTest` | SQL Server connectivity and rollback |
| `TradeEventConsumerIntegrationTest` | Kafka consumption, idempotency and lifecycle integration |
| `TradeControllerTest` | Trade API behavior |
| `MatchingControllerTest` | Matching API behavior |
| `SettlementControllerTest` | Settlement API behavior |
| `ReconciliationControllerTest` | Reconciliation API behavior |
| `BreakControllerTest` | Break API behavior |
| `OutboxPublisherTest` | Outbox publisher behavior |
| `BusinessEventProducerTest` | Business event producer |
| `TradeEventConsumerTest` | Trade event consumer |
| `SettlementEventConsumerTest` | Settlement event consumer |
| `ReconciliationEventConsumerTest` | Reconciliation event consumer |
| `MatchingServiceTest` | Matching business logic |
| `SettlementServiceTest` | Settlement business logic |
| `ReconciliationServiceTest` | Reconciliation business logic |
| `BreakServiceTest` | Break management |
| `TradeServiceTest` | Trade business logic |

The test class list should be checked against the current branch before using it as an exhaustive inventory.

---

**Testing principle:** A passing API response alone does not prove the business operation completed correctly. Verify the response, database state, lifecycle/audit records, and Kafka/outbox behavior wherever those components participate in the operation.
