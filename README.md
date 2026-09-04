# Product Inventory Management Application

A full-stack Product Inventory Management web application for importing, validating, and managing product inventory records from Excel (`.xlsx`) and CSV (`.csv`) files.

## Features

- Import product data from `.xlsx` and `.csv` files
- Comprehensive file and row-level validation with actionable error messages
- Excel formula evaluation using Apache POI `FormulaEvaluator`
- Duplicate detection within uploaded files and against existing database records
- Atomic transactional imports (all-or-nothing)
- Paginated, sortable product listing with search
- Dashboard summary metrics:
  - Total products
  - Total inventory value (`Unit Price × Quantity`, using `BigDecimal`)
  - Average stock age (dynamically calculated)
- Angular Material UI with responsive layout, loading states, and error handling

## Architecture

The solution follows a clean layered architecture:

```
backend/
  controller/     REST API endpoints
  service/        Business logic and orchestration
  repository/     Spring Data JPA persistence
  entity/         JPA entities
  dto/            API request/response models
  mapper/         Entity/DTO mapping
  exception/      Centralized error handling (@RestControllerAdvice)
  validation/     Product validation rules
  imports/parser/ File parsing (Excel/CSV strategy pattern)

frontend/
  components/     Dashboard and import dialog
  services/       HTTP API client
  models/         TypeScript interfaces
```

## Technology Stack

| Layer | Technologies |
|-------|-------------|
| Backend | Java 21, Spring Boot 3.4, Spring Web, Spring Data JPA, Bean Validation, Apache POI, OpenCSV, H2, JUnit 5, Mockito |
| Frontend | Angular 19, TypeScript, Angular Material, RxJS |
| Infrastructure | Docker, docker-compose, Nginx (frontend reverse proxy) |

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/products/import` | Multipart file upload (`file` parameter) |
| `GET` | `/api/products` | Paginated product list (`page`, `size`, `sort`, optional `search`) |
| `GET` | `/api/products/summary` | Dashboard summary metrics |

## Input File Format

Required columns (exact names):

| Column | Type | Example |
|--------|------|---------|
| Product SKU | Text | SKU001 |
| Product Name | Text | Laptop |
| Category | Text | Electronics |
| Purchase Date | Text (`uuuu-MM-dd`) | 2025-01-10 |
| Unit Price | Currency/Number | 750.00 |
| Quantity | Integer | 5 |

Sample files are in `sample-data/`.

## Validation Rules

1. File must not be empty
2. Extension must be `.xlsx` or `.csv`
3. Required columns must exist with exact names
4. Mandatory fields cannot be blank
5. Purchase Date must be `uuuu-MM-dd`
6. Unit Price must be positive
7. Quantity must be a non-negative integer
8. `Product SKU + Purchase Date` must be unique in file and database

## Duplicate Handling

- Composite unique constraint: `UNIQUE(product_sku, purchase_date)`
- Full-file validation before any import
- Transactional all-or-nothing import

## Excel Formula Evaluation

Formulas are evaluated via Apache POI `FormulaEvaluator`. Example: `=100+50` imports as `150`.

## Stock Age Calculation

`ChronoUnit.DAYS.between(purchaseDate, LocalDate.now(clock))` — not stored in DB.

## Summary Calculations

- Total Products: count of records
- Total Inventory Value: `SUM(unit_price * quantity)` with `BigDecimal`
- Average Stock Age: mean of computed stock ages (1 decimal place)

## How to Run with Docker

```bash
docker-compose up --build
```

| Service | URL |
|---------|-----|
| Frontend | http://localhost:4200 |
| Backend API | http://localhost:8080/api |

## Deploy to Render + Vercel

See **[DEPLOY.md](DEPLOY.md)** for step-by-step instructions to deploy:
- **Backend** → Render (Docker + PostgreSQL)
- **Frontend** → Vercel (Angular)

Quick summary:
1. Deploy backend on Render using `render.yaml` or manual Docker setup
2. Deploy frontend on Vercel with root directory `frontend`
3. Set Vercel env `API_URL=https://your-backend.onrender.com`
4. Set Render env `CORS_ALLOWED_ORIGINS=https://your-app.vercel.app`

## How to Run Locally

### Backend

```bash
cd backend
mvnw.cmd test
mvnw.cmd spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm start
```

## How to Run Tests

```bash
cd backend
mvnw.cmd test

cd frontend
npm run build
```

## Design Decisions

1. Strategy pattern for Excel/CSV parsers
2. Validate-then-import with atomic transactions
3. `BigDecimal` for monetary calculations
4. `Clock` injection for testable date logic
5. DTOs instead of exposing JPA entities
6. Nginx proxies `/api` in Docker for same-origin requests

## Assumptions

1. Date format is `uuuu-MM-dd` (locale-independent)
2. Currency displayed as USD in UI
3. 10 MB upload limit
4. No authentication required
5. H2 in-memory database (non-persistent across restarts)
6. Stock Age is dynamically calculated from Purchase Date and is server-sortable by mapping the sort to Purchase Date with the inverse direction.
