# Full-stack setup

1. Follow [database/README_DB.md](database/README_DB.md) to start MariaDB and load schema/seed. Seed accounts are public development fixtures.
2. Configure backend environment: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, a private random `RECALLRELAY_JWT_SECRET` (at least 32 bytes), and `OPENAI_API_KEY` for AI features. Never commit actual values. Set `FRONTEND_ORIGIN`/`FRONTEND_BASE_URL` to your frontend origin.
3. In `backend`, run `mvn spring-boot:run` with Java 17. Current SDK client initialization requires the server AI environment; offline tests and the mock UI need no AI credentials.
4. In the root, copy `.env.example` to `.env.local`, set `VITE_USE_MOCK=false` and `VITE_API_BASE_URL=http://localhost:8080`, then `npm ci` and `npm run dev`.
5. Log in with the development seed account, inspect a case and invoke each AI endpoint only with synthetic data. External AI calls may incur cost and are not part of CI.

The UI combines implemented API slices and mock operational views. Backend authorization is authoritative; changing a preview role cannot grant access. CI does not claim full MariaDB/email/LLM end-to-end validation. For a credential-free review use the mock UI and `ai-lab/evaluate.py`.
