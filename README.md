# Folio

A private blog workspace with a read-only research assistant. An iteration on the supplied Daily Blogger project, retaining Java 17 / Spring Boot 4, React 19, TypeScript, Vite and PostgreSQL.

## Run with Docker

Requires Docker Desktop (Linux containers) or Docker Engine with Compose.

1. Copy `.env.example` to `.env`.
2. Set a random `DATABASE_PASSWORD` and a Base64-encoded 32-byte `JWT_SECRET`. Generate the secret with `openssl rand -base64 32`, or PowerShell 7: `[Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32))`.
3. Run `docker compose up --build -d --wait`.
4. Open http://localhost:3000 and create an account. No seeded users or default credentials.

Nginx serves the SPA and proxies `/api` to Spring, so API URLs are not baked into the frontend. Only the web port is exposed; PostgreSQL persists in a named volume. Set `PORT` to change the web port. `docker compose down` retains data; deleting volumes deletes data.

Builds install dependencies inside containers. Maven and npm caches speed subsequent builds; backend runtime images run as an unprivileged user. Readiness probes wait for PostgreSQL and the backend before starting the frontend. The default stack contains only `db`, `backend` and `frontend`; local inference is an explicit overlay.

## Deploy a public demo

[Deploy Folio on Render](https://dashboard.render.com/blueprint/new?repo=https://github.com/CosmicAlison/folio)

The checked-in `render.yaml` creates a free Docker web service and a free PostgreSQL 16 database in the same region. The root `Dockerfile` bundles the React build inside Spring Boot, serving the UI and `/api` from one HTTPS origin. Render generates the JWT secret and supplies private database credentials; no local `.env` file is uploaded. `/healthz` checks database connectivity. `PORT` selects the listening port on this image.

Connect the repository, review the two free resources and apply the Blueprint. Use the resulting web-service URL for reviewers. This deploy button opens setup; it is not a running demo URL. The cloud database starts empty: sign up to create your own workspace. Existing local data is not copied.

Render's free web service sleeps after 15 minutes of inactivity and can take around a minute to wake. Its free database expires after 30 days. This configuration is for a temporary assessment demo; choose a paid database and backup policy for lasting use. See [free-tier limits](https://render.com/docs/free).

The Blueprint initially sets `AI_ENABLED=false`, so CRUD and passage search work without provider charges. To enable generated answers, add `AI_API_KEY` as a service secret and change `AI_ENABLED` to `"true"` in the Blueprint, then sync and deploy. Use a provider spending cap and available credits. A Dashboard-only change to a variable managed by the Blueprint can be overwritten on the next sync. Never put the secret value in `render.yaml`.

After deployment, run the dependency-free HTTP check from any machine with Python 3:

```sh
python scripts/smoke.py --base-url https://YOUR-SERVICE.onrender.com
```

It verifies the frontend, JavaScript assets, login routing, protected API, signup/login, PostgreSQL CRUD and cited passage search. It creates and removes a temporary account and post. Add `--check-ai` only when generation is enabled and provider calls are intended.

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

Ollama is defined only in the separate `compose.ollama.yaml` overlay. The default Compose file contains no local inference service or model volume. Local inference needs substantial free disk space and is not required for reviewing the project.

The overlay selects Ollama and its local model names without changing the hosted settings in `.env`. Start the optional service, pull the default models, then build and start the app with the same overlay:

```sh
docker compose -f docker-compose.yaml -f compose.ollama.yaml up -d ollama
docker compose -f docker-compose.yaml -f compose.ollama.yaml exec -T ollama ollama pull embeddinggemma
docker compose -f docker-compose.yaml -f compose.ollama.yaml exec -T ollama ollama pull gemma3:1b
docker compose -f docker-compose.yaml -f compose.ollama.yaml up --build -d
```

Optional `OLLAMA_MODEL` and `OLLAMA_EMBEDDING_MODEL` variables override those defaults; pull the matching names when overriding them. The overlay deliberately clears the hosted API key from the local backend environment.

To return to hosted AI after running an older local stack:

```sh
docker compose -f docker-compose.yaml -f compose.ollama.yaml stop ollama
docker compose up --build -d --force-recreate backend frontend
```

Stopping Ollama retains its model volume. These commands do not delete the PostgreSQL volume. For a native backend using local inference, configure `AI_PROVIDER=ollama`, `AI_URL=http://localhost:11434`, and matching local `AI_MODEL` / `AI_EMBEDDING_MODEL` values.

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

Backend tests cover ownership, CRUD, JWT types, chunking, ranking, retrieval isolation, hosted/local provider selection, authenticated provider requests, embedding index ordering, invalid responses and safe provider errors. All provider contract tests use local HTTP stubs and require no real key, credits or model downloads. HTTP integration tests exercise validation, authentication, serialization, deployment routes and cross-user CRUD / assistant access. H2 runs in PostgreSQL compatibility mode for tests. Frontend tests cover reading/filtering, failed-save preservation, successful saves, delete confirmation, cited answers, provider error display and persistent light/dark themes. GitHub Actions runs both suites and the frontend build, builds the actual Compose stack, tests it against PostgreSQL, then builds and tests the combined cloud image. No live AI credentials are needed in CI.

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

For a browser-only review, use the public demo deployment above and configure its hosted AI key on the server. The live demo and its budget must be operated by the project owner; reviewers should not need to supply a personal key.

## Verification in this workspace

- Backend verification packages an executable JAR and exercises hosted AI against an HTTP stub.
- Frontend checks include both themes, persistence, storage-disabled behavior and hosted-provider error messages.
- `npm run build`: TypeScript and Vite production build passed.
- `docker compose config --quiet`: passed with placeholder environment variables.
- Local H2 preview: signup and creation of three explicitly labeled example posts succeeded through HTTP.
- Browser automation is unavailable for the latest design iteration. Both theme states are covered by automated behavior tests; no narrow/wide browser or screen-reader pass is claimed.
- The initial Compose stack was built and started successfully; PostgreSQL was healthy, the frontend returned HTTP 200 and the protected API returned HTTP 401 without a token. Hosted and Ollama contracts are tested against local HTTP stubs. Live inference requires a configured provider key with available credit.
