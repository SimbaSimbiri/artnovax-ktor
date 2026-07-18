# ArtNovaX Backend 🎨
_API & Services for Distraction-Free Digital Art Therapy_

This is the **Ktor-based backend** for ArtNovaX — the service that powers guided art therapy sessions, personalization, telemetry, and user data for the mobile app.

Where the client creates a **sanctuary for creativity**, the backend quietly keeps everything **secure, consistent, and available**: sessions, recommendations, analytics, and configuration.

---

## 🎯 MVP Scope

At a high level, the ArtNovaX backend is responsible for:

1. **Auth & Accounts**
    - User registration and login
    - JWT-based authentication for the mobile client
    - Basic profile and mood preferences where needed for recommendations

2. **Therapy Modules & Content**
    - Serving guided art therapy modules as JSON, including steps, timing, prompts, and assets
    - Tagging modules by goals such as _grounding_, _stress relief_, and _processing emotion_
    - Supporting content packs such as “Ubuntu Flow” and “Grounding Through Color”

3. **Sessions & Progress**
    - Recording session starts, completions, duration, and optional mood check-ins
    - Tracking streaks and total time for “My Journey” views
    - Exposing summary statistics for lightweight progress visuals

4. **Recommendations & Personalization**
    - Rule-based recommendations, such as gentler content when mood is low
    - Simple “Why recommended” explanations
    - Support for more advanced ML-driven recommendations later

5. **Telemetry & Analytics**
    - Logging non-identifying events such as `session_start` and `session_end`
    - Keeping raw video and audio used for emotion detection on-device
    - Applying retention rules to avoid storing unnecessary sensitive information

---

## 📐 Non-Functional Goals

### Performance & Reliability

- Target p95 latency:
    - Reads: **< 200 ms**
    - Writes: **< 400 ms** under expected load
- Health checks and automatic restart on failure
- Centralized logging and metrics for debugging and observability

### Security & Privacy

- HTTPS-only in production
- JWT authentication with short-lived tokens
- Secrets managed through environment variables or a secret manager
- No storage of raw camera, video, or audio data
- Planned data export and deletion endpoints

### Data Retention

- Session and telemetry data retained only for defined periods
- Older analytics anonymized or deleted after the retention window

---

## 🧭 Architecture

The backend follows a layered request flow:

```text
HTTP Request
    ↓
Ktor Route
    ↓
Request Validation and DTO Mapping
    ↓
Application Use Case
    ↓
Domain Policy
    ↓
Repository Interface
    ↓
Repository Implementation
    ↓
Exposed Transaction
    ↓
PostgreSQL
```

Responses and errors travel back through the same layers in reverse.

Each layer has a focused responsibility:

- **Presentation:** HTTP routes, DTOs, parameter parsing, and responses
- **Application:** use cases representing operations such as creating or updating a user
- **Domain:** business models and validation policies
- **Repository:** typed persistence contracts
- **Data:** Exposed queries, transactions, database entities, and mappings

Koin connects the layers through dependency injection. Routes receive application use cases rather than accessing repositories directly.

### User Aggregate

`User` is an aggregate root that owns the user profile and its social links.

```text
User
└── User Social Links
```

A typical user creation request follows this direction:

```text
POST /users
    ↓
UserUpsertDto
    ↓
User Domain Model
    ↓
CreateUserUseCase
    ↓
UserPolicy
    ↓
UserRepository.createUser
    ↓
UserRepoImpl Transaction
    ├── Validate social-platform references
    ├── Insert the user
    └── Insert social links
    ↓
HTTP 201 Created
```

The application uses typed values such as `UserId` and `UserType` instead of passing raw UUID strings or integer codes into the repository layer.

---

## 🧱 Tech Stack

### Language & Framework

- Kotlin
- [Ktor](https://ktor.io) Server

### Core Libraries and Features

- **Routing** and **Resources** for type-safe endpoints
- **Status Pages** for centralized error handling
- **Content Negotiation** with `kotlinx.serialization`
- **Request Validation** for incoming payloads
- **Koin** for dependency injection
- **Exposed** for database access

### Infrastructure Targets

- **AWS Elastic Beanstalk**
- **Aurora PostgreSQL**

### ML / AI Integration

- Python for training emotion and recommendation models
- On-device models such as TFLite preferred for affect detection
- Backend support for:
    - configuration flags
    - model metadata
    - non-sensitive recommendation logic

---