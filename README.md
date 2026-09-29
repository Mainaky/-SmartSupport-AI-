# 🤖 SmartSupport AI — Intelligent Customer Support Ticket System

> A demo-ready, AI-assisted support ticket management system built with
> **Java Spring Boot**, **Apache Kafka**, **Python (Flask AI)**, and **MySQL**.
> Purpose-built to align with the Kapture CX SDE Internship tech stack.

---

## 🏗 Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         HTML Frontend                           │
│                    (Dashboard + Ticket UI)                      │
└────────────────────────────┬────────────────────────────────────┘
                             │ REST API calls
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│              Spring Boot — Ticket Service (port 8080)           │
│  ┌──────────┐  ┌──────────┐  ┌──────────────┐  ┌───────────┐  │
│  │Controller│→ │ Service  │→ │AI Classifier │  │Kafka      │  │
│  │  (REST)  │  │(Business)│  │  Service     │  │Producer   │  │
│  └──────────┘  └────┬─────┘  └──────┬───────┘  └─────┬─────┘  │
│                     │               │                 │         │
│                     ▼               ▼                 ▼         │
│              ┌──────────┐   ┌───────────┐    ┌───────────────┐ │
│              │  MySQL   │   │  Python   │    │ Apache Kafka  │ │
│              │    DB    │   │  Flask AI │    │ ticket-events │ │
│              │(port3306)│   │(port 5000)│    │               │ │
│              └──────────┘   └───────────┘    └───────┬───────┘ │
└─────────────────────────────────────────────────────│───────────┘
                                                      │ Consume events
                             ┌────────────────────────▼────────────┐
                             │  Spring Boot — Notification Service  │
                             │              (port 8081)             │
                             │  Kafka Consumer → Email via MailHog  │
                             └─────────────────────────────────────┘
```

---

## ✨ Features

| Feature | Description |
|---|---|
| 🤖 AI Classification | Python AI auto-classifies tickets by category, priority, and urgency score |
| 📨 Kafka Streaming | All ticket state changes published as events to Kafka topics |
| 📧 Email Notifications | Notification service sends contextual emails via Kafka consumption |
| 🔺 Auto-Escalation | Scheduled job auto-escalates HIGH priority stale tickets |
| 📊 Analytics API | Real-time dashboard stats (by category, priority, status) |
| ⚡ Critical Alerts | Dedicated Kafka topic + email for CRITICAL tickets |
| 🏥 Health Checks | Spring Actuator + AI service `/health` endpoints |

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| Backend API | Java 17, Spring Boot 3.2, Spring Data JPA |
| Message Broker | Apache Kafka 7.5 (with Zookeeper) |
| AI Service | Python 3.11, Flask |
| Database | MySQL 8.0 |
| Email (dev) | MailHog (SMTP mock) |
| Build | Maven |
| DevOps | Docker + Docker Compose |

---

## 🚀 Quick Start

### Option 1: Docker Compose (Recommended — one command!)

```bash
# Clone / navigate to project
cd smartsupport

# Start everything
docker-compose up -d

# Services will be available at:
# API:          http://localhost:8080
# Frontend:     open frontend/index.html in browser
# Kafka UI:     http://localhost:9090
# MailHog:      http://localhost:8025 (view emails)
# AI Service:   http://localhost:5000/health
```

### Option 2: Manual Setup

#### Prerequisites
- Java 17+, Maven 3.8+
- Python 3.11+
- MySQL 8.0 running on port 3306
- Apache Kafka running on port 9092

#### Step 1 — MySQL
```sql
CREATE DATABASE smartsupport_db;
```

#### Step 2 — Python AI Classifier
```bash
cd ai-classifier
pip install flask
python app.py
# Starts on http://localhost:5000
```

#### Step 3 — Ticket Service
```bash
cd ticket-service
mvn spring-boot:run
# Starts on http://localhost:8080
```

#### Step 4 — Notification Service
```bash
cd notification-service
mvn spring-boot:run
# Starts on http://localhost:8081
```

#### Step 5 — Frontend
```bash
# Simply open in browser:
open frontend/index.html
# (No build step needed — pure HTML/CSS/JS)
```

---

## 📡 API Reference

### Create Ticket
```http
POST /api/v1/tickets
Content-Type: application/json

{
  "title": "Payment not processed",
  "description": "My payment failed and I was charged twice. This is urgent!",
  "customerEmail": "john@example.com",
  "customerName": "John Doe"
}
```

**Response:**
```json
{
  "id": 1,
  "title": "Payment not processed",
  "status": "OPEN",
  "priority": "HIGH",
  "category": "BILLING",
  "urgencyScore": 0.65,
  "aiSuggestion": "Verify the payment transaction...",
  "createdAt": "2025-05-01T10:30:00"
}
```

### Get All Tickets
```http
GET /api/v1/tickets
GET /api/v1/tickets?status=OPEN
GET /api/v1/tickets?customerEmail=john@example.com
```

### Get Single Ticket
```http
GET /api/v1/tickets/{id}
```

### Update Ticket
```http
PUT /api/v1/tickets/{id}
Content-Type: application/json

{
  "status": "RESOLVED",
  "assignedAgent": "Alice Smith"
}
```

### Delete Ticket
```http
DELETE /api/v1/tickets/{id}
```

### Analytics
```http
GET /api/v1/tickets/analytics
```

**Response:**
```json
{
  "totalTickets": 42,
  "openTickets": 15,
  "resolvedTickets": 20,
  "criticalTickets": 3,
  "avgResolutionTimeHours": 4.7,
  "ticketsByCategory": { "BILLING": 12, "TECHNICAL": 18, ... },
  "ticketsByPriority": { "CRITICAL": 3, "HIGH": 10, ... },
  "ticketsByStatus":   { "OPEN": 15, "RESOLVED": 20, ... }
}
```

---

## 🎯 Kafka Topics

| Topic | Partitions | Purpose |
|---|---|---|
| `ticket-events` | 3 | All ticket lifecycle events |
| `critical-ticket-alerts` | 1 | CRITICAL priority tickets only |

### Event Types
- `TICKET_CREATED` → sends confirmation email to customer
- `TICKET_RESOLVED` → sends resolution email
- `TICKET_ASSIGNED` → notifies customer of agent assignment
- `TICKET_ESCALATED` → alerts customer + support team

---

## 🤖 AI Classifier API

```http
POST http://localhost:5000/classify
Content-Type: application/json

{
  "title": "App crashing on login",
  "description": "The app crashes every time I try to log in. This is urgent!"
}
```

**Response:**
```json
{
  "category": "TECHNICAL",
  "priority": "HIGH",
  "urgencyScore": 0.75,
  "suggestion": "Collect error logs from the customer..."
}
```

---

## 📁 Project Structure

```
smartsupport/
├── ticket-service/                  # Spring Boot Main Service
│   ├── src/main/java/com/smartsupport/
│   │   ├── SmartSupportApplication.java
│   │   ├── controller/
│   │   │   └── TicketController.java    # REST endpoints
│   │   ├── service/
│   │   │   ├── TicketService.java       # Business logic
│   │   │   └── AIClassifierService.java # Python AI caller
│   │   ├── kafka/
│   │   │   ├── TicketEvent.java         # Event POJO
│   │   │   └── TicketEventProducer.java # Kafka publisher
│   │   ├── entity/
│   │   │   └── Ticket.java              # JPA Entity
│   │   ├── dto/
│   │   │   └── TicketDTO.java           # Request/Response DTOs
│   │   ├── repository/
│   │   │   └── TicketRepository.java    # JPA Repository
│   │   ├── config/
│   │   │   └── AppConfig.java           # Kafka + RestTemplate config
│   │   └── exception/
│   │       ├── TicketNotFoundException.java
│   │       └── GlobalExceptionHandler.java
│   ├── src/main/resources/
│   │   └── application.properties
│   └── pom.xml
│
├── notification-service/            # Kafka Consumer + Email Service
│   ├── src/main/java/com/notification/
│   │   ├── kafka/
│   │   │   ├── TicketEventConsumer.java  # Kafka listener
│   │   │   └── TicketEventMessage.java   # Message POJO
│   │   └── service/
│   │       └── EmailNotificationService.java
│   └── src/main/resources/
│       └── application.properties
│
├── ai-classifier/                   # Python AI Microservice
│   ├── app.py                       # Flask API + classifier logic
│   └── requirements.txt
│
├── frontend/
│   └── index.html                   # Full dashboard UI
│
└── docker-compose.yml               # Full stack orchestration
```

---

## 🧠 What the Classifier Actually Does

The Flask service uses a small, hand-written nearest-example text matcher for categories and keyword rules for urgency and priority. It does not load a trained scikit-learn model. This keeps the demo self-contained, but the tiny in-code examples are not suitable for real customer-support decisions.

## ⚠️ Demo Deployment Limits

This project is a learning/demo system. It has no user authentication or authorization, uses permissive cross-origin API access, and the Compose file has development defaults for database credentials. Do not expose it to the public internet or use it for real customer data without adding access controls, secret management, operational health checks, and a durable database-to-Kafka event strategy.

## 💡 Interview Talking Points

1. **Why Kafka?** It lets ticket creation publish an event without waiting for the email service to finish. Other consumer groups can independently read the same event stream.

2. **Why a separate Python service?** The classifier can be changed or scaled separately from the Java ticket API. In this demo it is a simple nearest-example and keyword classifier, not a production ML model.

3. **Producer reliability** — `acks=all` and producer idempotence help protect Kafka writes from certain retry-related duplicates. They do not make the database write and Kafka publish one atomic operation, and they do not guarantee end-to-end exactly-once email delivery.

4. **Partition key = ticketId** — events for one ticket are routed to the same partition, preserving their order within that partition.

5. **Auto-escalation** — a scheduled job runs hourly and escalates OPEN, HIGH-priority tickets older than 24 hours.

---

## 🏆 Alignment with Kapture CX JD

| JD Requirement | How This Project Covers It |
|---|---|
| Java Spring Boot | Main ticket service built entirely in Spring Boot 3 |
| Apache Kafka | Event streaming between ticket and notification services |
| Python | Flask-based AI classifier microservice |
| External System Integration | Notification service + Python AI = 2 integrations |
| Feature Enhancement / New Application | Full new CX-aligned application |
