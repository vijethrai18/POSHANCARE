# POSHANCARE Backend

Spring Boot REST API for POSHANCARE, a web-based Anganwadi Nutrition & Child Health Management System.

## Technology Stack

- Java 21
- Spring Boot 3.5.x
- Maven
- Spring Web
- Spring Security
- Spring Data JPA
- PostgreSQL
- Jakarta Bean Validation
- JWT-based authentication

## Features

- Child registration with a permanent unique Child ID
- Child growth record management
- Nutrition record management
- Follow-up record management
- Worker self-registration
- OTP-based authentication workflow
- Admin login and role-protected admin endpoints
- Stateless security using JWT
- PostgreSQL database integration

## Project Structure

```text
src/
├── main/
│   ├── java/com/poshancare/
│   │   ├── config/
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── entity/
│   │   ├── repository/
│   │   ├── security/
│   │   └── service/
│   └── resources/
│       └── application.properties
└── test/
```

The exact package and file structure may evolve as development continues.

## Prerequisites

Install the following software:

- JDK 21
- Maven
- PostgreSQL
- Git (optional, for cloning the repository)

## Database Setup

Create a PostgreSQL database:

```sql
CREATE DATABASE poshancare_db;
```

Configure these environment variables before starting the backend:

| Variable | Purpose |
|---|---|
| `DB_URL` | PostgreSQL JDBC URL |
| `DB_USERNAME` | PostgreSQL username |
| `DB_PASSWORD` | PostgreSQL password |
| `JWT_SECRET` | JWT signing secret |

For local development, the default database URL is:

```text
jdbc:postgresql://localhost:5432/poshancare_db
```

Set your actual credentials in your local environment. Never commit database passwords, JWT secrets, OTPs, or access tokens to GitHub.

## Run the Backend

Open a terminal in this directory:

```bash
mvn spring-boot:run
```

The backend runs on port `8080` by default.

Ensure PostgreSQL is running and the required environment variables are configured.

## Authentication Endpoints

All authentication endpoints use the `/api/auth` base path.

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/worker-register` | Register a worker |
| POST | `/api/auth/request-otp` | Request an OTP |
| POST | `/api/auth/verify-otp` | Verify an OTP |
| POST | `/api/auth/admin-login` | Admin login |

Worker registration is subject to the application's approval workflow. Admin endpoints under `/api/admin/**` require the `ADMIN` role.

Other API endpoints require authentication unless explicitly permitted by the security configuration.

## Core API Endpoints

### Children

- `POST /api/children` — Register a child
- `GET /api/children` — List children
- `GET /api/children?name=Anu` — Search children by name
- `GET /api/children/{childId}` — Retrieve a child

### Growth Records

- `POST /api/children/{childId}/growth` — Create a growth record
- `GET /api/children/{childId}/growth` — Retrieve growth history

### Nutrition Records

- `POST /api/children/{childId}/nutrition` — Create a nutrition record
- `GET /api/children/{childId}/nutrition` — Retrieve nutrition records

### Follow-up Records

- `POST /api/children/{childId}/followups` — Create a follow-up
- `GET /api/children/{childId}/followups` — Retrieve follow-ups

Related records are associated with the same permanent `childId`.

The API may evolve as additional endpoints and access-control rules are implemented.

## Development Notes

- OTP delivery currently uses the development workflow, which may display generated OTPs in the Spring Boot console.
- Production use requires a real OTP delivery provider and appropriate verification, rate limiting, and abuse prevention.
- JWT secrets and database credentials must be supplied securely through environment variables or a secrets manager.
- Review CORS settings and configure them for the actual frontend origin before deployment.
- Review authorization to ensure workers and parents can access only the records they're permitted to view.
- For production, use controlled database migrations such as Flyway or Liquibase instead of relying solely on automatic schema updates.
- Disable verbose SQL logging in production where appropriate.

## Security and Privacy

This is a college project under development. Security configuration is present, including JWT authentication and role-based protection for admin endpoints, but the complete application has not been independently audited for production readiness.

Do not use real children's personal or health data in an unsecured demo deployment.

Before public deployment, review authentication, authorization, OTP delivery, CORS, database permissions, logging, audit trails, and data protection.

## Testing

Run the backend tests with:

```bash
mvn test
'''