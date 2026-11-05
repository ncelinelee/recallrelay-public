# RecallRelay deployment patch

## Architecture
- Frontend: Vercel
- Backend: Railway
- Database: Railway MariaDB 11.4

## Backend production variables
PORT is injected by Railway.
Set:
- DB_URL=jdbc:mariadb://mariadb.railway.internal:3306/recallrelay
- DB_USERNAME=recallrelay
- DB_PASSWORD=<same value as MariaDB service>
- FRONTEND_ORIGIN=https://<your-vercel-domain>
- RECALLRELAY_JWT_SECRET=<long random secret, 32+ bytes>
- JWT_EXPIRATION_SECONDS=28800

## MariaDB service variables
- MARIADB_DATABASE=recallrelay
- MARIADB_USER=recallrelay
- MARIADB_PASSWORD=<choose strong password>
- MARIADB_ROOT_PASSWORD=<choose separate strong root password>

Attach a Railway Volume to /var/lib/mysql.
The Dockerfile copies schema.sql and seed.sql into /docker-entrypoint-initdb.d, so they run only when the database volume is empty.

## Backend health check
After adding /health to SecurityConfig permitAll, set Railway Healthcheck Path to /health.

## Frontend
Set Vercel environment variables:
- VITE_USE_MOCK=false
- VITE_API_BASE_URL=https://<backend-domain>

vercel.json ensures Vue Router history URLs resolve to index.html.
