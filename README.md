# POSHANCARE 🌱

## Anganwadi Nutrition & Child Health Management System

POSHANCARE is a web-based college project designed to help Anganwadi workers manage children's growth, nutrition, health records, and follow-ups in one place.

## Features

- **Worker registration:** Workers can register and follow the admin approval workflow.
- **Admin dashboard:** Manage worker approvals through protected admin functionality.
- **Child management:** Register children and maintain their unique Child IDs.
- **Growth tracking:** Record height and weight and view growth history.
- **Nutrition records:** Maintain nutrition-related information.
- **Follow-up management:** Create and track follow-up records.
- **Parent dashboard:** Access an existing child's information through the application's authentication workflow.
- **Authentication:** JWT-based security and an OTP verification workflow.

## Technology Stack

**Frontend**
- HTML
- CSS
- JavaScript

**Backend**
- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- Maven
- PostgreSQL
- JWT

## Project Structure

```text
PoshanCare/
├── frontend/
│   ├── index.html
│   ├── login.html
│   ├── worker.html
│   ├── parent.html
│   └── admin.html
├── poshancare-backend-starter/
│   └── poshancare-backend/
│       ├── pom.xml
│       └── src/
├── .gitignore
└── README.md
```

## Running the Project Locally

### 1. Prerequisites

Install:
- JDK 21
- PostgreSQL
- Maven, if not using the Maven Wrapper
- Git (optional)

### 2. Configure PostgreSQL

Create the database:

```sql
CREATE DATABASE poshancare_db;
```

### 3. Configure Environment Variables

Configure these variables in your local environment:

| Variable | Purpose |
|---|---|
| `DB_URL` | PostgreSQL JDBC URL |
| `DB_USERNAME` | PostgreSQL username |
| `DB_PASSWORD` | PostgreSQL password |
| `JWT_SECRET` | JWT signing secret |
| `ADMIN_MOBILE` | Initial admin mobile number, if using bootstrap |
| `ADMIN_PASSWORD` | Initial admin password, if using bootstrap |

Use your own local values. Never commit real credentials, OTPs, or access tokens.

### 4. Start the Backend

Open a terminal in `poshancare-backend-starter/poshancare-backend/` and run:

```bash
mvn spring-boot:run
```

The backend uses port `8080` by default.

### 5. Open the Frontend

Open `frontend/index.html` in your browser or use a local development server. Ensure the backend CORS configuration allows the frontend's actual origin.

## Authentication Notes

- Worker registration follows an admin approval workflow.
- OTP delivery currently uses a development workflow that may display the generated OTP in the Spring Boot console.
- A real OTP delivery provider and additional security controls are required before public deployment.

## Project Status

POSHANCARE is a college project under development. Core child, growth, nutrition, and follow-up APIs have been tested locally. Authentication and deployment configuration still require review before production use.

Do not use real children's personal or health information in an unsecured demo deployment.

## Future Improvements

- Production OTP delivery
- Deployment with managed hosting and a production database
- Further authorization and privacy testing
- Improved documentation and automated testing
