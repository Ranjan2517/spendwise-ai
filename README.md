# SpendWise AI

An AI-powered expense tracking and budgeting platform built as a Java/Spring Boot
microservices system — event-driven architecture, resilience patterns, CQRS, and
LLM integration.

**Status:** Day 1 — project scaffolding.

## Architecture

Client apps -> API Gateway -> Core services (User, Account, Transaction, Budget)
-> Kafka event bus -> AI & support services (Categorization, Notification, Insights)

Platform infra: Config Server, Eureka, Prometheus/Grafana, Zipkin

## Modules

| Module | Purpose | Status |
|---|---|---|
| config-server | Centralized Spring Cloud Config | Not started |
| discovery-server | Eureka service registry | Not started |
| api-gateway | Spring Cloud Gateway, JWT validation | Not started |
| user-service | Auth, user profiles, JWT issuance | Not started |
| account-service | Bank accounts, balances | Not started |
| transaction-service | Transaction ingestion, outbox events | Not started |
| budget-service | Budget rules, threshold tracking | Not started |
| categorization-service | Rule-based + LLM categorization | Not started |
| notification-service | Alerts on budget breaches | Not started |
| insights-service | CQRS read models, AI insights | Not started |

## Tech stack

Java 17, Spring Boot 3.3, Spring Cloud 2023.0.3, PostgreSQL, Apache Kafka, Redis,
Resilience4j, Keycloak/OAuth2, Prometheus + Grafana, Zipkin, Docker/Kubernetes.

## Running locally

1. `docker-compose up -d` — starts Postgres + pgAdmin
2. `mvn clean install` — builds all modules
3. `cd <module> && mvn spring-boot:run` — run an individual service