# Search MCP Server

Separate Spring Boot MCP server for the doctor assistant.

It exposes one MCP tool:

```text
web_search(query, trustedMedicalOnly)
```

The first version uses DuckDuckGo's free instant-answer endpoint. It is useful for learning MCP and basic demos, but it is not a full medical search engine.

## Run

From `assistant-ai-app/search-mcp-server`:

```powershell
..\assistant\mvnw.cmd spring-boot:run
```

It runs on:

```text
http://localhost:8090
```

## Run With The Assistant

Open one PowerShell window for the MCP server:

```powershell
cd "C:/Users/Ivaylo Andreev/Desktop/Projects/assistant-ai-app/search-mcp-server"
..\assistant\mvnw.cmd spring-boot:run
```

Open another PowerShell window for the assistant with the MCP profile:

```powershell
cd "C:/Users/Ivaylo Andreev/Desktop/Projects/assistant-ai-app/assistant"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=mcp"
```

The assistant connects to:

```properties
spring.ai.mcp.client.streamable-http.connections.medical-search.url=http://localhost:8090
spring.ai.mcp.client.streamable-http.connections.medical-search.endpoint=/mcp
```

Keep appointment and user logic inside the main assistant app. Use this server only for external search-style tools.
