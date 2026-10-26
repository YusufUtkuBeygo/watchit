# WatchIt

A YouTube-style video platform: users create a channel, upload videos, comment, like, subscribe and build playlists.
Built as a personal full-stack project with a Spring Boot REST API and a React frontend.

> 🚧 Work in progress. The backend API and the main frontend screens (home, watch, channel, search, playlists, history, account) work; server-side search and deployment are next.

## Tech stack

| Layer | Technologies |
|---|---|
| Backend | Java 21, Spring Boot 4, Spring Security, Spring Data JPA, Bean Validation, MapStruct |
| Auth | JWT (jjwt), BCrypt password hashing |
| Database | PostgreSQL 15, Flyway migrations |
| File storage | MinIO (S3-compatible object storage) with presigned upload URLs |
| Frontend | React 19, Vite, React Router, Axios |
| Testing | JUnit 5, Mockito, MockMvc |
| API docs | Swagger UI (springdoc-openapi) with JWT authorization |
| Infrastructure | Docker Compose (PostgreSQL + MinIO), GitHub Actions CI |

## Architecture

```
React (Vite, :5173)
   │  /api/*  ── Vite proxy ──►  Spring Boot REST API (:8080)
   │                                │            │
   │                                ▼            ▼
   │                          PostgreSQL     MinIO (presigned URL)
   │                           (Flyway)           ▲
   └──────────── direct file upload ──────────────┘
```

The backend follows a layered structure: `controller` → `service` → `repository`, with DTOs mapped by MapStruct so entities are never exposed directly.

## Features

- **Accounts:** register, login with JWT, `GET/PUT/DELETE /users/me`
- **Channels:** one channel per user, channel page
- **Videos:** upload with thumbnail, categories, watch page, view counter
- **Search:** find videos by title (currently filtered in the frontend)
- **Social:** comments, video and comment likes (`likeCount`, `likedByMe`), subscriptions, notifications
- **Playlists:** public and private playlists
- **Watch history**

## Engineering decisions

- **Files never pass through the backend.** The API returns a presigned MinIO URL and the browser uploads directly to storage. This avoids Spring's request size limit and keeps large video traffic off the application server.
- **Identity comes from the token, not the request.** User IDs are taken from the JWT (`@AuthenticationPrincipal`), never from the request body or URL, so a user cannot act on someone else's behalf.
- **Ownership checks return `403`.** Updating or deleting another user's video, comment, channel or playlist throws a `ForbiddenException`, handled globally.
- **Read privacy.** Watch history and subscriptions only return the caller's own records; private playlists are hidden from others; public user profiles do not expose email addresses.
- **Schema is versioned with Flyway.** Hibernate runs in `validate` mode, so the database only changes through reviewed migration files (`V1`–`V9`).
- **Secrets stay out of the repo.** Passwords and keys are read from environment variables and git-ignored local files; example files are provided.
- **Fair view counting.** The player reports a view only after 30 seconds (or half of a short video). The backend then counts at most one view per viewer per video every half video length, with a daily cap of 20, so refreshing the page cannot inflate the number.
- **Every push is checked.** GitHub Actions builds and tests the backend against a temporary PostgreSQL database and runs lint and build for the frontend on every push and pull request.
- **Git workflow:** every feature is developed on its own branch and merged through a pull request (30 so far).

## Getting started

**Requirements:** Java 21, Node.js, Docker

### 1. Start PostgreSQL and MinIO

```bash
cd backend
cp .env.example .env            # fill in the database and MinIO credentials
docker compose up -d
```

PostgreSQL runs on `localhost:5433`, MinIO on `localhost:9000` (console: `localhost:9001`).

### 2. Run the backend

```bash
cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties
# fill in DB_PASSWORD, MINIO_SECRET_KEY and JWT_SECRET
./gradlew bootRun
```

The API runs on `http://localhost:8080`. Flyway creates the schema on first start.
Interactive API docs: `http://localhost:8080/swagger-ui.html` (use **Authorize** to paste a JWT).

**Optional demo data:** after the first start, load sample users, channels and videos into the empty database:

```bash
docker exec -i postgres_veritabani psql -U postgres -d watchit < seed/ornek-veri.sql
```

Demo accounts and their password are listed at the top of `seed/ornek-veri.sql`.

### 3. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`.

## API overview

| Resource | Endpoints |
|---|---|
| Users | `POST /users` · `POST /users/login` · `GET/PUT/DELETE /users/me` · `GET /users/{id}` |
| Channels | `/channels` CRUD · `GET /channels/me` |
| Videos | `/videos` CRUD · `POST /videos/{id}/view` |
| Files | `POST /api/files/upload-url` (presigned URL) |
| Comments | `/comments` CRUD · `/commentLikes` |
| Likes | `/videoLikes` |
| Subscriptions | `/subscriptions` |
| Playlists | `/playlists` · `/playlistvideos` |
| Others | `/categories` · `/notifications` · `/watchHistory` |

## Roadmap

- [x] Registration, channel page and the main frontend screens
- [x] CI pipeline and first tests (view counter: service, limiter and endpoint security)
- [ ] Server-side, accent-insensitive search
- [ ] Wider test coverage
- [ ] Deployment

## Author

**Yusuf Utku Beygo** · Software Engineering student, Gümüşhane University
