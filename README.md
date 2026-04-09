# demo-secured-api

A demonstration project showcasing a secured microservices architecture using **Spring Boot**, **Spring Cloud Gateway**, and **Keycloak**. The entire stack runs locally with a single `docker-compose up` command.

---

## What This Project Demonstrates

- **API security** using JWT Bearer tokens issued by Keycloak
- **Gateway pattern** — a single entry point that validates tokens and routes requests to internal services
- **Custom Keycloak SPI provider** — a custom password hashing algorithm plugged into Keycloak's authentication flow
- **Repeatable environment setup** — realm, client, roles, and users are imported automatically on startup; no manual Keycloak configuration required
- **Docker Compose orchestration** — all services run in an isolated Docker network with environment-specific configuration via Spring profiles

---

## Architecture

```
                        ┌─────────────────────────────────────────┐
                        │            Docker Network               │
                        │                                         │
 Client (Postman) ────► │  Gateway :8082 ────► API Service :8081  │
        │               │       │                                 │
        │               │       │ JWT validation                  │
        │               │       ▼                                 │
        └──── token ──► │  Keycloak :8080                         │
              request   │  (+ custom SPI provider)                │
                        └─────────────────────────────────────────┘
```

### Request flow

1. The client requests a JWT access token directly from Keycloak using username and password
2. The client sends a request to the **Gateway** with the token in the `Authorization: Bearer` header
3. The Gateway validates the token by fetching Keycloak's public signing keys (`jwk-set-uri`)
4. If valid, the Gateway forwards the request to the **API Service**, passing the token along in the header
5. The API Service performs its own JWT validation and role check before processing the request
6. The response is returned to the client through the Gateway

This double-validation pattern (Gateway + API Service) ensures that even if the Gateway were bypassed, the API Service would still reject unauthenticated requests.

---

## Project Structure

```
demo-secured-api/
├── api-service/              # Spring Boot REST API (Tasks CRUD)
├── gateway-service/          # Spring Cloud Gateway (routing + JWT validation)
├── keycloak-provider/        # Custom Keycloak SPI (SHA-256 password hash provider)
├── keycloak-import/          # Realm export JSON — imported automatically on startup
├── keycloak-providers/       # Built SPI jar — mounted into Keycloak container
└── docker-compose.yml        # Orchestrates all three services + Keycloak
```

---

## Services

### API Service (port 8081)
A Spring Boot REST API managing `Task` entities stored in an H2 in-memory database. Exposes standard CRUD endpoints under `/api/tasks`.

Secured as an **OAuth2 Resource Server** — every request must carry a valid JWT with the `USER` role. The API validates tokens independently of the Gateway using Keycloak's public signing keys.

### Gateway Service (port 8082)
A Spring Cloud Gateway acting as the single entry point for all client requests. Validates the JWT on every incoming request and forwards valid requests to the API Service, relaying the `Authorization` header downstream.

Internal services are hidden from the client — the client only ever talks to the Gateway.

### Keycloak (port 8080)
The identity and access management server responsible for issuing and signing JWT access tokens. Configured with:
- A `demo` realm
- A `gateway-client` OAuth2 client
- A `USER` realm role
- A `testuser` account

On first startup, Keycloak imports `keycloak-import/demo-realm.json` automatically, so the environment is fully configured without any manual steps.

### Keycloak SPI Provider
A custom `PasswordHashProvider` implementation that uses **SHA-256 with a random salt** for password hashing. It is packaged as a jar and mounted into Keycloak's providers directory at runtime.

This demonstrates the same pattern used to integrate Keycloak with legacy systems that use non-standard password hashing — Keycloak delegates the hash and verification logic to the custom provider, allowing existing user credentials to be authenticated without migrating or resetting passwords.

The provider is registered via the Java ServiceLoader mechanism (`META-INF/services/org.keycloak.credential.hash.PasswordHashProviderFactory`), which is how Keycloak discovers and loads custom extensions.

---

## Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- [Git](https://git-scm.com/)

That's it. You do not need Java, Maven, or any other tooling installed locally to run the project.

---

## Running the Project

**1. Clone the repository**
```bash
git clone https://github.com/VerrasAlex/demo-secured-api.git
cd demo-secured-api
```

**2. Start the stack**
```bash
docker-compose up
```

Wait approximately 30–40 seconds for Keycloak to finish initialising. You will see this line in the logs when it is ready:
```
keycloak  | Keycloak 25.0.x ... started
```

The API Service and Gateway Service may restart once or twice while waiting for Keycloak — this is expected and handled automatically.

**3. Verify everything is running**

Open [http://localhost:8080](http://localhost:8080) in your browser and log in to the Keycloak admin console with:
- Username: `admin`
- Password: `admin`

You should see the `demo` realm already configured with the client, role, and user.

> **Note:** When creating users in Keycloak, make sure to fill in the email, first name, and last name fields. Keycloak will block login for users with incomplete profiles even if credentials are correct.

---

## Testing the API

Use [Postman](https://www.postman.com/) or any HTTP client.

### Step 1 — Confirm unauthenticated requests are rejected

```
GET http://localhost:8082/api/tasks
```
Expected: `401 Unauthorized`

### Step 2 — Obtain a token from Keycloak

```
POST http://localhost:8080/realms/demo/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

grant_type=password
client_id=gateway-client
client_secret=WrwPcfpSC3shSVkraY4tVTGOv4E3VzBk
username=testuser
password=password
```

Copy the `access_token` value from the response.

### Step 3 — Call the API through the Gateway

```
GET http://localhost:8082/api/tasks
Authorization: Bearer <paste your access_token here>
```

Expected: `200 OK` with a JSON array of tasks.

### Step 4 — Create a task

```
POST http://localhost:8082/api/tasks
Authorization: Bearer <paste your access_token here>
Content-Type: application/json

{
  "title": "My first task",
  "description": "Testing the secured API",
  "completed": false
}
```

> **Note:** Access tokens expire after 5 minutes. If you receive a `401` after previously getting a `200`, request a new token and retry.

---

## Technology Stack

| Technology | Purpose |
|---|---|
| Java 21 | Primary language |
| Spring Boot 3.x | API Service framework |
| Spring Cloud Gateway | Request routing and JWT relay |
| Spring Security (OAuth2 Resource Server) | JWT validation |
| Keycloak 25 | Identity and access management |
| Keycloak SPI | Custom password hash provider |
| Docker / Docker Compose | Containerisation and orchestration |
| H2 | In-memory database for the API |
| Maven | Build tool |
