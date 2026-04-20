# Starto V2

Real-time startup ecosystem platform. Founders, investors, and mentors post Signals, connect, and collaborate.

**Stack:** Spring Boot 3.3 · Next.js 14 · Android (Jetpack Compose) · PostgreSQL+PostGIS · Redis · Firebase Auth

---

## Prerequisites

- Java 21 (JDK), Maven 3.9+
- Node.js 20+, npm 9+
- PostgreSQL 15+ with PostGIS extension
- Redis 7+
- Firebase project (Auth + Firestore enabled)
- Android Studio Hedgehog+ (for Android)

---

## Environment Variables

Copy `.env.example` to `.env` and fill in all values:

```bash
cp .env.example .env
```

Key variables required before first run:

| Variable | Description |
|---|---|
| `DB_URL` | PostgreSQL JDBC URL |
| `FIREBASE_SERVICE_ACCOUNT_B64` | Base64-encoded Firebase service account JSON |
| `GEMINI_API_KEY` | Google Gemini AI key |
| `RAZORPAY_KEY_ID` / `RAZORPAY_KEY_SECRET` | Razorpay payment keys |
| `NEXT_PUBLIC_API_BASE_URL` | Backend URL for the frontend |

---

## Run Backend

```bash
cd starto-api
# First run: initialise schema
psql -U postgres -d starto -f ../schema.sql
# Start
../run-backend.ps1          # Windows
mvn spring-boot:run         # Mac/Linux (set env vars first)
# Runs on http://localhost:8080
```

---

## Run Frontend

```bash
cd starto-web
cp ../.env.example .env.local   # fill in NEXT_PUBLIC_* values
npm install
npm run dev
# Runs on http://localhost:3000
```

---

## Run with Docker Compose (recommended)

```bash
cp .env.example .env   # fill in all values
docker compose up -d --build
```

---

## Android

1. Open `starto-android/` in Android Studio.
2. Add `API_BASE_URL=http://10.0.2.2:8080` to `starto-android/local.properties`.
3. Add `google-services.json` to `starto-android/app/` (from Firebase console).
4. Run on emulator or device.
