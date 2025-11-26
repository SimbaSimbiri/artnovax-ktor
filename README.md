# ArtNovaX Backend 🎨
_API & Services for Distraction-Free Digital Art Therapy_

This is the **Ktor-based backend** for ArtNovaX — the service that powers guided art therapy sessions, personalization, telemetry, and user data for the mobile app.

Where the client creates a **sanctuary for creativity**, the backend quietly keeps everything **secure, consistent, and available**: sessions, recommendations, analytics, and configuration.

---

## 🎯 MVP Scope

At a high level, the ArtNovaX backend is responsible for:

1. **Auth & Accounts**
   - User registration & login
   - JWT-based authentication for the mobile client
   - Basic profile/mood preferences (where needed for recommendations)
   

2. **Therapy Modules & Content**
   - Serving **guided art therapy modules** as JSON (steps, timing, prompts, assets)
   - Tagging modules by goals: _grounding_, _stress relief_, _processing emotion_, etc.
   - Supporting content packs (e.g. “Ubuntu Flow”, “Grounding Through Color”)


3. **Sessions & Progress**
   - Recording session starts/completions, duration, and optional mood check-ins
   - Tracking streaks & total time for “My Journey” views in the app
   - Exposing summary stats for lightweight progress visuals


4. **Recommendations & Personalization**
   - Rule-based recommendations (e.g., gentler content when mood is low)
   - Simple “Why recommended” explanation strings
   - Room to plug in more advanced logic later (e.g., ML-driven recs)


5. **Telemetry & Analytics (Privacy-Respecting)**
   - Logging non-identifying events (e.g., `session_start`, `session_end`)
   - No raw video/audio storage for emotion detection — those stay on-device
   - Data retention rules to avoid hoarding sensitive information


---

## 📐 Non-Functional Goals

The backend is designed with a few key qualities in mind:

### Performance & Reliability

- Target p95 latency:
  - Reads: **< 200 ms**
  - Writes: **< 400 ms** (under expected load)
- Deployed behind a load balancer, with health checks and auto-restart on failure
- Centralized logging & metrics for debugging and observability

### Security & Privacy

- HTTPS-only in production
- JWT for auth, short-lived tokens, refresh strategy TBD
- Secrets (JWT keys, DB creds) managed via environment/secret manager (not in Git)
- No storage of raw camera/video/audio; only summarized emotion or mood signals if/when needed
- Data export/delete endpoints planned to support user privacy controls

### Data Retention

- Session & telemetry data retained for limited windows (e.g., 12–18 months) for insights and improvement
- Beyond retention window, older analytics are anonymized or purged

---

## 🧱 Tech Stack

**Language & Framework**

- Kotlin
- [Ktor](https://ktor.io) Server

**Core Libraries / Features**

- **Routing** & **Resources** for type-safe endpoints
- **Status Pages** for centralized error handling
- **Content Negotiation** + `kotlinx.serialization` (JSON)
- **Request Validation** for payloads
- **Koin** for dependency injection

**Infrastructure Targets**

- **AWS Elastic Beanstalk**
- **Aurora PostgreSQL**

**ML / AI Integration**

- Python for training emotion/recommendation models (out of band)
- On-device models (TFLite) preferred for affect detection to avoid streaming sensitive data
- Backend may provide:
  - configuration flags
  - model metadata
  - non-sensitive recommendation logic

---
