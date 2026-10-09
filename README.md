# PickDeal

여러 커뮤니티의 핫딜을 수집해 한곳에서 조회하는 단일 사용자 MVP.

- Frontend: `frontend/` — Next.js App Router, TypeScript, Tailwind CSS
- Backend: `backend/` — Spring Boot, Java 17, Gradle, JPA
- Database: PostgreSQL

## 로컬 실행

Backend (`backend/`, 로컬 PostgreSQL `pickdeal` DB 필요. 접속 정보는 `DB_USERNAME`·`DB_PASSWORD`):

```bash
./gradlew bootRun
```

Frontend (`frontend/`, API 주소는 `NEXT_PUBLIC_API_BASE_URL`):

```bash
npm install
npm run dev
```

Docker Compose 실행은 `.env.example`을 `.env`로 복사해 값을 채운 뒤 `docs/06-deployment.md`를 따른다.

## 검증

- Backend: `./gradlew test` (H2 in-memory, 별도 DB 불필요)
- Frontend: `npm run lint`, `npm run typecheck`, `npm test`

## 문서

- 작업 지침: `AGENTS.md` (Claude는 `CLAUDE.md`가 이를 import)
- 용어·재사용 자산: `CONTEXT.md`
- 설계 계약: `docs/01-requirements.md` ~ `docs/06-deployment.md`
- 남은 작업: `docs/roadmap.md`
- 결정 근거·조사 기록: `docs/adr/`, `docs/notes/`
