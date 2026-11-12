# Folio

A private blog workspace with a read-only research assistant. An iteration on the supplied Daily Blogger project, retaining Java 17 / Spring Boot 4, React 19, TypeScript, Vite and PostgreSQL.

## Run with Docker

Requires Docker Desktop (Linux containers) or Docker Engine with Compose.

1. Copy `.env.example` to `.env`.
2. Set a random `DATABASE_PASSWORD` and a Base64-encoded 32-byte `JWT_SECRET`. Generate the secret with `openssl rand -base64 32`, or PowerShell 7: `[Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32))`.
3. Run `docker compose up --build -d`.
4. Open http://localhost:3000 and create an account. No seeded users or default credentials.

Nginx serves the SPA and proxies `/api` to Spring, so API URLs are not baked into the frontend. Only the web port is exposed; PostgreSQL persists in a named volume. Set `PORT` to change the web port. `docker compose down` retains data; deleting volumes deletes data.

For an internet deployment, run the same stack on a Docker host behind a TLS reverse proxy, set strong secrets, restrict host access, back up PostgreSQL, and pin reviewed container image digests. This repository prepares deployment; it does not provision cloud infrastructure or publish your data.

## Enable local AI

No paid API key is needed. The optional Ollama service runs models on your machine; allow sufficient memory, disk and CPU for the chosen models.

```sh
docker compose --profile ai up -d ollama
docker compose exec ollama ollama pull embeddinggemma
docker compose exec ollama ollama pull gemma3:1b
```

Set `AI_ENABLED=true` in `.env`, then run `docker compose --profile ai up --build -d`. Model names are configurable with `AI_MODEL` and `AI_EMBEDDING_MODEL`. Pull matching models before changing these settings.

Without AI enabled, Ask Folio explicitly returns matching passages in **search mode**. It never pretends that a language model answered. With AI enabled, provider failures return HTTP 503 rather than silently substituting search results.

## Local development

Start PostgreSQL separately. Set `DATABASE_URL` (a JDBC URL), `DATABASE_USER`, `DATABASE_PASSWORD`, and `JWT_SECRET` in the backend process environment. Optional: `AI_ENABLED`, `AI_URL` (default `http://localhost:11434`), and model names. Run `mvn spring-boot:run` in `backend`.

In `frontend`, run `npm ci` then `npm run dev`. Vite proxies `/api` to localhost:8080. For a separately hosted API, set `VITE_API_URL` **at build time** and configure allowed origins in Spring Security. The default deployment uses one origin.

Sessions deliberately use in-memory access tokens; reloading requires sign-in. A future persistent session should use rotated refresh tokens in HttpOnly cookies with CSRF protection rather than browser storage. Access and refresh tokens have separate types and cannot be interchanged.

## Checks

```sh
cd backend
mvn verify
cd ../frontend
npm ci
npm test
npm run build
```

Backend tests cover ownership, CRUD, JWT types, chunking, ranking and retrieval isolation. HTTP integration tests exercise validation, authentication, serialization and cross-user CRUD / assistant access. H2 runs in PostgreSQL compatibility mode for tests; it is not a substitute for PostgreSQL deployment testing. Frontend tests cover reading/filtering, failed-save preservation, successful saves, delete confirmation and cited answers. GitHub Actions runs both suites and the frontend build.

## Architecture and RAG

The API separates controllers, services and repositories. Spring Security authenticates requests; BCrypt hashes passwords; the authenticated principal supplies the owner ID. Post responses omit the user relation and credential fields. Validation bounds titles, bodies and questions. Unexpected failures are logged server-side with generic client errors. React renders post text without HTML injection.

Ask Folio is a bounded, read-only retrieval agent:

1. Load only the authenticated user's posts from PostgreSQL.
2. Split content into 900-character chunks with 200-character overlap.
3. Embed the question and chunks through Ollama `/api/embed` when enabled.
4. Rank using 75% cosine similarity and 25% lexical overlap; select at most four passages above the threshold.
5. Generate through `/api/generate`, instructing the model to use only supplied evidence, cite post IDs and abstain when unsupported.
6. Return the answer, mode and source excerpts. Never execute tools or mutate posts from model output.

For this small personal library, chunks and vectors are rebuilt per question. Updates and deletes are immediately reflected without stale indexes or a second database. Requests are capped at 500 chunks and report excess size rather than silently omitting posts. Larger libraries should use persisted versioned embeddings, an owner-filtered pgvector index, queued indexing, request quotas and evaluation datasets. Generation may still hallucinate or follow injected text; the read-only boundary limits actions, and visible excerpts let readers verify claims. The similarity threshold is a starting heuristic, not an evaluated confidence score. Questions are independent; conversation history is not stored.

## Assessment and design

The supplied assessment is a requirements reference, not authorization to email, submit or publish. Scope: REST CRUD, database persistence, input validation, error handling and logging, unit tests, responsive React forms and post list/grid, state management, routing, authentication and Docker. Reading, adding and editing use native dialogs within the protected library route. The rename, visual overhaul and assistant are additions requested by the project owner.

The interface uses graphite surfaces, cool blue accents, locally bundled Manrope typography, and a content-first library layout. Light and dark themes cover authentication, the workspace, forms and dialogs. Appearance follows the system on the first visit and saves a manual selection locally. The sidebar has its own keyboard-focusable scroll area; narrow screens use a compact navigation rail. The current reference direction comes from the supplied [AI fintech](https://dribbble.com/shots/26497092-AI-Fintech-Mobile-App), [crypto exchange](https://dribbble.com/shots/27521853-Crypto-Exchange-App-UI-Design), and [music app](https://dribbble.com/shots/26914745-AI-Music-Mood-Mobile-App) designs. Search is explicitly scoped to the current page; assistant retrieval covers the entire personal library within the documented bound. Overview numbers derive from real loaded data.

## Verification in this workspace

- `mvn verify`: 21 backend tests passed; executable JAR packaged.
- `npm test`: 7 frontend tests passed, including both themes, persistence and storage-disabled behavior.
- `npm run build`: TypeScript and Vite production build passed.
- `docker compose --profile ai config --quiet`: passed with placeholder environment variables.
- Local H2 preview: signup and creation of three explicitly labeled example posts succeeded through HTTP.
- Browser automation is unavailable for the latest design iteration. Both theme states are covered by automated behavior tests; no narrow/wide browser or screen-reader pass is claimed.
- The initial Compose stack was built and started successfully; PostgreSQL was healthy, the frontend returned HTTP 200 and the protected API returned HTTP 401 without a token. Real Ollama inference is not verified; the provider contract was tested against a local HTTP stub.
