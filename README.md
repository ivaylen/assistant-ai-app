# Assistant AI App

Doctor-assistant demo application built with Spring Boot, Spring AI, Ollama, PostgreSQL/pgvector, and a separate Spring MCP search server.

The project is split into two Maven modules:

```text
assistant-ai-app
  assistant           Main doctor assistant web app
  search-mcp-server   Separate Spring MCP server that exposes web_search
```

## Functionality

The main assistant app provides:

- Local login, signup, and logout with Spring Security
- User accounts stored in PostgreSQL
- A simple React chat UI served by Spring Boot
- Chat memory using Spring AI JDBC chat memory
- Ollama-backed local chat model
- Medical safety guidance tools for common health questions
- Wellbeing/life-coaching guidance with safety boundaries
- Appointment request creation from chat
- User-scoped appointment list
- Admin appointment review and confirmation
- pgvector configuration for future RAG/document retrieval work
- Optional MCP profile for external web-search tools

The appointment workflow is human-in-the-loop:

1. A user asks to schedule an appointment.
2. The assistant creates an appointment request.
3. Normal users see only their own requests.
4. Admin users can see all requests.
5. Admin users can confirm requested appointments.

Admin access is configured locally through application properties or environment variables. Do not commit real admin credentials.

## AI And Safety

The assistant uses a cautious system prompt and local tools for safer behavior:

- It should not diagnose with certainty.
- It should not prescribe medication.
- It should recommend emergency help for red-flag symptoms.
- It hides raw tool-call JSON if the local model leaks it.
- It has a current-message safety guard for self-harm or violence phrases.
- It can recover useful guidance when the local model returns tool-call-shaped text.

This is still a demo. It is not a medical device and should not replace a licensed clinician.

## Main App

Path:

```text
assistant
```

Important local services:

```text
Spring app: http://localhost:8080
PostgreSQL: localhost:5433
Database: mydatabase
User: root
Password: secret
```

Start PostgreSQL:

```powershell
cd "C:\Users\Ivaylo Andreev\Desktop\Projects\assistant-ai-app\assistant"
docker compose up -d
```

Start the assistant without MCP:

```powershell
.\mvnw.cmd spring-boot:run
```

Open:

```text
http://localhost:8080
```

## MCP Search Server

Path:

```text
search-mcp-server
```

This is a separate Spring Boot MCP server. It exposes:

```text
web_search(query, trustedMedicalOnly)
```

It currently uses DuckDuckGo's free instant-answer endpoint, so results are limited. For production medical use, prefer trusted document RAG or a search API that can restrict sources to reliable medical domains.

Start the MCP server:

```powershell
cd "C:\Users\Ivaylo Andreev\Desktop\Projects\assistant-ai-app\search-mcp-server"
..\assistant\mvnw.cmd spring-boot:run
```

It runs on:

```text
http://localhost:8090
```

Start the assistant with MCP enabled:

```powershell
cd "C:\Users\Ivaylo Andreev\Desktop\Projects\assistant-ai-app\assistant"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=mcp"
```

Then ask with explicit search wording:

```text
Search the internet for trusted information about hair loss causes.
```

The assistant only receives the MCP search instructions when the `mcp` profile is active.

## IntelliJ Setup

Open the root folder:

```text
C:\Users\Ivaylo Andreev\Desktop\Projects\assistant-ai-app
```

The root `pom.xml` imports both Maven modules:

```text
assistant
search-mcp-server
```

In IntelliJ:

1. Open the root folder.
2. Right-click the root `pom.xml`.
3. Select **Add as Maven Project**.
4. Open the Maven tool window.
5. Click **Reload All Maven Projects**.

Run order for MCP testing:

1. Start `SearchMcpServerApplication`.
2. Start `AssistantApplication` with profile `mcp`.

## Useful Test Messages

General symptom:

```text
My foot hurts, what can help?
```

Heat headache:

```text
I have a headache from very hot weather, what should I do?
```

Appointment:

```text
Schedule an appointment. Patient name Ivaylo. Reason headache. Preferred time tomorrow afternoon. Contact 0888123456.
```

Search through MCP:

```text
Search the internet for trusted information about hair loss causes.
```

## Build

From the root folder:

```powershell
.\assistant\mvnw.cmd validate
```

From the assistant module:

```powershell
.\mvnw.cmd test
```

From the MCP server module:

```powershell
..\assistant\mvnw.cmd -q -DskipTests package
```
