# 🚀 LinkScale

### A Production-Style Distributed Social Platform Built with Spring Boot & Microservices

<p align="center">
  <strong>Scalable • Event-Driven • Secure • Searchable • Distributed</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk" />
  <img src="https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen?style=for-the-badge&logo=springboot" />
  <img src="https://img.shields.io/badge/Spring%20Cloud-Gateway-blue?style=for-the-badge&logo=spring" />
  <img src="https://img.shields.io/badge/Apache%20Kafka-KRaft-black?style=for-the-badge&logo=apachekafka" />
  <img src="https://img.shields.io/badge/Redis-Cache-red?style=for-the-badge&logo=redis" />
  <img src="https://img.shields.io/badge/Elasticsearch-Search-yellow?style=for-the-badge&logo=elasticsearch" />
  <img src="https://img.shields.io/badge/MySQL-Database-blue?style=for-the-badge&logo=mysql" />
  <img src="https://img.shields.io/badge/Docker-Containerized-2496ED?style=for-the-badge&logo=docker" />
</p>

---

## 📌 Overview

**LinkScale** is a production-style distributed social networking backend inspired by platforms such as LinkedIn.

The project is designed to demonstrate how a large backend system can be decomposed into **independently deployable microservices**, while still providing a seamless experience to clients.

Instead of building everything inside a single monolithic application, LinkScale separates responsibilities into dedicated services for:

* 👤 User management
* 🔐 Authentication & authorization
* 📝 Post management
* 📰 Personalized feeds
* 🔎 Full-text search
* 🔔 Event-driven notifications
* ⚡ Distributed caching
* 📨 Asynchronous communication

The system combines **synchronous REST communication** with **asynchronous Kafka-based event processing** to achieve loose coupling and scalability.

> **The primary goal of LinkScale is not just to build a social platform, but to demonstrate real-world backend engineering concepts using Java, Spring Boot, distributed systems, event-driven architecture, caching, search infrastructure, and security.**

---

# 🏗️ Architecture

LinkScale follows a **microservices architecture** where each service owns a specific business capability.

```text
                                      ┌──────────────────────┐
                                      │       CLIENT         │
                                      │ Web / Mobile / API   │
                                      └──────────┬───────────┘
                                                 │
                                                 │ HTTP
                                                 ▼
                              ┌────────────────────────────────┐
                              │          API GATEWAY            │
                              │                                │
                              │   Spring Cloud Gateway         │
                              │   WebFlux                       │
                              │   JWT Authentication            │
                              │   Routing                       │
                              │   Request Filtering             │
                              └───────────────┬────────────────┘
                                              │
                  ┌───────────────────────────┼───────────────────────────┐
                  │                           │                           │
                  ▼                           ▼                           ▼
        ┌──────────────────┐       ┌──────────────────┐       ┌──────────────────┐
        │   USER SERVICE   │       │   POST SERVICE   │       │   FEED SERVICE   │
        │                  │       │                  │       │                  │
        │ Authentication   │       │ Create Post      │       │ Feed Generation  │
        │ User Profile     │       │ Update Post      │       │ Feed Retrieval   │
        │ User Management  │       │ Delete Post      │       │ Feed Caching     │
        └────────┬─────────┘       └────────┬─────────┘       └────────┬─────────┘
                 │                          │                          │
                 │                          │                          │
                 ▼                          ▼                          ▼
        ┌──────────────────┐       ┌──────────────────┐       ┌──────────────────┐
        │      MySQL       │       │      MySQL       │       │      Redis       │
        │                  │       │                  │       │                  │
        │ User Data        │       │ Post Data        │       │ Feed Cache       │
        │ Credentials      │       │ Relationships    │       │ Hot Data         │
        └──────────────────┘       └──────────────────┘       └──────────────────┘


                         ASYNCHRONOUS EVENT FLOW
                         ───────────────────────

          ┌─────────────────────────────────────────────────────────┐
          │                     APACHE KAFKA                         │
          │                       KRaft Mode                        │
          │                                                         │
          │  user.created   user.updated   post.created             │
          └───────────────┬───────────────┬─────────────────────────┘
                          │               │
                ┌─────────┘               └───────────────┐
                ▼                                         ▼
      ┌──────────────────────┐                 ┌──────────────────────┐
      │   SEARCH SERVICE     │                 │ NOTIFICATION SERVICE │
      │                      │                 │                      │
      │ Elasticsearch        │                 │ Event Consumers      │
      │ Full-text Search     │                 │ Notifications        │
      │ User Search          │                 │ Async Processing     │
      │ Post Search          │                 │                      │
      └──────────┬───────────┘                 └──────────────────────┘
                 │
                 ▼
      ┌──────────────────────┐
      │    ELASTICSEARCH     │
      │                      │
      │ User Index           │
      │ Post Index           │
      │ Fuzzy Search         │
      │ Multi-field Search   │
      └──────────────────────┘
```

---

# 🧩 Microservices

| Service                     | Responsibility                          | Main Technologies                  |
| --------------------------- | --------------------------------------- | ---------------------------------- |
| 🚪 **API Gateway**          | Entry point, routing & authentication   | Spring Cloud Gateway, WebFlux, JWT |
| 👤 **User Service**         | Registration, authentication & profiles | Spring Boot, MySQL                 |
| 📝 **Post Service**         | Post creation & management              | Spring Boot, MySQL                 |
| 📰 **Feed Service**         | Personalized feed generation & caching  | Spring Boot, Redis                 |
| 🔎 **Search Service**       | Full-text search for users & posts      | Spring Data Elasticsearch          |
| 🔔 **Notification Service** | Event-driven notifications              | Spring Boot, Kafka                 |

---

# 🔐 Authentication & Security

Authentication is centralized at the **API Gateway**.

```text
             Client
                │
                │ Authorization: Bearer <JWT>
                ▼
        ┌─────────────────┐
        │   API Gateway   │
        └────────┬────────┘
                 │
                 ▼
          JWT Validation
                 │
          ┌──────┴──────┐
          │             │
       Invalid        Valid
          │             │
          ▼             ▼
        401          Extract Claims
                        │
                        ▼
                X-User-Id
                X-User-Email
                        │
                        ▼
              Downstream Service
```

### Security flow

1. Client authenticates through the User Service.
2. User Service issues a JWT.
3. Client sends the JWT with subsequent requests.
4. API Gateway validates the JWT.
5. Gateway extracts user identity from the token.
6. Gateway forwards trusted identity headers to downstream services.
7. Downstream services can focus on business logic instead of repeatedly implementing authentication logic.

### Benefits

* Centralized authentication
* Consistent security enforcement
* Reduced authentication overhead
* Clean separation between security and business logic
* Downstream services remain simpler

---

# 📨 Event-Driven Architecture

LinkScale uses **Apache Kafka** to decouple services.

Instead of tightly coupling services with synchronous REST calls, important business events are published to Kafka.

### Example: User Registration

```text
User Registration
       │
       ▼
┌─────────────────┐
│  User Service   │
└────────┬────────┘
         │
         │ user.created
         ▼
┌─────────────────┐
│      Kafka      │
└───────┬─────────┘
        │
        ├───────────────┐
        │               │
        ▼               ▼
┌───────────────┐ ┌────────────────────┐
│Search Service │ │Notification Service│
└───────┬───────┘ └────────────────────┘
        │
        ▼
┌────────────────┐
│ Elasticsearch  │
└────────────────┘
```

### Current Events

| Event          | Producer     | Consumers                    |
| -------------- | ------------ | ---------------------------- |
| `user.created` | User Service | Search, Notification         |
| `user.updated` | User Service | Search                       |
| `post.created` | Post Service | Search, Feed/other consumers |

This architecture allows new consumers to be introduced without modifying the original producer.

---

# 🔎 Search Architecture

The Search Service uses **Elasticsearch** instead of relying exclusively on relational database queries.

This enables:

* Full-text search
* Fuzzy matching
* Multi-field search
* Fast text retrieval
* Search across users and posts

### Search Flow

```text
                 User / Client
                      │
                      ▼
               API Gateway
                      │
                      ▼
               Search Service
                      │
                      ▼
              Elasticsearch
                 ┌────┴────┐
                 │         │
                 ▼         ▼
              Users      Posts
```

Search indexes are maintained asynchronously using Kafka events.

For example:

```text
User Created
     │
     ▼
User Service
     │
     │ user.created
     ▼
Kafka
     │
     ▼
Search Service
     │
     ▼
Elasticsearch Index
```

This avoids coupling the User Service directly to Elasticsearch.

---

# ⚡ Redis Caching

The Feed Service uses **Redis** to cache frequently accessed feed data.

Without caching:

```text
Client
  │
  ▼
Feed Service
  │
  ▼
Database
  │
  ▼
Complex Feed Query
```

With Redis:

```text
Client
  │
  ▼
Feed Service
  │
  ▼
Redis Cache
  │
  ├──── Cache Hit ────► Return Feed
  │
  └──── Cache Miss
           │
           ▼
        Database
           │
           ▼
       Build Feed
           │
           ▼
       Store Redis
```

### Why Redis?

* Reduces database load
* Improves response latency
* Useful for frequently requested feed data
* Supports high-throughput read workloads
* Allows the application to scale independently from the database

---

# 🗄️ Data Architecture

LinkScale follows a **polyglot persistence** approach.

Each infrastructure component is selected according to its workload.

```text
┌─────────────────────────────────────────────────────────┐
│                    DATA LAYER                           │
├─────────────────┬───────────────────┬───────────────────┤
│      MySQL      │       Redis       │   Elasticsearch   │
├─────────────────┼───────────────────┼───────────────────┤
│ Relational Data │ Cached Data       │ Search Data       │
│ Users           │ Feed              │ Users             │
│ Posts           │ Hot Data          │ Posts             │
│ Jobs            │                   │                   │
└─────────────────┴───────────────────┴───────────────────┘
```

### MySQL

Used for structured relational data:

* Users
* Posts
* Relationships
* Persistent application data

### Redis

Used for:

* Feed caching
* Frequently accessed data
* Performance optimization

### Elasticsearch

Used for:

* User search
* Post search
* Full-text indexing
* Fuzzy search

---

# 🛠️ Technology Stack

## Backend

* **Java 21**
* **Spring Boot**
* **Spring Web**
* **Spring Cloud Gateway**
* **Spring WebFlux**
* **Spring Data JPA**
* **Spring Data Elasticsearch**

## Security

* **JWT**
* **JJWT**
* Gateway-level authentication
* Identity propagation through trusted headers

## Databases

* **MySQL**
* **Redis**
* **Elasticsearch**

## Messaging

* **Apache Kafka**
* **Kafka KRaft Mode**
* Event-driven architecture

## Infrastructure

* **Docker**
* **Docker Compose**
* Maven

---

# 📁 Project Structure

```text
LinkScale/
│
├── api-gateway/
│   └── API Gateway & JWT Authentication
│
├── user-service/
│   └── User Management & Authentication
│
├── post-service/
│   └── Post Management
│
├── feed-service/
│   └── Personalized Feed & Redis Caching
│
├── search-service/
│   └── Elasticsearch-based Search
│
├── notification-service/
│   └── Kafka Event Consumers & Notifications
│
├── docker-compose.yml
│
└── README.md
```

Each service is designed as an independently deployable application.

---

# 🔄 End-to-End Request Flow

Consider a user creating a post.

```text
                         POST /api/v1/posts
                                  │
                                  ▼
                         ┌─────────────────┐
                         │   API Gateway   │
                         └────────┬────────┘
                                  │
                           Validate JWT
                                  │
                                  ▼
                         ┌─────────────────┐
                         │  Post Service   │
                         └────────┬────────┘
                                  │
                                  ▼
                               MySQL
                                  │
                            Post persisted
                                  │
                                  ▼
                         Publish post.created
                                  │
                                  ▼
                         ┌─────────────────┐
                         │      Kafka      │
                         └───────┬─────────┘
                                 │
                    ┌────────────┴────────────┐
                    │                         │
                    ▼                         ▼
             Search Service          Notification Service
                    │
                    ▼
              Elasticsearch
```

The important part is that the Post Service does **not need to synchronously call every downstream service**.

This reduces coupling and makes the system easier to evolve.

---

# 🎯 Key Engineering Concepts Demonstrated

This project was built to explore and demonstrate real backend engineering concepts.

### 🏛️ Distributed Systems

* Service decomposition
* Independent service ownership
* Synchronous vs asynchronous communication
* Event-driven architecture
* Failure isolation

### 🔐 Security

* JWT authentication
* Gateway security
* Authentication filters
* Identity propagation
* Protected APIs

### ⚡ Performance

* Redis caching
* Asynchronous processing
* Elasticsearch indexing
* Efficient data access
* Reduced database pressure

### 📨 Messaging

* Kafka producers
* Kafka consumers
* Domain events
* Event-driven workflows
* Service decoupling

### 🔎 Search

* Elasticsearch
* Full-text search
* Fuzzy matching
* Multi-field search
* Asynchronous index synchronization

### 🐳 Infrastructure

* Docker
* Docker Compose
* Local distributed infrastructure
* Kafka KRaft mode

---

# 🧠 Important Architecture Decisions

## 1. Why Microservices?

The project intentionally separates business capabilities into independently deployable services.

This provides:

* Independent scaling
* Clear ownership
* Smaller codebases
* Better fault isolation
* Easier future evolution

---

## 2. Why API Gateway?

The API Gateway acts as the single entry point for clients.

Instead of exposing every service directly:

```text
Client
  │
  ├── User Service
  ├── Post Service
  ├── Feed Service
  └── Search Service
```

clients communicate through:

```text
Client
   │
   ▼
API Gateway
   │
   ├── User Service
   ├── Post Service
   ├── Feed Service
   └── Search Service
```

This provides a central place for:

* Authentication
* Routing
* Request filtering
* Cross-cutting concerns

---

## 3. Why Kafka?

Direct service-to-service communication can create strong dependencies.

Instead:

```text
Service A ─────► Service B
```

LinkScale can use:

```text
Service A
    │
    ▼
  Kafka
    │
    ├────► Service B
    ├────► Service C
    └────► Service D
```

New consumers can subscribe to events without changing the producer.

---

## 4. Why Elasticsearch?

Relational databases are excellent for transactional data, but search-heavy workloads require specialized indexing.

Elasticsearch provides:

* Inverted indexes
* Full-text search
* Fuzzy matching
* Multi-field search
* Fast retrieval

Therefore:

> **MySQL remains the source of truth while Elasticsearch acts as the search-optimized representation.**

---

## 5. Why Redis?

Feeds are frequently requested and can involve expensive aggregation.

Redis provides a fast cache layer between the application and database.

This reduces:

* Database reads
* Query frequency
* Response latency
* Computational overhead

---

# 🚀 Getting Started

## Prerequisites

Make sure the following are installed:

* Java 21
* Maven
* Docker
* Docker Compose
* Git

Verify:

```bash
java -version
mvn -version
docker --version
docker compose version
```

---

# 📥 Clone the Repository

```bash
git clone https://github.com/Bhavnish15/LinkScale.git

cd LinkScale
```

---

# 🐳 Start Infrastructure

LinkScale uses Docker Compose to run infrastructure dependencies.

```bash
docker compose up -d
```

This starts the required infrastructure such as:

* MySQL
* Redis
* Elasticsearch
* Kafka

Verify running containers:

```bash
docker ps
```

---

# ▶️ Run the Services

Each microservice can be started independently.

For example:

```bash
cd user-service
./mvnw spring-boot:run
```

Then start the remaining services:

```text
api-gateway
user-service
post-service
feed-service
search-service
notification-service
```

---

# 🔌 Service Communication

### Synchronous

Used where an immediate response is required:

```text
Client
   │
   ▼
API Gateway
   │
   ▼
Service
   │
   ▼
Response
```

### Asynchronous

Used for event-driven workflows:

```text
Producer
   │
   ▼
 Kafka
   │
   ├──► Consumer A
   ├──► Consumer B
   └──► Consumer C
```

This hybrid communication model provides both **low-latency request/response interactions** and **loosely coupled asynchronous processing**.

---

# 📊 Architecture at a Glance

```text
                           ┌───────────────┐
                           │    CLIENT     │
                           └───────┬───────┘
                                   │
                                   ▼
                       ┌──────────────────────┐
                       │     API GATEWAY      │
                       │                      │
                       │ JWT • Routing • Auth │
                       └──────────┬───────────┘
                                  │
          ┌───────────────────────┼────────────────────────┐
          │                       │                        │
          ▼                       ▼                        ▼
   ┌────────────┐          ┌────────────┐          ┌────────────┐
   │    USER    │          │    POST    │          │    FEED    │
   │  SERVICE   │          │  SERVICE   │          │  SERVICE   │
   └─────┬──────┘          └─────┬──────┘          └─────┬──────┘
         │                       │                       │
         ▼                       ▼                       ▼
      MySQL                   MySQL                    Redis
         │                       │
         │                       │
         └──────────┬────────────┘
                    │
                    │ Events
                    ▼
              ┌─────────────┐
              │    KAFKA    │
              │    KRaft    │
              └──────┬──────┘
                     │
              ┌──────┴───────┐
              │              │
              ▼              ▼
      ┌─────────────┐ ┌──────────────┐
      │   SEARCH    │ │ NOTIFICATION │
      │   SERVICE   │ │   SERVICE    │
      └──────┬──────┘ └──────────────┘
             │
             ▼
      ┌───────────────┐
      │ ELASTICSEARCH │
      └───────────────┘
```

---

# 🧪 Testing Strategy

The architecture is designed to support testing at multiple levels:

```text
                 Testing Pyramid

                     /\
                    /  \
                   / E2E\
                  /──────\
                 /Integration\
                /──────────────\
               /     Unit       \
              /──────────────────\
```

### Unit Testing

Test individual components in isolation:

* Services
* Business logic
* Validators
* Utility classes

### Integration Testing

Validate integration between:

* Spring Boot + MySQL
* Spring Boot + Redis
* Spring Boot + Kafka
* Spring Boot + Elasticsearch

### End-to-End Testing

Validate complete flows:

```text
Authentication
     ↓
Gateway
     ↓
User/Post Service
     ↓
Kafka
     ↓
Search / Notification
```

---

# 📈 Scalability

One of the major benefits of the architecture is that individual services can scale independently.

For example, if feed traffic becomes significantly higher:

```text
                    ┌───────────────┐
                    │ API Gateway   │
                    └───────┬───────┘
                            │
                  ┌─────────┴─────────┐
                  │                   │
                  ▼                   ▼
          ┌──────────────┐    ┌──────────────┐
          │ Feed Service │    │ Feed Service │
          │   Instance 1 │    │   Instance 2 │
          └──────┬───────┘    └──────┬───────┘
                 │                   │
                 └─────────┬─────────┘
                           ▼
                         Redis
```

The Feed Service can scale independently without scaling unrelated services.

---

# 🔮 Future Improvements

The architecture provides a foundation for further production-grade improvements.

Potential next steps:

* [ ] Service discovery with Eureka
* [ ] Distributed configuration with Spring Cloud Config
* [ ] Resilience4j circuit breakers
* [ ] Retry & timeout policies
* [ ] Distributed tracing with OpenTelemetry
* [ ] Prometheus metrics
* [ ] Grafana dashboards
* [ ] Centralized logging
* [ ] ELK/EFK logging stack
* [ ] Kubernetes deployment
* [ ] CI/CD with GitHub Actions
* [ ] Automated integration testing
* [ ] Testcontainers
* [ ] Database migration with Flyway
* [ ] API documentation with OpenAPI / Swagger
* [ ] Distributed rate limiting
* [ ] Dead-letter Kafka topics
* [ ] Kafka retry strategies
* [ ] Outbox Pattern
* [ ] Idempotent event processing
* [ ] Horizontal service scaling

---

# 💡 What I Learned

Building LinkScale provided hands-on experience with several concepts that are difficult to understand through theory alone.

### Backend Engineering

* Designing REST APIs
* Layered architecture
* DTOs and validation
* Exception handling
* Database interaction
* Transaction management

### Spring Ecosystem

* Spring Boot
* Spring Data JPA
* Spring Cloud Gateway
* Spring WebFlux
* Spring Security concepts
* Spring Data Elasticsearch

### Distributed Systems

* Microservice boundaries
* Service-to-service communication
* Event-driven architecture
* Message brokers
* Asynchronous processing
* Eventual consistency

### Infrastructure

* Docker
* Docker Compose
* Kafka
* Redis
* Elasticsearch
* MySQL

### System Design

* Caching strategies
* Search architecture
* Gateway patterns
* Scalability
* Decoupling
* Polyglot persistence

---

# 📚 Project Philosophy

LinkScale is intentionally built around one principle:

> **Don't just make the application work — understand what happens when the system grows.**

The project focuses on answering questions such as:

* What happens when one service goes down?
* How can services communicate without becoming tightly coupled?
* Where should authentication happen?
* When should communication be synchronous?
* When should events be asynchronous?
* How can expensive queries be cached?
* How should full-text search be implemented?
* How can individual services scale independently?
* How can data remain consistent across distributed services?

These questions drive the architecture of LinkScale.

---

# 👨‍💻 Author

### Bhavnish Bhardwaj

Java Backend Developer | Spring Boot | Microservices | Distributed Systems

🔗 GitHub:
**https://github.com/Bhavnish15**

---

# ⭐ Support

If you found this project useful or interesting, consider giving it a ⭐ on GitHub.

It helps support the project and motivates further development.

---

<p align="center">
  <strong>Built with Java ☕ • Spring Boot 🍃 • Kafka 📨 • Redis ⚡ • Elasticsearch 🔎 • MySQL 🗄️ • Docker 🐳</strong>
</p>

<p align="center">
  <i>Learning distributed systems by building them.</i>
</p>
