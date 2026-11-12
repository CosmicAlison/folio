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

## Hosted AI (recommended)

Hosted AI is the default provider configuration. No Ollama image, local models or additional AI SDK is needed. The backend uses its existing HTTP client for an OpenAI-compatible API. Default model IDs are configured for OpenRouter:

```dotenv
AI_ENABLED=true
AI_PROVIDER=hosted
AI_URL=https://openrouter.ai/api/v1
AI_API_KEY=your-provider-key
AI_MODEL=google/gemini-2.5-flash-lite
AI_EMBEDDING_MODEL=openai/text-embedding-3-small
```

Put these values in the ignored root `.env`, or in the deployment platform's backend environment/secrets settings. Keep the key on the server; never put it in a `VITE_` variable, browser storage, source code, or Git. Use a key with a restricted budget and make sure the provider account has credits. Model IDs are configurable because availability and pricing can change.

After configuring the key, apply the changes:

```sh
docker compose up --build -d backend frontend
```

The default Compose stack starts the frontend, backend and PostgreSQL. It does not start Ollama. The app is not publicly hosted merely by configuring a hosted model API.

With `AI_ENABLED=false`, core CRUD and matching-passage search work without credentials or network calls to an AI provider. With AI enabled, missing credentials, provider authentication errors, exhausted credits, rate limits and malformed responses produce a safe HTTP 503 message shown in the UI. Provider error bodies, credentials and prompt text are not included in these error logs or responses. A failed provider request does not silently substitute a generated-looking answer.

In hosted mode, the question and the current user's blog chunks are sent to the provider for embeddings. At most four retrieved passages are sent for answer generation. Each question currently re-embeds the small corpus; the implementation bounds corpus size and generated output, but does not impose application-level daily spending quotas. Configure a provider spending cap for a public demo. Review the provider's data policy before using private writing.

API contracts: [embeddings](https://openrouter.ai/docs/api/api-reference/embeddings/submit-an-embedding-request) and [chat completions](https://openrouter.ai/docs/api/api-reference/chat/create-a-chat-completion).

## Optional local AI

Ollama remains available for users who explicitly want local inference. This route needs substantial disk space for the runtime image and models. It is not required for reviewing the project.

Set all of the following in `.env` when using the Docker profile:

```dotenv
AI_ENABLED=true
AI_PROVIDER=ollama
AI_URL=http://ollama:11434
AI_API_KEY=
AI_MODEL=gemma3:1b
AI_EMBEDDING_MODEL=embeddinggemma
```

Then start the optional service, pull the matching models, and recreate the app with this configuration:

```sh
docker compose --profile ai up -d ollama
docker compose exec -T ollama ollama pull embeddinggemma
docker compose exec -T ollama ollama pull gemma3:1b
docker compose --profile ai up --build -d
```

For a native backend talking to an Ollama process on the same machine, use `AI_URL=http://localhost:11434` instead.

## Local development

Start PostgreSQL separately. Set `DATABASE_URL` (a JDBC URL), `DATABASE_USER`, `DATABASE_PASSWORD`, and `JWT_SECRET` in the backend process environment. Optional: `AI_ENABLED`, `AI_PROVIDER`, `AI_URL`, `AI_API_KEY`, and model names. The default hosted base URL is `https://openrouter.ai/api/v1`. Run `mvn spring-boot:run` in `backend`.

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

Backend tests cover ownership, CRUD, JWT types, chunking, ranking, retrieval isolation, hosted/local provider selection, authenticated provider requests, embedding index ordering, invalid responses and safe provider errors. All provider contract tests use local HTTP stubs and require no real key, credits or model downloads. HTTP integration tests exercise validation, authentication, serialization and cross-user CRUD / assistant access. H2 runs in PostgreSQL compatibility mode for tests; it is not a substitute for PostgreSQL deployment testing. Frontend tests cover reading/filtering, failed-save preservation, successful saves, delete confirmation, cited answers, provider error display and persistent light/dark themes. GitHub Actions runs both suites and the frontend build.

## Architecture and RAG

The API separates controllers, services and repositories. Spring Security authenticates requests; BCrypt hashes passwords; the authenticated principal supplies the owner ID. Post responses omit the user relation and credential fields. Validation bounds titles, bodies and questions. Unexpected failures are logged server-side with generic client errors. React renders post text without HTML injection.

Ask Folio is a bounded, read-only retrieval agent:

1. Load only the authenticated user's posts from PostgreSQL.
2. Split content into 900-character chunks with 200-character overlap.
3. Embed the question and chunks through the selected `AiClient` provider when enabled. The hosted adapter uses `/embeddings`; the optional Ollama adapter uses `/api/embed`.
4. Rank using 75% cosine similarity and 25% lexical overlap; select at most four passages above the threshold.
5. Generate through hosted `/chat/completions` or optional Ollama `/api/generate`, instructing the model to use only supplied evidence, cite post IDs and abstain when unsupported.
6. Return the answer, mode and source excerpts. Never execute tools or mutate posts from model output.

For this small personal library, chunks and vectors are rebuilt per question. Updates and deletes are immediately reflected without stale indexes or a second database. Requests are capped at 500 chunks and report excess size rather than silently omitting posts. Larger libraries should use persisted versioned embeddings, an owner-filtered pgvector index, queued indexing, request quotas and evaluation datasets. Generation may still hallucinate or follow injected text; the read-only boundary limits actions, and visible excerpts let readers verify claims. The similarity threshold is a starting heuristic, not an evaluated confidence score. Questions are independent; conversation history is not stored.

## Assessment and design

The supplied assessment is a requirements reference, not authorization to email, submit or publish. Scope: REST CRUD, database persistence, input validation, error handling and logging, unit tests, responsive React forms and post list/grid, state management, routing, authentication and Docker. Reading, adding and editing use native dialogs within the protected library route. The rename, visual overhaul and assistant are additions requested by the project owner.

The interface uses graphite surfaces, cool blue accents, locally bundled Manrope typography, and a content-first library layout. Light and dark themes cover authentication, the workspace, forms and dialogs. Appearance follows the system on the first visit and saves a manual selection locally. The sidebar has its own keyboard-focusable scroll area; narrow screens use a compact navigation rail. The current reference direction comes from the supplied [AI fintech](https://dribbble.com/shots/26497092-AI-Fintech-Mobile-App), [crypto exchange](https://dribbble.com/shots/27521853-Crypto-Exchange-App-UI-Design), and [music app](https://dribbble.com/shots/26914745-AI-Music-Mood-Mobile-App) designs. Search is explicitly scoped to the current page; assistant retrieval covers the entire personal library within the documented bound. Overview numbers derive from real loaded data.

## Reviewer workflow

1. Review the React UI and REST API using the default Docker setup; no AI key is needed for CRUD or passage search.
2. Run the checks above or inspect [GitHub Actions](https://github.com/CosmicAlison/folio/actions). They exercise both provider contracts without external API calls.
3. To evaluate generated answers, configure the server-side hosted key, create a post with a specific fact, and ask about that fact in Ask Folio. Check the returned passage and citation.
4. Verify user isolation with a second account and confirm that edited/deleted text no longer appears in subsequent answers.

For a browser-only review, deploy the same app and configure its hosted AI key on the server. The live demo and its budget must be operated by the project owner; reviewers should not need to supply a personal key. No public demo is provisioned by this repository.

## Verification in this workspace

- Backend verification packages an executable JAR and exercises hosted AI against an HTTP stub.
- Frontend checks include both themes, persistence, storage-disabled behavior and hosted-provider error messages.
- `npm run build`: TypeScript and Vite production build passed.
- `docker compose --profile ai config --quiet`: passed with placeholder environment variables.
- Local H2 preview: signup and creation of three explicitly labeled example posts succeeded through HTTP.
- Browser automation is unavailable for the latest design iteration. Both theme states are covered by automated behavior tests; no narrow/wide browser or screen-reader pass is claimed.
- The initial Compose stack was built and started successfully; PostgreSQL was healthy, the frontend returned HTTP 200 and the protected API returned HTTP 401 without a token. Live model inference is not verified without a configured key; hosted and Ollama contracts are tested against local HTTP stubs.
