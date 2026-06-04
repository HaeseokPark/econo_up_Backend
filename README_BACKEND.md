# Econo-up Backend

Spring Boot backend for the first MVP scope:

- Google login
- JWT authentication
- Onboarding
- Curriculum import from `src/main/resources/data/curriculum.xlsx`
- Category, unit, stage, session APIs
- Learning attempt, answer, and completion APIs

## Local environment

Set secrets and DB credentials as environment variables.

```properties
DB_HOST=localhost
DB_PORT=3306
DB_NAME=econoup
DB_USERNAME=root
DB_PASSWORD=your-password
GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
JWT_SECRET=replace-with-a-long-random-secret
```

Do not commit real passwords or Google client secrets.

## Run

```bash
gradle bootRun
```

The curriculum xlsx is imported once on startup when the database has no sessions.
