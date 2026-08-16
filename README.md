# ApiGuildMaster

## Local configuration

The API requires these environment variables:

```bash
export DB_URL='jdbc:mysql://localhost:3306/GuildMaster?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true'
export DB_USERNAME='root'
export DB_PASSWORD='<local-password>'
export JWT_SECRET='<random-secret-at-least-32-bytes>'
```

Optional variables are documented in `.env.example`.

Use `SPRING_PROFILES_ACTIVE=local` for local development and
`SPRING_PROFILES_ACTIVE=prod` only after a versioned database migration has
been applied.

Run tests with:

```bash
bash ./mvnw test
```

Run the API with:

```bash
bash ./mvnw spring-boot:run
```

The API listens on port `8081` by default. Health is available at
`/actuator/health`.

## Container

Build and run the image by injecting the variables through the runtime or a
secrets manager:

```bash
docker build -t guildmaster-api .
docker run --rm -p 8081:8081 --env-file .env guildmaster-api
```

Never commit `.env` or production secrets.
