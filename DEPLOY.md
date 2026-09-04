# Deploy: Render (Backend) + Vercel (Frontend)

Deploy the **Spring Boot API on Render** and the **Angular UI on Vercel**.

## Prerequisites

- GitHub repository with this project
- [Render](https://render.com) account
- [Vercel](https://vercel.com) account

---

## 1. Deploy backend on Render

### Option A: Blueprint (recommended)

1. Push the repo to GitHub.
2. Render → **New** → **Blueprint** → connect repo → apply `render.yaml`.
3. When prompted, set:
   - `CORS_ALLOWED_ORIGINS` = your Vercel URL (e.g. `https://your-app.vercel.app`)
4. Deploy and note the backend URL: `https://inventory-backend.onrender.com`

### Option B: Manual

1. **New Web Service** → connect repo
2. **Root Directory:** `backend`
3. **Runtime:** Docker
4. Create a **PostgreSQL** database and link it to the service
5. Environment variables:

| Key | Value |
|-----|-------|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `CORS_ALLOWED_ORIGINS` | `https://your-app.vercel.app` |
| `DATABASE_URL` | (auto from linked DB) |

6. **Health Check Path:** `/api/products/summary`

### Verify

```bash
curl https://YOUR-BACKEND.onrender.com/api/products/summary
```

---

## 2. Deploy frontend on Vercel

1. **Add New Project** → import GitHub repo
2. **Root Directory:** `frontend`
3. **Build Command:** `npm run build:vercel`
4. **Output Directory:** `dist/frontend/browser`
5. Environment variable:

| Key | Value |
|-----|-------|
| `API_URL` | `https://YOUR-BACKEND.onrender.com` |

6. Deploy and copy your Vercel URL.

---

## 3. Connect both services

1. In Render, set `CORS_ALLOWED_ORIGINS` to your exact Vercel URL.
2. Redeploy the backend.
3. Open the Vercel site and test **Import Data**.

---

## Production notes

- Render `prod` profile uses **PostgreSQL** (data persists; H2 is local-only).
- Vercel builds inject `API_URL` into `environment.prod.ts` via `scripts/set-env.js`.
- Render free tier may sleep; first request after idle can take ~30 seconds.

## Troubleshooting

| Issue | Fix |
|-------|-----|
| CORS error | Match exact Vercel URL in `CORS_ALLOWED_ORIGINS` |
| API 404 | Check `API_URL` in Vercel and redeploy |
| Backend crash on start | Ensure PostgreSQL is linked and `SPRING_PROFILES_ACTIVE=prod` |
