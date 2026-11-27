# WatchIt

A YouTube-style video platform: users create a channel, upload videos, comment, like, subscribe and build playlists.
Built as a personal full-stack project with a Spring Boot REST API and a React frontend.

> 🚧 Work in progress. The backend API and the main frontend screens (home, watch, channel, search, playlists, history, account) work; deployment is next.

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
| AI | Whisper (faster-whisper, local container), Spring AI, Gemini, pgvector |
| Infrastructure | Docker Compose (PostgreSQL + pgvector, MinIO, Whisper), GitHub Actions CI |

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

- **Accounts:** register, login with JWT, profile and password change, account deletion
- **Channels:** one channel per user, channel page
- **Videos:** upload with thumbnail, categories, watch page, view counter
- **Home feed:** "for you" ranking (popularity, subscribed channels, watched categories, freshness; watched videos sink), infinite scroll on every list
- **Search:** title, description and channel name, case- and accent-insensitive; plus a separate "said in the video" section that finds the exact word in transcripts and links to the second it is spoken (`?t=`)
- **Chapters:** generated once per video from the transcript, clickable on the watch page
- **Social:** comments (newest / top, pinned by the video owner), video and comment likes, subscriptions
- **Moderation:** report a video or comment; admin panel to remove content (owner is notified) or dismiss; admin role is set in the database
- **Studio:** your videos with processing status, daily views chart (last 28 days)
- **Abuse protection:** per-user / per-IP rate limits on comments, likes, subscriptions, uploads and sign-ups (429 + `Retry-After`)
- **Ask the video:** a panel on the watch page; ask a question in any language and get an answer grounded only in what is said in the video, with clickable minutes that jump the player there. Questions about a moment ("what happens at the start / at 03:20?") pick chunks by time, "what is this video about?" uses a summary written once per video.
- **Notifications:** new subscriber, comment, reply, like and new video from a subscribed channel
- **Playlists:** public and private playlists
- **Watch history**

## Engineering decisions

- **Files never pass through the backend.** The API returns a presigned MinIO URL and the browser uploads directly to storage. This avoids Spring's request size limit and keeps large video traffic off the application server.
- **Identity comes from the token, not the request.** User IDs are taken from the JWT (`@AuthenticationPrincipal`), never from the request body or URL, so a user cannot act on someone else's behalf.
- **Ownership checks return `403`.** Updating or deleting another user's video, comment, channel or playlist throws a `ForbiddenException`, handled globally.
- **Read privacy.** Watch history and subscriptions only return the caller's own records; private playlists are hidden from others; public user profiles do not expose email addresses.
- **Schema is versioned with Flyway.** Hibernate runs in `validate` mode, so the database only changes through reviewed migration files (`V1`–`V10`).
- **Secrets stay out of the repo.** Passwords and keys are read from environment variables and git-ignored local files; example files are provided.
- **Fair view counting.** The player reports a view only after 30 seconds (or half of a short video). The backend then counts at most one view per viewer per video every half video length, with a daily cap of 20, so refreshing the page cannot inflate the number.
- **Search that understands Turkish.** Titles are matched with PostgreSQL `lower(unaccent(...))`, so "istanbul" finds "İstanbul". The built-in case-insensitive query uses `upper()` and missed these on an `en_US` database. `%`, `_` and `\` in the query are escaped, and queries longer than 100 characters are rejected with `400`.
- **Notifications are written in the same transaction.** Subscribing, commenting, liking and uploading create the notification inside one `@Transactional` method, so a failure rolls back both; a test proves the rollback. Users never get notified about their own actions.
- **Sensitive changes need the current password.** Changing the password or the email requires the current password, and wrong attempts share the login limit (5 per 15 minutes), so a stolen token cannot take over the account.
- **Answers come from the video, not from the model's memory (RAG).** On upload, a local Whisper container turns speech into timestamped text in the background. The text is split into 30-60 second overlapping chunks and each chunk is stored as a 768-dimension Gemini embedding in PostgreSQL (pgvector). A question is embedded the same way, the closest chunks of that video are retrieved, and Gemini answers only from them, citing minutes. If no chunk passes a similarity threshold, Gemini is not called at all and the API answers "not found"; the transcript is treated as data, so instructions inside it are ignored.
- **Every upload plays everywhere.** After upload, the same media container converts the video in the background to at most 720p H.264/AAC MP4 with `faststart` (files that already fit are only repackaged, in seconds). The backend stores the result in MinIO, switches the video to it with a compare-and-swap update (so an edit made meanwhile is never overwritten) and deletes the original. The transcript starts only after this step, so Whisper always reads the final file. If conversion fails, the original keeps playing.
- **Every push is checked.** GitHub Actions builds and tests the backend against a temporary PostgreSQL database and runs lint and build for the frontend on every push and pull request.
- **Git workflow:** every feature is developed on its own branch and merged through a pull request (over 30 so far).

## Getting started

**Requirements:** Java 21, Node.js, Docker

### 1. Start PostgreSQL and MinIO

```bash
cd backend
cp .env.example .env            # fill in the database and MinIO credentials
docker compose up -d
```

PostgreSQL runs on `localhost:5433`, MinIO on `localhost:9000` (console: `localhost:9001`), the Whisper service on `localhost:9010`. The first transcript downloads the Whisper model (about 1.5 GB) once.

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

### Running everything with Docker

`compose.prod.yaml` builds and runs the whole stack (PostgreSQL, MinIO, media service, backend, frontend behind nginx):

```bash
cp .env.prod.example .env.prod   # fill in the secrets; .env.prod is git-ignored
docker compose -f compose.prod.yaml --env-file .env.prod up -d --build
```

The site is served on port 80, `/api` is proxied to the backend, `GET /api/actuator/health` returns `{"status":"UP"}`.
Secrets are passed as environment variables only; they are never copied into the images.

## API overview

| Resource | Endpoints |
|---|---|
| Users | `POST /users` · `POST /users/login` · `GET/PUT/DELETE /users/me` · `PUT /users/me/password` · `GET /users/{id}` |
| Channels | `/channels` CRUD · `GET /channels/me` |
| Videos | `/videos` CRUD · `GET /videos?sort=foryou\|new\|popular&categoryId=&channelId=&maxDuration=&search=&page=&size=` · `GET /videos/subscriptions` · `GET /videos/liked` · `GET /videos/transcript-search?q=` · `POST /videos/{id}/view` |
| AI | `GET/POST /videos/{id}/transcript` · `POST /videos/{id}/questions` · `GET /videos/{id}/chapters` |
| Studio | `GET /channels/me/stats?days=28` |
| Moderation | `POST /reports` · `GET /admin/reports` · `POST /admin/reports/{id}/remove-content` · `POST /admin/reports/{id}/dismiss` · `GET /admin/feedback` |
| Files | `POST /api/files/upload-url` (presigned URL) |
| Comments | `/comments` CRUD · `GET /comments?videoId=&sort=new\|top` · `PUT /comments/{id}/pin` · `/commentLikes` |
| Likes | `/videoLikes` |
| Subscriptions | `/subscriptions` |
| Playlists | `/playlists` · `/playlistvideos` |
| Others | `/categories` · `/notifications` (+ `unread-count`, `read-all`) · `/watchHistory` · `/actuator/health` |

All list endpoints are paginated: `?page=0&size=24` (max 50) → `{ items, page, size, hasNext, total }`.

## Roadmap

- [x] Registration, channel page and the main frontend screens
- [x] CI pipeline and first tests (view counter: service, limiter and endpoint security)
- [x] Server-side, accent-insensitive search (with unit tests and PostgreSQL query tests)
- [x] Notifications for subscriptions, comments, likes and new videos
- [x] Ask questions about a video (backend: Whisper transcript, pgvector retrieval, Gemini answers)
- [x] "Ask the video" panel on the watch page (time and summary questions, clickable minutes)
- [x] Evaluation of "Ask the video" on 3 videos and 30 questions: Hit@3 100%, accuracy 95%, "not found" 100% ([report, in Turkish](docs/soru-cevap-degerlendirme.md), `./gradlew eval`)
- [x] Background video conversion to 720p H.264/AAC MP4
- [x] Pagination on every list, rate limits, reports and admin role
- [x] Automatic chapters, transcript word search, answer cache, automatic thumbnails, studio chart
- [x] Docker images (backend, frontend + nginx), health endpoint, production compose file
- [ ] Deployment

## Author

**Yusuf Utku Beygo** · Software Engineering student, Gümüşhane University
