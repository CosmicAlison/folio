# Folio

## Preview: [link](https://folio-ug3h.onrender.com/)

Folio is a private blog workspace. Create an account, write posts, browse your library, and update or delete your writing.

I also added **Ask Folio**, an assistant that answers questions using your own posts and shows the passages behind its answers.

## Built with

- **Frontend:** React 19, TypeScript, Vite, Tailwind CSS and React Router.
- **Backend:** Java 17, Spring Boot, Spring Security and Spring Data JPA.
- **Database:** PostgreSQL.
- **Testing:** JUnit, Mockito, H2, Vitest and React Testing Library.
- **Deployment:** Docker Compose, GitHub Actions and a Render deployment configuration.

## Run it locally

You only need Docker with Compose. Dependencies are installed inside the containers.

1. Copy `.env.example` to `.env`.
2. Set `DATABASE_PASSWORD` and `JWT_SECRET`. The JWT secret must contain 32 random bytes encoded as Base64; you can generate one with `openssl rand -base64 32`.
3. Start the app:

```sh
docker compose up --build -d --wait
```

Open **http://localhost:3000** and create an account.

To stop it, run `docker compose down`. Your database stays in a Docker volume.

## How it meets the assignment

The REST API supports creating, listing, updating and deleting posts, with database persistence, input validation, logging and consistent error responses.

The React interface includes create/edit forms, a reading view, delete confirmation, pagination, and list/grid layouts. React hooks manage form and library state; Context manages authentication.

React Router handles the login and protected workspace routes. Reading and editing happen in dialogs so you can stay in your library.

## Design choices

I separated the backend into controllers, services and repositories to keep request handling, business rules and database access easy to maintain.

Each account has its own posts. JWT authentication protects the API, BCrypt hashes passwords, and ownership checks prevent access to another user's writing.

The interface uses restrained typography, clear spacing, responsive layouts, and light/dark themes to keep the focus on reading and writing.

## Extras

**Ask Folio** splits your posts into overlapping passages, retrieves relevant content using embeddings and keyword matching, then generates an answer with source citations. It is read-only and cannot change your posts.

Hosted AI uses OpenRouter. Set `AI_ENABLED=true` and `AI_API_KEY` in `.env`, then recreate the backend:

```sh
docker compose up -d backend
```

A funded provider key is required. Questions and blog passages are sent to that provider; keep the key server-side and out of Git. With AI disabled, matching-passage search still works. Ollama is available through a separate optional Compose overlay.

## Tests and deployment

GitHub Actions runs the backend and frontend tests, builds both Docker setups, and checks authentication, PostgreSQL CRUD and assistant citations over HTTP.

To run the unit tests yourself:

```sh
cd backend
mvn verify
cd ../frontend
npm ci
npm test
```

For cloud deployment, [use the Render configuration](https://dashboard.render.com/blueprint/new?repo=https://github.com/CosmicAlison/folio). It serves the frontend and API together with a managed PostgreSQL database.

The Blueprint enables hosted AI and asks for `AI_API_KEY` during initial setup. For an existing service, add the key in Render's Environment settings, sync the Blueprint and redeploy. The key stays out of Git and needs available provider credit.
