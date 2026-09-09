# LinkScale

A distributed microservices backend inspired by LinkedIn's core functionality — user profiles, posts/feed, full-text search, and event-driven notifications — built to demonstrate production-grade backend architecture, service isolation, and asynchronous communication.

## Overview

LinkScale breaks a typical monolithic social platform into independently deployable services, each owning its own data and responsibility. Services communicate synchronously via the API Gateway and asynchronously via Kafka events, with dedicated infrastructure for caching, search, and messaging.

## Architecture

                          ┌─────────────────┐
                          │   API Gateway    │
                          │  (Spring Cloud   │
                          │   Gateway WebFlux)│
                          │   JWT Auth Filter │
                          └────────┬─────────┘
                                   │
    ┌───────────────┬─────────────┼─────────────┬───────────────┐
    │               │             │              │               │
    ┌────▼────┐ ┌─────▼─────┐ ┌────▼─────┐ ┌─────▼──────┐ ┌────▼─────────┐
│ User │ │ Post │ │ Feed │ │ Search │ │ Notification │
│ Service │ │ Service │ │ Service │ │ Service │ │ Service │
└────┬────┘ └─────┬──────┘ └────┬─────┘ └─────┬──────┘ └──────┬───────┘
│ │ │ │ │
│ │ │ │ │
┌────▼───────────────▼──────┐ ┌────▼────┐ ┌──────▼──────┐ │
│ MySQL │ │ Redis │ │Elasticsearch│ │
│ (User / Post / Job data) │ │ (Feed │ │ (Full-text │ │
│ │ │ cache) │ │ search) │ │
└─────────────────────────────┘ └─────────┘ └─────────────┘ │
│
┌───────────────────────────────────────────┘
│
┌─────▼──────┐
│ Kafka │
│ (KRaft) │
│ event bus │
└────────────┘



## Services

| Service | Responsibility |
|---|---|
| **api-gateway** | Single entry point for all client requests. Validates JWTs via a custom `AbstractGatewayFilterFactory`-based filter, strips the token, and forwards verified identity (`X-User-Id`, `X-User-Email`) to downstream services. Excludes `/api/v1/auth/**` from auth checks. |
| **user-service** | User registration, authentication, and profile management. Source of truth for user data (MySQL). Publishes user lifecycle events (`user.created`, `user.updated`) to Kafka. |
| **post-service** | Post creation and management. Publishes `post.created` events for downstream consumers (search indexing, feed updates). |
| **feed-service** | Aggregates and serves personalized feeds, backed by Redis caching to avoid repeated expensive reads. |
| **search-service** | Full-text search across users (name, headline, skills, location) and posts (content), powered by Elasticsearch via Spring Data Elasticsearch repositories with `multi_match`/`match` queries and fuzzy matching. Kept in sync via Kafka consumers listening to `user.created`, `user.updated`, and `post.created` events. |
| **notification-service** | Event-driven notifications triggered by Kafka events from other services. |

## Tech Stack

- **Language / Framework:** Java 21, Spring Boot, Spring Cloud Gateway (WebFlux)
- **Security:** JWT-based authentication (`io.jsonwebtoken` / JJWT), centralized at the gateway
- **Databases:** MySQL (relational data — users, posts, jobs), Elasticsearch (search index)
- **Caching:** Redis (feed caching, rate limiting)
- **Messaging:** Apache Kafka (KRaft mode, no ZooKeeper) for event-driven, decoupled service communication
- **Containerization:** Docker Compose for local infra orchestration

## Key Design Decisions

- **Event-driven sync over direct calls** — services stay decoupled by reacting to Kafka events (e.g., search-service indexes new users/posts by consuming events rather than being called directly).
- **Auth centralized at the gateway** — JWT validation happens once, at the edge; downstream services trust gateway-injected headers instead of re-validating tokens.
- **Polyglot persistence** — each service uses the storage best suited to its access pattern (MySQL for relational data, Elasticsearch for search, Redis for hot-path caching).

## Running Locally

```bash
# Start infrastructure (MySQL, Redis, Elasticsearch, Kafka)
docker-compose up -d

# Run each service (from its own directory)
./mvnw spring-boot:run
```

## Repository Structure
