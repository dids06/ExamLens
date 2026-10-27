# ExamLens

**ExamLens** is an AI-powered adaptive exam preparation and question-paper intelligence platform. It helps students organize syllabus topics, build a study plan around an exam date, track progress, and—when AI features are available—analyse study materials and generate practice questions.

> **Project status:** College project / MVP in development. Features may be incomplete until the team integrates them.

## Tech Stack

- **Language:** Java
- **Frontend:** JSP, HTML, CSS, JavaScript
- **Backend:** Java Servlets and service classes
- **Database access:** JDBC
- **Database:** MySQL
- **Server:** Apache Tomcat
- **Build tool:** Maven
- **Version control:** Git and GitHub
- **AI integration:** Gemini API through a backend `AIService` abstraction

The core planner should still provide a usable baseline plan when AI is unavailable.

## Architecture

The agreed request flow is:

`JSP → Servlet/Controller → Service → DAO → JDBC → MySQL`

- **JSP:** Displays pages and user-facing data. Keep SQL and business logic out of JSP files.
- **Servlet:** Receives HTTP requests, performs basic request validation, calls a service, and selects the response or JSP.
- **Service:** Implements business rules and coordinates operations.
- **DAO (Data Access Object):** Encapsulates database queries and persistence operations.
- **JDBC:** Java's API for communicating with the database.
- **MySQL:** Stores application data.
- **AIService:** A backend interface for AI operations. Do not call Gemini directly from a JSP or browser JavaScript.

## Team Responsibilities

The team is using one repository and one shared application.

1. **Frontend:** JSP pages, HTML/CSS/JavaScript, UI behavior, loading/error states, and integration with the agreed routes and request/response formats.
2. **Backend:** Maven/Tomcat foundation, Servlets, services, DAOs/JDBC, MySQL schema, authentication/session handling, authorization, validation, and baseline study-planning logic.
3. **AI and intelligent features:** Gemini integration behind `AIService`, prompts and structured-output validation, document/syllabus extraction, PYQ analysis, question/model-paper generation, and answer evaluation.

Coordinate before changing shared models, database schema, route names, or method signatures. Do not create separate competing versions of the same classes or schema.

## Prerequisites

Install and configure:

- A compatible JDK (the team’s proposed baseline is Java 17).
- Apache Maven.
- MySQL Server.
- Apache Tomcat 10.1, if using the agreed Jakarta Servlet API setup.
- Git.
- An IDE/editor that supports Java and Maven.

**Compatibility rule:** If the project uses Tomcat 10.1, use the Jakarta Servlet namespace (`jakarta.servlet.*`). Do not mix it with the older `javax.servlet.*` namespace. Follow the `pom.xml` and shared integration contract if they specify a different agreed setup.

## Getting Started

### 1. Clone the repository

```bash
git clone <REPOSITORY_URL>
cd ExamLens
```

Replace `<REPOSITORY_URL>` with the team’s GitHub repository URL.

### 2. Create the database

1. Start MySQL.
2. Create a development database using the name agreed in the shared contract/configuration.
3. Run the project’s schema script, when available:

```text
database/schema.sql
```

4. Use `database/seed.sql` only if the team has provided suitable development/test data.

Do not commit real passwords, API keys, or personal data.

### 3. Configure local settings

Set the database connection settings using the configuration mechanism implemented by the backend (for example, environment variables or a local properties file). Use `.env.example` as a documentation template if present.

Keep actual credentials and API keys out of Git. Never put secrets in JSP files or browser-side JavaScript.

### 4. Build the project

From the project root:

```bash
mvn clean package
```

Resolve compilation/test errors before attempting deployment.

### 5. Run on Tomcat

Deploy the generated WAR from `target/` to the configured Tomcat server, or use the IDE’s Tomcat integration if the project is configured for it. Start Tomcat and open the local application URL shown by the server configuration.

The exact WAR name, context path, database configuration keys, and run command depend on the repository’s actual `pom.xml` and configuration files.

## Development Workflow

1. Pull the latest shared changes before starting work.
2. Confirm the relevant model, route, database and interface contracts.
3. Implement one small feature at a time.
4. Build and test locally.
5. Commit working changes with a clear message.
6. Push the branch and open a pull request, or follow the team’s agreed merge workflow.
7. Test the integrated application—not just each person's isolated code.

Suggested commit messages:

```text
feat: add exam creation endpoint
fix: validate exam date before saving
test: add study plan service tests
docs: update local setup instructions
```

## MVP Priorities

Prioritize an end-to-end working path before optional advanced features:

1. Application starts and connects to MySQL.
2. Registration/login and protected student sessions work.
3. A student can create and view an exam.
4. A baseline study plan can be generated without AI.
5. Tasks can be marked complete and progress is saved.
6. Frontend, backend and AI modules integrate using the shared contract.
7. Optional document analysis, PYQ analysis, question generation and answer evaluation are added and tested.

## Security and Reliability

- Use prepared statements for SQL parameters.
- Hash passwords with a suitable password-hashing algorithm; never store plain-text passwords.
- Validate inputs on the server, even if the frontend also validates them.
- Enforce authorization on every protected operation; do not rely only on hiding UI elements.
- Keep secrets outside source control.
- Return useful error messages without exposing stack traces, credentials, or sensitive internal details.
- Handle AI/API failure gracefully. Core exam and baseline planning flows should not fail merely because the AI provider is unavailable.
- Do not claim an AI-generated question or study priority is guaranteed to appear in an exam.

## Shared Contract

The **Shared Requirements & Integration Contract** is the source of truth for detailed functional requirements, package structure, models/interfaces, database design, routes, business rules, team boundaries and acceptance checks. If this README conflicts with that contract or an explicitly agreed team decision, stop and resolve the discrepancy with the team rather than independently changing the architecture.

## Current Limitations

This README describes the intended setup. It does not guarantee that every feature is implemented yet. Update the setup instructions, routes, database details and feature list as the actual codebase becomes available and is verified.
