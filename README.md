# Search Engine MCP

Spring Boot MCP server for exposing search service topology sources to AI clients.

The server calls the existing backend endpoint:

```http
GET /query/topology/sources
```

and exposes the result through MCP tools.

Topology tool:

- `listSearchServiceSources` - lists data sources available in the search service topology.

Saved-query template tools:

- `listSavedQueryTemplates` - lists personal or shared saved query templates;
- `getQueryTemplateParameters` - returns the typed execution schema for one template;
- `executeQueryTemplate` - fills empty unlocked filters and starts asynchronous execution;
- `getQueryResult` - checks execution readiness and returns a bounded page of table rows.

Templates are backend data, not dynamically registered MCP tools. A template created
in the web application is therefore available through `listSavedQueryTemplates` without
restarting or redeploying this MCP server.

The MCP response intentionally omits backend `status` and `timestamp` fields and
returns only the source list that is useful for an AI answer.

## MCP Transport

This project is not a regular REST API for AI clients. It uses the Spring AI
MCP WebMVC starter and the Streamable HTTP transport. All MCP traffic goes
through one endpoint:

```text
POST   /mcp   sends MCP JSON-RPC requests and notifications
GET    /mcp   optionally opens a server-to-client event stream
DELETE /mcp   terminates an MCP session
```

Most tool discovery and invocation calls use `POST /mcp`. Depending on the
negotiated session and response type, the server can return either JSON or an
SSE stream. Clients must send `Accept: application/json, text/event-stream` and
must return the `Mcp-Session-Id` header when the server creates a session.

The AI client calls tools through JSON-RPC, for example:

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/call",
  "params": {
    "name": "listSearchServiceSources",
    "arguments": {}
  }
}
```

The MCP tool then calls the backend REST endpoint internally:

```text
tools/call listSearchServiceSources
  -> GET /query/topology/sources
```

## Run

### OAuth security

The HTTP MCP endpoints are OAuth2 Resource Server endpoints. Security is enabled
by default. Configure the corporate OpenID Connect issuer and the public MCP URL:

```bash
export MCP_AUTH_ISSUER_URI=https://auth.example.ru/realms/search
export MCP_AUTH_AUDIENCE=search-engine-mcp
export MCP_RESOURCE_URI=https://mcp.example.ru/mcp
export MCP_RESOURCE_METADATA_URI=https://mcp.example.ru/.well-known/oauth-protected-resource
export SEARCH_SERVICE_BASE_URL=https://iam-proxy.example.ru/search-service
mvn spring-boot:run
```

The server validates the JWT signature, issuer, audience and lifetime. Tool calls
also require these scopes:

```text
sources:read
templates:read
templates:execute
results:read
```

Inbound OAuth protects the MCP endpoints only. The current IAM Proxy accepts a
browser session cookie and does not accept Bearer tokens from CLI applications,
so the MCP server cannot yet authenticate its outbound backend calls safely.
Do not copy a browser cookie into Qwen settings or application configuration:
it is short-lived, user-specific and not designed for service-to-service use.

Before production deployment, IAM must provide one of these supported machine
interfaces: a Bearer-token route, OAuth Token Exchange/On-Behalf-Of, or another
documented delegated-user flow. Personal templates require preserving the user
identity; a shared service account alone is not equivalent.

The public `/.well-known/oauth-protected-resource` endpoint advertises the issuer,
resource and supported scopes. Unauthenticated calls to `/mcp` return HTTP 401
with a `WWW-Authenticate` discovery challenge.

For local development only, security can be explicitly disabled:

```bash
MCP_SECURITY_ENABLED=false mvn spring-boot:run
```

Do not disable security in a shared or deployed environment.

Register Qwen as a public OAuth client with Authorization Code + PKCE. The
Streamable HTTP connection can be configured as follows:

```bash
qwen mcp add \
  --transport http \
  search-engine-mcp \
  https://mcp.example.ru/mcp \
  --oauth-client-id qwen-cli \
  --oauth-authorization-url https://auth.example.ru/realms/search/protocol/openid-connect/auth \
  --oauth-token-url https://auth.example.ru/realms/search/protocol/openid-connect/token \
  --oauth-redirect-uri http://127.0.0.1:7777/oauth/callback \
  --oauth-scopes "openid,profile,sources:read,templates:read,templates:execute,results:read"
```

Do not configure a client secret for a public CLI client. The authorization
server must require PKCE with the `S256` challenge method.

### Starting the application

```bash
mvn spring-boot:run
```

The MCP server listens on port `8081`.

For local development with security disabled, add it to Qwen with:

```bash
qwen mcp add --transport http search-engine-mcp http://localhost:8081/mcp
```

By default, it expects the search service backend at `http://localhost:8080`.
In production, point it to the IAM Proxy route:

```bash
export SEARCH_SERVICE_BASE_URL=https://iam-proxy.example.ru/search-service
```

## Current Shape

```text
AI client
  -> ReportingTools / TemplateTools
    -> SearchServiceTopologyClient / TemplateExecutionService
      -> HttpSearchServiceTopologyClient / HttpTemplateBackendClient
        -> search service backend
```

## Template Backend Contract

The initial template implementation assumes these backend endpoints:

```text
POST /mid/template/list
GET  /mid/template?id={templateId}
GET  /mid/query/topology/structureTable
POST /mid/query/executeQuery
POST /mid/query/executeCrossQuery
POST /mid/query/result
POST /mid/query/crossResult
```

The MCP server builds the execution schema locally by recursively traversing the
template `where.filters` tree. It selects the execution and result endpoints
from `metaInfo.isCross`. Before execution it adds the display name, `templateId`,
and a new `rqUid` to the filled raw template. It converts `TimeRangeUI` to the
absolute `min/max` shape expected by execution endpoints. For a regular query it
also loads the selected table structure and adds every column marked
`isBaseColumn` to `baseColumns`. HTTP 425 and 426 mean that execution is still
pending; HTTP 200 contains the result page.
See [`docs/template-tools.md`](docs/template-tools.md) for request and response
examples and the complete MCP execution flow.
