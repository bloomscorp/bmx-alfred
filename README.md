# bmx-alfred

A lightweight, database-backed micro-logging library for Spring Boot applications. **bmx-alfred** provides structured persistence of general application logs and user authentication events (login/logout) into a relational database, with built-in support for asynchronous non-blocking dispatch.

- **Group ID:** `com.bloomscorp`
- **Artifact ID:** `bmx-alfred`
- **Version:** `3.4.0.1`
- **Java:** 21
- **License:** MIT

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Dependencies](#dependencies)
4. [Installation](#installation)
5. [Database Schema](#database-schema)
6. [Core Concepts](#core-concepts)
   - [Log Severity Levels](#log-severity-levels)
   - [Authentication Actions](#authentication-actions)
   - [LogBook](#logbook)
   - [ILogBookDAO](#ilogbookdao)
   - [CronManager](#cronmanager)
   - [Reporter Payloads](#reporter-payloads)
   - [ReporterID](#reporterid)
   - [GsonExclude](#gsonexclude)
7. [Integration Guide](#integration-guide)
   - [Step 1 — Define your JPA entities](#step-1--define-your-jpa-entities)
   - [Step 2 — Implement ILogBookDAO](#step-2--implement-ilogbookdao)
   - [Step 3 — Extend LogBook](#step-3--extend-logbook)
   - [Step 4 — Register as a Spring Bean](#step-4--register-as-a-spring-bean)
   - [Step 5 — Inject CronManager and log](#step-5--inject-cronmanager-and-log)
8. [API Reference](#api-reference)
   - [LogBook](#logbook-api)
   - [CronManager](#cronmanager-api)
   - [ReporterID](#reporterid-api)
   - [ILogBookDAO](#ilogbookdao-api)
9. [Contract Constants](#contract-constants)
10. [Design Decisions](#design-decisions)
11. [Package Structure](#package-structure)

---

## Overview

Most application logging libraries (Logback, Log4j, SLF4J) write to files or stdout. **bmx-alfred** takes a different approach: it persists log entries directly into a relational database table alongside your application data, enabling:

- **Queryable logs** — filter, aggregate, and join log data with your domain data using standard SQL.
- **Authentication audit trail** — every login and logout event is recorded with the user identity and a timestamp.
- **Reporter traceability** — every log entry can embed a JSON reporter payload that identifies exactly which class and method produced the log.
- **Non-blocking dispatch** — the `CronManager` fires each log operation on a dedicated thread, so the calling request thread is never blocked by database I/O.

---

## Architecture

```
┌────────────────────────────────────────────────┐
│  Your Spring Boot Application                  │
│                                                │
│  ┌──────────────┐    ┌───────────────────────┐ │
│  │  Controller  │───▶│  CronManager<B,L,A,…> │ │
│  │  Service     │    └──────────┬────────────┘ │
│  └──────────────┘               │ spawns Thread│
│                                 ▼              │
│                    ┌────────────────────────┐  │
│                    │  LoggerTask            │  │
│                    │  ExceptionLoggerTask   │  │
│                    │  AuthenticationLogger  │  │
│                    │         Task           │  │
│                    └──────────┬─────────────┘  │
│                               │ delegates      │
│                               ▼                │
│                    ┌──────────────────────┐    │
│                    │  LogBook<L,A,T,E,R>  │    │
│                    │  (your subclass)     │    │
│                    └──────────┬───────────┘    │
│                               │                │
│                               ▼                │
│                    ┌──────────────────────┐    │
│                    │  ILogBookDAO<A,L>    │    │
│                    │  (your impl)         │    │
│                    └──────────┬───────────┘    │
│                               │                │
│                               ▼                │
│                    ┌──────────────────────┐    │
│                    │  Database            │    │
│                    │  ┌────┐ ┌──────────┐ │    │
│                    │  │log │ │auth_log  │ │    │
│                    │  └────┘ └──────────┘ │    │
│                    └──────────────────────┘    │
└────────────────────────────────────────────────┘
```

The library provides the abstract skeleton (`LogBook`, `CronManager`, `ILogBookDAO`, all ORM interfaces and enums, contracts, and utilities). Your application provides the concrete JPA entities, the DAO implementation, and the `LogBook` subclass that knows how to build entity instances.

---

## Dependencies

| Dependency | Version | Purpose |
|---|---|---|
| `org.projectlombok:lombok` | 1.18.44 | `@AllArgsConstructor` for constructor generation |
| `org.jetbrains:annotations` | 26.0.1 | `@NotNull`, `@Contract` nullability annotations |
| `com.google.code.gson:gson` | 2.11.0 | Serializing reporter payloads to JSON |
| `com.bloomscorp:bmx-pastebox-java` | 0.0.3 | `Pastebox.getStackTraceAsString` for exception serialization |
| `com.bloomscorp:bmx-nverse` | 3.4.0.2 | `NVerseTenant` and `NVerseRole` base types for tenant/role generics |

---

## Installation

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.bloomscorp</groupId>
    <artifactId>bmx-alfred</artifactId>
    <version>3.4.0.1</version>
</dependency>
```

---

## Database Schema

You are responsible for creating these tables in your database. The column names are defined in `LogContract` and `AuthenticationLogContract`.

### `log` table

```sql
CREATE TABLE log (
    id        BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version   INT          NOT NULL DEFAULT 0,
    logger    VARCHAR(512) NOT NULL,
    log_type  VARCHAR(32)  NOT NULL,
    message   TEXT         NOT NULL,
    data_dump LONGTEXT,
    time      DATETIME     NOT NULL
);
```

| Column | Type | Description |
|---|---|---|
| `id` | `BIGINT` | Auto-generated primary key |
| `version` | `INT` | Optimistic locking version |
| `logger` | `VARCHAR` | Reporter identifier — typically `ClassName#methodName` |
| `log_type` | `VARCHAR` | Severity level: one of `EMERGENCY`, `ALERT`, `CRITICAL`, `ERROR`, `WARNING`, `NOTICE`, `INFO`, `DEBUG` |
| `message` | `TEXT` | Human-readable description of the event |
| `data_dump` | `LONGTEXT` | Optional serialized payload (JSON body, stack trace, etc.) |
| `time` | `DATETIME` | Timestamp of the event |

### `authentication_log` table

```sql
CREATE TABLE authentication_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version     INT          NOT NULL DEFAULT 0,
    action      VARCHAR(16)  NOT NULL,
    user_id     BIGINT       NOT NULL,
    time        DATETIME     NOT NULL,
    attempt     INT          NOT NULL DEFAULT 1,
    information TEXT
);
```

| Column | Type | Description |
|---|---|---|
| `id` | `BIGINT` | Auto-generated primary key |
| `version` | `INT` | Optimistic locking version |
| `action` | `VARCHAR` | `LOGIN` or `LOGOUT` |
| `user_id` | `BIGINT` | ID of the user who performed the action |
| `time` | `DATETIME` | Timestamp of the event |
| `attempt` | `INT` | Attempt sequence number for the authentication event |
| `information` | `TEXT` | Additional contextual information |

---

## Core Concepts

### Log Severity Levels

`LOG_TYPE` follows the **syslog RFC 5424** severity convention. Choose the level that best matches the urgency of the event:

| Value | Syslog Severity | When to use |
|---|---|---|
| `EMERGENCY` | 0 | System is completely unusable |
| `ALERT` | 1 | Immediate human intervention required |
| `CRITICAL` | 2 | Hard failures — critical service down |
| `ERROR` | 3 | Operation failed; requires attention |
| `WARNING` | 4 | Unexpected but recoverable condition |
| `NOTICE` | 5 | Normal but notable business event |
| `INFO` | 6 | Routine informational message |
| `DEBUG` | 7 | Detailed diagnostic data for development |

> **Note:** When logging exceptions via `LogBook.log(Exception, String, String)`, the type is always fixed to `ERROR` automatically.

### Authentication Actions

`AUTH_ACTION_ENUM` has exactly two values:

| Value | Meaning |
|---|---|
| `LOGIN` | User successfully authenticated |
| `LOGOUT` | User terminated their session |

### LogBook

`LogBook<L, A, T, E, R>` is the **abstract central service** of the library. It is generic over five type parameters so it can work with any application's entity model:

| Type Parameter | Bound | Role |
|---|---|---|
| `L` | `Log` | Your concrete log JPA entity |
| `A` | `AuthenticationLog` | Your concrete authentication log JPA entity |
| `T` | `NVerseTenant<E, R>` | Your user/tenant entity |
| `E` | `Enum<E>` | Your role enum |
| `R` | `NVerseRole<E>` | Your role entity |

You extend `LogBook` and implement two factory methods:

- `buildLogInstance(logger, logType, message, dataDump)` — returns a new, unpersisted `L` entity.
- `buildAuthenticationLogInstance(action, user)` — returns a new, unpersisted `A` entity.

`LogBook` itself handles all routing: calling the right DAO method, formatting stack traces, serializing reporters to JSON.

### ILogBookDAO

`ILogBookDAO<A, L>` is the **persistence adapter interface**. You implement it once in your application using whatever persistence mechanism you use (Spring Data JPA, JDBC, etc.) and pass the implementation to your `LogBook` subclass via its constructor.

```
ILogBookDAO<A, L>
├── insertAuthenticationLog(A log) → long   (returns generated PK)
└── insertLog(L log) → long                (returns generated PK)
```

### CronManager

`CronManager<B, L, A, T, E, R>` wraps your `LogBook` subclass and exposes **asynchronous** equivalents of every logging operation. Each `schedule*` call creates a new `Thread` and starts it immediately, so the calling thread returns without waiting for the database write.

```
CronManager
├── scheduleLoginLogTask(user)
├── scheduleLogoutLogTask(user)
├── scheduleLogTask(message, logger, type, dataDump)
└── scheduleExceptionLogTask(exception, message, logger)
```

> **Thread model note:** Each call spawns a bare `new Thread(...)`. There is no thread pool or queue. This is appropriate for moderate log volumes. In applications with extremely high log throughput, consider wrapping your calls with an `ExecutorService` before passing them to `CronManager`, or contributing a pooled variant.

### Reporter Payloads

Every log entry can optionally carry a **reporter payload** — a small JSON object that identifies the origin of the log. There are two variants:

**Authenticated reporter** (`LogReporter`):
```json
{ "userID": 42, "reporterID": "UserService#createUser" }
```

**Unauthorized reporter** (`UnauthorizedLogReporter`):
```json
{ "user": "unauthorized", "reporterID": "AuthController#login" }
```

These are generated by `LogBook.prepareLogReporter(user, reporterID)` and `LogBook.prepareUnauthorizedLogReporter(reporterID)` respectively, and are typically stored in the `data_dump` column of the `log` table alongside or instead of other context data.

### ReporterID

`ReporterID` is a utility class with a single static method that generates the standard `"ClassName#methodName"` string:

```java
String id = ReporterID.prepareID("UserService", "createUser");
// → "UserService#createUser"
```

Always use this method when constructing reporter IDs to ensure consistent formatting across your application.

### GsonExclude

`@GsonExclude` is a runtime field annotation. When applied to a field in a reporter POJO, that field is silently omitted from the serialized JSON. The exclusion is enforced by `GsonExclusionStrategy`, which is registered on the shared `Gson` instance inside `LogBook`.

```java
public final class MyReporter {
    public long userID;

    @GsonExclude
    public String internalDebugInfo; // will NOT appear in JSON
}
```

---

## Integration Guide

### Step 1 — Define your JPA entities

Create two JPA entities: one implementing `Log`, one implementing `AuthenticationLog`. Use the column name constants from `LogContract` and `AuthenticationLogContract` in your `@Column` annotations to stay in sync with the library's expected schema.

```java
@Entity
@Table(name = LogContract.TABLE)
public class AppLog implements Log {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = LogContract.ID)
    private long id;

    @Version
    @Column(name = LogContract.VERSION)
    private int version;

    @Column(name = LogContract.LOGGER, nullable = false)
    private String logger;

    @Enumerated(EnumType.STRING)
    @Column(name = LogContract.LOG_TYPE, nullable = false)
    private LOG_TYPE logType;

    @Column(name = LogContract.MESSAGE, nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = LogContract.DATA_DUMP, columnDefinition = "LONGTEXT")
    private String dataDump;

    @Column(name = LogContract.TIME, nullable = false)
    private LocalDateTime time;

    // getters / setters or Lombok
}
```

```java
@Entity
@Table(name = AuthenticationLogContract.TABLE)
public class AppAuthenticationLog implements AuthenticationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = AuthenticationLogContract.ID)
    private long id;

    @Version
    @Column(name = AuthenticationLogContract.VERSION)
    private int version;

    @Enumerated(EnumType.STRING)
    @Column(name = AuthenticationLogContract.ACTION, nullable = false)
    private AUTH_ACTION_ENUM action;

    @Column(name = AuthenticationLogContract.USER_ID, nullable = false)
    private long userId;

    @Column(name = AuthenticationLogContract.TIME, nullable = false)
    private LocalDateTime time;

    @Column(name = AuthenticationLogContract.ATTEMPT)
    private int attempt;

    @Column(name = AuthenticationLogContract.INFORMATION, columnDefinition = "TEXT")
    private String information;

    // getters / setters or Lombok
}
```

### Step 2 — Implement ILogBookDAO

Create a Spring `@Repository` that delegates to your JPA repositories:

```java
@Repository
public class AppLogBookDAO implements ILogBookDAO<AppAuthenticationLog, AppLog> {

    private final AppLogRepository logRepository;
    private final AppAuthLogRepository authLogRepository;

    public AppLogBookDAO(AppLogRepository logRepository,
                         AppAuthLogRepository authLogRepository) {
        this.logRepository = logRepository;
        this.authLogRepository = authLogRepository;
    }

    @Override
    public long insertAuthenticationLog(AppAuthenticationLog log) {
        return this.authLogRepository.save(log).getId();
    }

    @Override
    public long insertLog(AppLog log) {
        return this.logRepository.save(log).getId();
    }
}
```

### Step 3 — Extend LogBook

Create a `@Service` that extends `LogBook` and implements the two entity factory methods:

```java
@Service
public class AppLogBook extends LogBook<
    AppLog,
    AppAuthenticationLog,
    AppUser,           // your NVerseTenant subclass
    AppRoleEnum,       // your role enum
    AppRole            // your NVerseRole subclass
> {

    public AppLogBook(ILogBookDAO<AppAuthenticationLog, AppLog> repository) {
        super(repository);
    }

    @Override
    public AppLog buildLogInstance(
        String logger,
        LOG_TYPE logType,
        String message,
        String dataDump
    ) {
        AppLog log = new AppLog();
        log.setLogger(logger);
        log.setLogType(logType);
        log.setMessage(message);
        log.setDataDump(dataDump);
        log.setTime(LocalDateTime.now());
        return log;
    }

    @Override
    public AppAuthenticationLog buildAuthenticationLogInstance(
        AUTH_ACTION_ENUM action,
        AppUser user
    ) {
        AppAuthenticationLog log = new AppAuthenticationLog();
        log.setAction(action);
        log.setUserId(user.getId());
        log.setTime(LocalDateTime.now());
        log.setAttempt(1);
        return log;
    }
}
```

### Step 4 — Register as a Spring Bean

Expose a `CronManager` bean in a `@Configuration` class:

```java
@Configuration
public class LoggingConfiguration {

    @Bean
    public CronManager<AppLogBook, AppLog, AppAuthenticationLog, AppUser, AppRoleEnum, AppRole>
    cronManager(AppLogBook logBook) {
        return new CronManager<>(logBook);
    }
}
```

### Step 5 — Inject CronManager and log

Inject the `CronManager` wherever you need logging:

```java
@Service
public class UserService {

    private final CronManager<AppLogBook, AppLog, AppAuthenticationLog, AppUser, AppRoleEnum, AppRole> cronManager;

    public UserService(CronManager<...> cronManager) {
        this.cronManager = cronManager;
    }

    public void createUser(AppUser requestingUser, CreateUserRequest request) {
        try {
            // ... business logic ...

            String reporter = this.cronManager.logBook()
                // if you exposed logBook via a getter, or call directly:
                .prepareLogReporter(requestingUser, ReporterID.prepareID("UserService", "createUser"));

            this.cronManager.scheduleLogTask(
                "User created successfully",
                ReporterID.prepareID("UserService", "createUser"),
                LOG_TYPE.INFO,
                reporter
            );

        } catch (Exception e) {
            this.cronManager.scheduleExceptionLogTask(
                e,
                "Failed to create user",
                ReporterID.prepareID("UserService", "createUser")
            );
        }
    }

    public ResponseEntity<?> login(AppUser user) {
        // ... authentication logic ...
        this.cronManager.scheduleLoginLogTask(user);
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<?> logout(AppUser user) {
        // ... session termination logic ...
        this.cronManager.scheduleLogoutLogTask(user);
        return ResponseEntity.ok().build();
    }
}
```

**Logging from an unauthenticated context** (e.g., a public endpoint or filter):

```java
String reporter = this.appLogBook.prepareUnauthorizedLogReporter(
    ReporterID.prepareID("AuthController", "login")
);

this.cronManager.scheduleLogTask(
    "Login attempt from unknown user",
    ReporterID.prepareID("AuthController", "login"),
    LOG_TYPE.WARNING,
    reporter
);
```

---

## API Reference

### LogBook API

| Method | Description |
|---|---|
| `log(String message, String logger, LOG_TYPE type, String dataDump)` | Synchronously persists a structured log entry. |
| `log(Exception exception, String message, String logger)` | Synchronously persists an `ERROR`-level log with the full stack trace as `dataDump`. |
| `logLogin(T user)` | Synchronously records a `LOGIN` authentication event. |
| `logLogout(T user)` | Synchronously records a `LOGOUT` authentication event. |
| `prepareLogReporter(T user, String reporterID)` | Returns a JSON string `{"userID":…,"reporterID":…}` for embedding in a log entry. |
| `prepareUnauthorizedLogReporter(String reporterID)` | Returns a JSON string `{"user":"unauthorized","reporterID":…}` for embedding in a log entry. |
| `buildLogInstance(String logger, LOG_TYPE logType, String message, String dataDump)` | **Abstract.** Factory method — construct and return a new unpersisted `L` entity. |
| `buildAuthenticationLogInstance(AUTH_ACTION_ENUM action, T user)` | **Abstract.** Factory method — construct and return a new unpersisted `A` entity. |

### CronManager API

| Method | Description |
|---|---|
| `scheduleLoginLogTask(T user)` | Async — records a login event on a new thread. |
| `scheduleLogoutLogTask(T user)` | Async — records a logout event on a new thread. |
| `scheduleLogTask(String message, String logger, LOG_TYPE type, String dataDump)` | Async — persists a structured log entry on a new thread. |
| `scheduleExceptionLogTask(Exception exception, String message, String logger)` | Async — persists an `ERROR`-level exception log on a new thread. |

### ReporterID API

| Method | Description |
|---|---|
| `static prepareID(String className, String methodName)` | Returns `"className#methodName"`. Pure function — no side effects. |

### ILogBookDAO API

| Method | Description |
|---|---|
| `insertAuthenticationLog(A log)` | Persist an authentication log entity. Returns generated primary key. |
| `insertLog(L log)` | Persist a log entity. Returns generated primary key. |

---

## Contract Constants

### LogContract

| Constant | Value | Maps to |
|---|---|---|
| `TABLE` | `"log"` | Table name |
| `ID` | `"id"` | Primary key column |
| `VERSION` | `"version"` | Optimistic lock version |
| `LOGGER` | `"logger"` | Reporter identifier |
| `LOG_TYPE` | `"log_type"` | Severity enum value |
| `MESSAGE` | `"message"` | Log message text |
| `DATA_DUMP` | `"data_dump"` | Supplementary serialized data |
| `TIME` | `"time"` | Event timestamp |

### AuthenticationLogContract

| Constant | Value | Maps to |
|---|---|---|
| `TABLE` | `"authentication_log"` | Table name |
| `ID` | `"id"` | Primary key column |
| `VERSION` | `"version"` | Optimistic lock version |
| `ACTION` | `"action"` | `LOGIN` or `LOGOUT` enum value |
| `USER_ID` | `"user_id"` | Authenticated user's ID |
| `TIME` | `"time"` | Event timestamp |
| `ATTEMPT` | `"attempt"` | Authentication attempt sequence |
| `INFORMATION` | `"information"` | Additional context text |

---

## Design Decisions

**Why abstract `LogBook` instead of a concrete service?**
Different applications have different JPA entity shapes, timestamp strategies, and field populations. Rather than imposing a fixed schema, the library provides marker interfaces (`Log`, `AuthenticationLog`) and abstract factory methods. Your implementation knows exactly how to build its own entities.

**Why bare threads in `CronManager` instead of a thread pool?**
The library deliberately avoids taking an opinionated stance on your application's concurrency model. A bare thread keeps the library dependency-free from `ExecutorService` configuration. For high-throughput use cases, wrap your `CronManager` calls with your own `ExecutorService` before delegating.

**Why Gson for reporter serialization instead of Jackson?**
The library avoids a hard dependency on Jackson (which many Spring Boot applications customize heavily). Gson is lightweight, has a stable API, and its `ExclusionStrategy` makes it easy to control which fields appear in the reporter JSON via the `@GsonExclude` annotation.

**Why syslog severity levels?**
RFC 5424 syslog levels are a well-understood, widely-adopted convention. They provide an unambiguous vocabulary for expressing urgency without requiring application-specific tuning. Mapping to syslog also makes it straightforward to forward database log entries to external syslog or log aggregation systems.

**Why are `Log` and `AuthenticationLog` interfaces rather than abstract classes?**
Java supports single inheritance. Using marker interfaces lets consuming applications inherit from whatever base class their ORM or framework requires (e.g., a Hibernate base entity with audit fields), while still satisfying the library's type bounds.

---

## Package Structure

```
com.bloomscorp.alfred
├── LogBook.java                          Abstract logging service — extend this
│
├── adapter/
│   └── ILogBookDAO.java                  Persistence adapter — implement this
│
├── configuration/
│   ├── GsonExclude.java                  Field annotation to exclude from reporter JSON
│   └── GsonExclusionStrategy.java        Gson ExclusionStrategy implementation
│
├── contract/
│   ├── AuthenticationLogContract.java    Column name constants for authentication_log
│   └── LogContract.java                  Column name constants for log
│
├── cron/
│   ├── CronManager.java                  Async dispatch — inject and use this
│   └── task/
│       ├── AuthenticationLoggerTask.java  Runnable for login/logout events
│       ├── ExceptionLoggerTask.java       Runnable for exception logging
│       └── LoggerTask.java               Runnable for general log entries
│
├── orm/
│   ├── AUTH_ACTION_ENUM.java             LOGIN / LOGOUT enum
│   ├── AuthenticationLog.java            Marker interface for auth log entities
│   ├── LOG_TYPE.java                     Severity level enum (syslog RFC 5424)
│   ├── Log.java                          Marker interface for log entities
│   ├── LogReporter.java                  Reporter POJO for authenticated users
│   └── UnauthorizedLogReporter.java      Reporter POJO for unauthenticated requests
│
└── support/
    └── ReporterID.java                   Utility for generating ClassName#methodName IDs
```
