# Pick Deal Backend

Spring Boot 기반 Pick Deal MVP 백엔드입니다.

## Stack

- Java 17
- Spring Boot 4.0.6
- Gradle
- Spring Web
- Spring Data JPA
- Bean Validation
- H2 in-memory DB
- PostgreSQL driver

## Run

Gradle이 설치된 환경에서 실행합니다.

```bash
gradle bootRun
```

테스트:

```bash
gradle test
```

## Local DB

초기 설정은 H2 in-memory DB를 사용합니다.

- JDBC URL: `jdbc:h2:mem:pickdeal`
- H2 Console: `http://localhost:8080/h2-console`
- Username: `sa`
- Password: empty

애플리케이션 시작 시 `SeedDataInitializer`가 샘플 출처, 키워드, 핫딜 데이터를 등록합니다.

## APIs

- `GET /api/v1/deals`
- `GET /api/v1/deals/{dealId}`
- `POST /api/v1/internal/deals` (내부용)
- `GET /api/v1/sources`
- `PATCH /api/v1/sources/{sourceId}/visibility`
- `GET /api/v1/keywords`
- `POST /api/v1/keywords`
- `DELETE /api/v1/keywords/{keywordId}`

모든 응답은 `ApiResponse` 형태를 사용합니다.

