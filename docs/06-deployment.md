# 06. 배포 설계 (Deployment)

> PickDeal의 현재 로컬 환경과 최초 상시 배포 계약을 관리한다. 작업 순서와 상태는 `docs/roadmap.md`에서만 관리한다.
> 최초 작성: 2026-05-20 · 현재 상태 갱신: 2026-09-05

## 1. 기본 배포 구성

```text
사용자
  │ HTTPS
  ▼
Vercel
└─ Next.js frontend
       │ HTTPS REST
       ▼
OCI Compute
└─ reverse proxy → Spring Boot backend + scheduler
                         │ JDBC/TLS
                         ▼
                    PostgreSQL
```

- frontend는 Vercel, backend는 OCI Compute 한 대를 기본으로 한다.
- backend와 scheduler는 같은 Spring Boot 프로세스에서 실행한다. 운영 인스턴스는 하나만 둬 중복 수집을 막는다.
- frontend까지 OCI에 둘 수 있지만, 현재는 Vercel의 Next.js 빌드·배포·프리뷰 관리를 사용해 OCI 운영 범위를 줄인다.
- Redis, 별도 collector worker, 메시지 큐는 사용하지 않는다.
- 공개 backend에는 HTTPS가 필수다. reverse proxy 제품과 도메인은 구현 시 선택한다.

## 2. PostgreSQL 선택

최초 배포 전에 다음 중 하나를 선택한다. 애플리케이션의 JPA 모델과 Flyway 스키마는 어느 쪽이든 동일하게 유지한다.

### OCI 같은 서버의 PostgreSQL 컨테이너

- backend와 DB를 별도 컨테이너로 실행한다.
- DB는 Compose 내부 네트워크에서만 접근하고 `5432`를 공개하지 않는다.
- 데이터 디렉터리는 컨테이너 밖의 영속 볼륨에 연결한다.
- `pg_dump`와 볼륨 백업·복구 절차를 직접 관리한다.

추가 서비스 비용과 네트워크 지연은 작지만, 서버 장애와 DB 운영 책임이 한 서버에 모인다.

### Supabase PostgreSQL

- OCI에는 backend 컨테이너만 두고 JDBC/TLS로 Supabase에 연결한다.
- 지속 실행 backend는 direct connection을 우선하고, 네트워크가 IPv4만 지원하면 shared pooler의 session mode를 사용한다.
- Free 플랜을 쓰면 용량·egress·비활성 정지와 자동 백업 부재를 확인하고 수동 백업을 둔다.
- 운영 데이터가 중요해지면 비활성 정지와 자동 백업 정책을 기준으로 유료 전환을 판단한다.

DB 위치는 아직 확정하지 않았다. 비용을 최소화하고 DB 운영도 경험하려면 OCI 컨테이너, backend 운영에 집중하려면 Supabase가 적합하다.

## 3. 현재 로컬 환경

- backend는 로컬 PostgreSQL `pickdeal` DB를 사용한다.
- 단일 `application.yml`과 JPA `ddl-auto: update`로 기동한다.
- `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`로 로컬 접속 정보를 바꿀 수 있다.
- frontend는 `next dev`로 실행하고 `NEXT_PUBLIC_API_BASE_URL`로 backend 주소를 받는다.
- 테스트는 H2 in-memory와 `ddl-auto: create-drop`을 사용하며 수집 scheduler를 끈다.
- 기본 로컬 실행은 빈 DB에 샘플 데이터를 넣지만 `compose` 프로필에서는 Seed를 끈다.
- backend Dockerfile, backend/PostgreSQL Compose, Compose 전용 Flyway 초기화가 구현되어 있다. reverse proxy와 CI/CD는 아직 없다.

### Backend Docker 이미지

저장소 루트에서 `docker build -t pickdeal-backend:local ./backend`로 빌드한다.
`backend/`가 빌드 컨텍스트이며 `.dockerignore`는 Gradle Wrapper·빌드 설정·소스만 포함한다.
Java 17 JDK 단계에서 Wrapper로 `bootJar`를 만들고, 최종 Java 17 JRE 이미지에는 JAR만 복사한다.
Temurin 이미지는 호스트 아키텍처를 따르므로 OCI A1에서는 ARM64로 빌드된다.
다른 아키텍처에서 OCI용 이미지를 만들 때는 Buildx의 `--platform linux/arm64`를 사용한다.

실행 프로세스는 UID/GID `10001`을 사용한다. 기본 힙은 128~768MB이며
`JAVA_TOOL_OPTIONS`로 변경할 수 있다. 컨테이너 전체 메모리 제한은 힙 외 사용량을 고려해 별도로 설정한다.
이미지 빌드는 테스트를 실행하지 않으므로 배포 전에 `./gradlew test`를 수행한다.

컨테이너의 `localhost`는 DB 서버가 아니다. 실행 시 `SPRING_DATASOURCE_URL`에 실제 JDBC 주소를,
`DB_USERNAME`·`DB_PASSWORD`에 접속 정보를 주입한다. `EXPOSE 8080`은 포트 안내이며 외부 공개 설정이 아니다.
Compose는 `SPRING_PROFILES_ACTIVE=compose`로 Flyway·`validate`·Seed 비활성화를 적용한다.
프로필 없이 이미지만 실행하면 기존 로컬 설정을 사용한다. 쓰기 API 접근 제어와 HTTPS를 완료한 뒤 공개 배포한다.

### Docker Compose 실행 (공개 전 준비 환경)

루트 `compose.yml`은 backend와 PostgreSQL 17을 함께 실행한다. 기존 DB 볼륨이 다른 PostgreSQL
메이저 버전이면 그대로 연결하지 말고 논리 백업·복구 또는 정식 업그레이드 절차를 사용한다.
아래 명령은 저장소 루트의 OCI Ubuntu 터미널에서 실행한다.

```bash
cp .env.example .env
chmod 600 .env
nano .env
docker compose config --quiet
docker compose up -d --build
docker compose ps
docker compose logs --tail=100 backend
curl -f http://127.0.0.1:8080/api/v1/deals
```

`.env`의 `DB_PASSWORD`는 반드시 입력한다. 빈 값이면 Compose가 실행을 거부한다.
비밀번호에 `$` 등 특수문자가 있으면 `.env`에서 작은따옴표로 감싼다.
`docker compose config`는 비밀번호까지 출력할 수 있으므로 검증에는 `--quiet`를 사용한다.
DB 계정 환경변수는 PostgreSQL의 최초 초기화 때만 적용된다. 기존 데이터가 있는 상태에서
`.env` 비밀번호만 변경하면 DB 비밀번호는 바뀌지 않아 backend 연결이 실패한다.

- PostgreSQL은 포트를 호스트에 공개하지 않는다. backend는 Docker 서비스명 `postgres`로 접속한다.
- backend의 `8080`은 호스트 `127.0.0.1`에만 연결한다. OCI 외부/Vercel에서는 아직 접근할 수 없다.
- DB가 `pg_isready` healthcheck를 통과한 뒤 backend를 시작한다. 이는 DB 준비 확인이며
  backend 준비 완료는 로그와 API 응답으로 별도 확인한다.
- 메모리 상한은 backend 1.5GiB(힙 768MiB), PostgreSQL 1GiB다. 로그는 서비스당 10MB × 3개로 제한한다.
- `restart: unless-stopped`로 재부팅 후 자동 실행한다. DB 장애 후 backend 재연결은 애플리케이션에 맡긴다.
- scheduler는 기본적으로 꺼져 있다. 단일 수집 서버임을 확인하고 `.env`에
  `COLLECTOR_ENABLED=true`를 지정한 뒤 `docker compose up -d`로 반영한다.
- 새 DB에는 Flyway V1이 스키마만 만든다. 샘플 Seed를 끄므로 수집 활성화 전에는 빈 목록이 정상이다.
- Hibernate는 `validate`만 수행하며 SQL debug 로그는 끈다. 쓰기 HTTP 요청은 조회 전용 필터로 차단한다. HTTPS는 호스트의 Caddy 등에서 구성하고 운영 CORS는 아래 환경변수로 지정한다.

### Flyway 적용 방식

Flyway는 backend 안에서 동작하는 라이브러리이므로 별도 컨테이너나 서버 설치가 필요 없다.
첫 기동 때 `db/migration/V1__initial_schema.sql`을 실행하고 `flyway_schema_history`에 버전·체크섬을 기록한다.
다음 기동부터는 적용된 파일을 검증하고 새 버전만 실행한다. 이후 Hibernate가 엔티티와 테이블을 검증한다.
앞으로 스키마를 변경할 때는 엔티티와 함께 `V2__...sql` 등 새 파일을 추가한다.
이미 적용된 V1을 수정하거나 이력 테이블을 지워서 우회하지 않는다.

기본 로컬 PostgreSQL과 기존 H2 테스트는 Flyway 비활성 상태를 유지한다.
기존 테이블이 있는 DB에 `compose` 프로필을 적용하면 baseline 없이 실패하도록 설정했다.
기존 DB 전환은 백업과 스키마 비교 후 별도로 진행하며 `baseline-on-migrate`를 자동으로 켜지 않는다.
Flyway는 백업이나 실패한 변경의 자동 되돌리기를 대신하지 않는다.

적용 상태는 다음 읽기 전용 명령으로 확인한다.

```bash
docker compose exec postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT version, description, success FROM flyway_schema_history;"'
```

`ComposeMigrationTest`는 새 DB 초기화·Hibernate 검증·Seed 미실행·중복/FK 제약·재적용 시 데이터 보존을 검증한다.
기본 실행은 H2이며 PostgreSQL 실검증 시 `MIGRATION_TEST_DB_URL`에 일회용 테스트 DB 주소를 지정한다.
이 테스트는 `migration_test` 사용자와 빈 비밀번호를 사용하므로 기존 개발/운영 DB를 지정하지 않는다.

중지는 `docker compose stop`, 다시 시작은 `docker compose start`를 사용한다.
`docker compose down`은 컨테이너·네트워크만 제거하고 `pickdeal_postgres_data` 볼륨은 유지한다.
**`docker compose down -v`는 DB 데이터도 삭제하므로 사용하지 않는다.** 볼륨은 백업을 대신하지 않는다.
데이터 유지 검증은 동일 DB의 행 수를 확인한 뒤 `down` → `up -d` 후 다시 비교한다.

## 4. 운영 설정 계약

배포 구현에서 다음 설정을 환경변수로 주입한다. 실제 이름은 코드와 함께 확정하며 비밀값은 저장소에 커밋하지 않는다.

| 영역 | 설정 | 목적 |
| --- | --- | --- |
| frontend | backend API base URL | 브라우저와 SSR 요청 대상 |
| backend | active profile | local/prod 설정 분리 |
| backend | JDBC URL·계정·비밀번호 | PostgreSQL 연결 |
| backend | 허용 frontend origin | 실제 Vercel·운영 도메인 CORS |
| backend | collector enabled·출처별 limit | 수집 활성화와 요청 상한 |
| 접근 제어 | 배포 방식에 따른 인증값 | 개인 설정·내부 쓰기 API 보호 |

운영 프로필은 다음 조건을 만족한다.

- 샘플 Deal과 키워드 Seed를 실행하지 않는다.
- Flyway가 기준 스키마를 적용하고 JPA는 `ddl-auto: validate`를 사용한다.
- SQL debug 로그를 끈다.
- 기본 DB 비밀번호에 의존하지 않는다.
- 수집기 로그에서 출처, 성공·실패, 신규 저장 수와 필드 확보율을 확인할 수 있다.

## 5. 접근 범위와 네트워크

### 공개 조회 모드

- `compose` 프로필은 `pickdeal.read-only=true`가 기본이다. 공개 쓰기는 `403 / READ_ONLY`로 거부한다. 키워드·출처 변경 및 기존 내부 Deal 등록도 포함한다. 명시적으로 활성화하고 토큰 인증된 원격 수집 전용 POST 두 개만 예외다(`docs/03` §8). 앱 내부 수집기의 DB 저장에는 영향이 없다.
- 기본 로컬 backend 실행에서는 필터가 비활성화되어 기존 설정 기능을 유지한다. 운영에서 `PICKDEAL_READ_ONLY=false`로 해제하지 않는다.
- frontend는 production 빌드에서 조회 전용이 기본이다. Vercel에는 `NEXT_PUBLIC_READ_ONLY=true`를 명시하고 재배포한다. 키워드 메뉴·데스크톱 출처 설정·모바일 출처 drawer를 숨기고, `/settings/keywords` 직접 접근은 404로 처리한다. 데모 UI는 변경하지 않는다.
- 로컬 `npm run dev`는 기존 UI를 유지한다. 로컬 production 빌드로 설정 화면을 검증할 때만 `NEXT_PUBLIC_READ_ONLY=false`를 지정한다. UI 숨김은 보안 경계가 아니며 backend 차단이 실제 보호다.
- 조회 전용 모드만으로 Vercel 연결이 완료되지는 않는다. HTTPS 구성과 아래 운영 CORS·Vercel 환경변수 설정이 필요하다.

### Vercel CORS 및 API 주소 연결

원격 수집 API를 사용하려면 별도의 아래 설정을 확인한다. 프론트 CORS 허용과 수집기 인증은 독립적이다.

OCI 저장소 루트의 `.env`에 실제 frontend origin을 지정한다. 경로나 끝의 `/`는 붙이지 않는다. 여러 주소는 쉼표로 구분하고 Preview 주소는 필요한 정확한 주소만 추가한다(`*.vercel.app` 전체 허용 금지).

```dotenv
CORS_ALLOWED_ORIGINS=https://pick-deal.vercel.app
```

Compose가 이를 `PICKDEAL_CORS_ALLOWED_ORIGINS`로 backend에 전달하고 `CorsConfig`가 읽는다. Compose에서는 localhost 자동 허용을 끄며, 값이 비어 있으면 교차 origin 브라우저 요청을 허용하지 않는다. Origin 없는 SSR·curl 요청은 별개다. 기본 로컬 개발은 기존 localhost 가변 포트 허용을 유지한다. 쿠키 인증은 사용하지 않으므로 credentials 허용은 켜지 않는다. CORS는 인증이나 쓰기 API 보호를 대신하지 않는다.

머지된 코드를 가져오고 `.env`를 수정한 후 backend를 재빌드한다. DB 볼륨을 삭제하지 않는다.

```bash
git pull --ff-only origin main
docker compose up -d --build backend
```

Vercel Production 환경변수를 설정하고 재배포한다.

```dotenv
NEXT_PUBLIC_API_BASE_URL=https://pickdeal.dedyn.io
NEXT_PUBLIC_READ_ONLY=true
```

API base URL에 `/api/v1`을 붙이지 않는다. 아래 preflight 응답이 성공하고 `Access-Control-Allow-Origin: https://pick-deal.vercel.app`이 포함되는지 확인한다.

```bash
curl -i -X OPTIONS 'https://pickdeal.dedyn.io/api/v1/deals' \
  -H 'Origin: https://pick-deal.vercel.app' \
  -H 'Access-Control-Request-Method: GET' \
  -H 'Access-Control-Request-Headers: content-type'
```

첫 목록은 SSR이므로 화면 표시만으로 CORS 성공을 판단하지 않는다. 브라우저 Network에서 더 보기 요청도 확인한다.

현재는 인증 없이 고정 `user_id = 1`을 사용한다. 다음 API를 공개하면 다른 방문자가 동일한 개인 설정이나 데이터를 변경할 수 있다.

- 출처 표시/숨김
- 키워드 등록·삭제
- 내부 Deal 등록

`/internal`이라는 경로명은 보안 경계가 아니다. 최초 배포는 위 조회 전용 모드로 쓰기 요청을 차단한다. 클라이언트 번들에 비밀 헤더를 넣는 방식은 사용하지 않는다.

OCI와 호스트 방화벽에서는 필요한 공개 포트만 연다.

- `80/443`: reverse proxy
- `22`: 관리용 SSH, 가능한 한 접근 IP 제한
- backend 내부 포트와 PostgreSQL `5432`: 공개하지 않음

### 원격 수집 수신 설정 (기본 OFF)

수신 API와 로컬 전송 모드가 구현돼 있으며, 운영 연결 검증 전에는 운영에서 켜지 않는다.
Compose에는 `COLLECTOR_INGRESS_ENABLED=false`, `COLLECTOR_INGRESS_TOKEN=`을 기본값으로 전달한다.
추후 연결 시 backend `.env`에 활성 여부와 무작위 토큰을 설정하고 backend를 재생성한다.
토큰은 32~256자의 영문·숫자·`_`·`-`만 허용하며 활성인데 비었거나 짧으면 기동에 실패한다.
길이 검증은 무작위성 검증이 아니므로 사람이 만든 문구 대신 안전한 난수로 생성한다.

- `COLLECTOR_INGRESS_ENABLED` → `PICKDEAL_COLLECTOR_INGRESS_ENABLED`
- `COLLECTOR_INGRESS_TOKEN` → `PICKDEAL_COLLECTOR_INGRESS_TOKEN`
- 로컬 프로필 실행은 `PICKDEAL_COLLECTOR_INGRESS_ENABLED=true`로 명시하며 토큰은 환경변수로 전달한다.
- 원격 수집기는 HTTPS와 `Authorization: Bearer ...`를 사용한다. 로그·명령 기록·Git에 실제 토큰을 남기지 않는다.
- 토큰 교체 시 서버와 수집기의 환경변수를 함께 바꾼다. API를 끄려면 ingress enabled만 false로 되돌린다.
- `PICKDEAL_READ_ONLY=false`로 풀지 않는다. 올바른 수집 토큰도 설정·수동 등록 API를 열지 않는다.
- Vercel에는 토큰을 설정하지 않는다. DB 포트/권한도 외부에 추가로 열지 않는다.
- 전송 모드 적용 후 같은 출처는 한 곳에서만 수집한다. OCI 수집을 모두 옮긴다면 `COLLECTOR_ENABLED=false`를 유지한다.
- 본문 제한은 1MiB이고 한 요청 최대 150건이다. 인증 실패를 반복 재시도하지 않는다.
- 운영 배포 전에는 테스트용 DB에서 전송/재전송/토큰 오류/공개 쓰기 차단을 확인한다.

### 로컬 수집기 실행 (노트북·홈 서버)

별도 저장소 없이 같은 프로젝트를 사용한다. 서버용 `compose.yml`과 로컬용
`compose.collector.yml`은 서로 합치지 않고 따로 실행한다. 로컬용에는 DB 서비스·공개 포트·DB 환경변수가 없다.
수집기 전용 Java 실행점은 Spring을 시작하지 않으며, 파서와 DTO는 backend 코드를 공유한다.
같은 JAR를 프로필로 전환하는 방식은 아니다. 서버 bootJar와 로컬 배포 디렉터리를 같은 Gradle 빌드에서 만든다.

1. 노트북에서 `.env.collector.example`을 `.env.collector`로 복사한다.
2. `COLLECTOR_RECEIVER_URL`에는 backend의 HTTPS origin만 설정한다(경로 없이).
3. `COLLECTOR_INGRESS_TOKEN`에는 수신 서버와 같은 토큰을 넣고 `COLLECTOR_SOURCES`에 명시적으로 출처를 선택한다.
4. 처음에는 `COLLECTOR_RUN_ONCE=true`, `COLLECTOR_BOOTSTRAP_MAX_PAGES=1`, `COLLECTOR_MAX_DETAIL_REQUESTS=0`으로 검증한다.

```powershell
# 이미지 빌드만으로는 사이트/API 요청이나 서비스 기동이 일어나지 않는다.
docker compose --env-file .env.collector -f compose.collector.yml build
# 아래부터 실제 수집 및 수신 서버 DB 저장이 발생한다. 운영 실행 전 대상 URL을 확인한다.
docker compose --env-file .env.collector -f compose.collector.yml run --rm collector
```

한 번 검증한 후 필요할 때만 상세 상한(최대 3)을 늘리고 `COLLECTOR_RUN_ONCE=false`로 20분 주기를 사용한다.
연속 실행은 같은 Compose에 `up -d`를 사용하고 중지는 `stop`으로 한다.
설정 오류·인증 실패를 무한 반복하지 않도록 Docker restart는 `no`다. PC/Docker 재시작 후에는 수동으로 다시 시작한다.
절전/전원 종료 동안 수집은 멈추며, 중단 중 지나간 게시글 및 메모리에 남았던 미전송 배치는 유실될 수 있다.
성공 응답을 받지 못한 배치는 다음 주기에 먼저 다시 전송하고 서버의 중복 처리로 행 중복을 막는다.
프로세스를 종료한 뒤 같은 목록을 다시 수집하는 것은 재전송 큐 복구와 다르다.

수신 API는 HTTP 4xx(429 제외)나 redirect에 대해 즉시 종료하며 토큰을 다른 주소로 따라 보내지 않는다.
수신 URL의 평문 HTTP는 같은 기기 테스트의 `localhost`/`127.0.0.1`/`::1`에만 허용한다.
컨테이너의 localhost는 컨테이너 자신이므로 호스트 테스트 서버 주소로 착각하지 않는다.
`host.docker.internal`에 평문 HTTP를 열기 위해 운영 보안 제한을 풀지 않는다.

컨테이너는 비-root·read-only·512MiB/1CPU 제한으로 실행한다. 이는 설정값이며 실제 기기 자원 사용량을 측정한 값은 아니다.
라즈베리파이/OCI ARM64 등 다른 아키텍처에서의 이미지 실행과 사이트 접근은 별도 확인이 필요하다.
수집기에는 `impersonator-okhttp:1.10.2`를 포함한다. 상류 GPL/LGPL 조건은 기존 독립 진단과 같이
재배포 전에 확인한다. 해당 의존성은 서버 bootJar에 추가하지 않는다.

외부 사이트 요청 없는 로컬 테스트:

```powershell
cd backend
./gradlew.bat test collectorDist bootJar
```

`RemoteCollectionHttpTest`는 fixture → 실제 로컬 HTTP 수신 API → 격리된 H2 테스트 DB 저장/재전송을 검증한다.
운영 PostgreSQL·Docker 종단 간 검증을 대체하지 않는다. 실사이트 테스트는 기본 skip이며,
별도 승인 아래 `PICKDEAL_LIVE_COLLECTOR_TEST=true`를 설정하고
`--tests "com.pickdeal.collector.remote.RemoteCollectionHttpTest.singleLivePpomppuListToIsolatedTestReceiver"`만
선택하면 뽐뿌 목록 1회·상세 0회·격리된 H2 저장으로 확인할 수 있다. 반복 실행하지 않는다.

2026-09-20 검증: 로컬 Java 17의 fixture/HTTP 테스트와 실제 뽐뿌 목록 1회 → 로컬 수신 API →
격리된 H2 저장이 통과했다(21건, 상세 0회). 서버 bootJar에 전용 실행점/impersonator가 없음을 확인했다.
이어 같은 날 로컬 Docker Linux/amd64에서 아래 fixture 기반 PostgreSQL 종단 간 검증도 통과했다.
OCI 전송, 실제 HTTPS 수신 연결, ARM64 실행은 아직 검증하지 않았다.

### 격리된 Docker + PostgreSQL 검증

Docker Desktop을 켠 뒤 저장소 루트에서 PowerShell 7로 실행한다.

```powershell
pwsh -NoProfile -File scripts/verify-local-collector.ps1
```

- 매 실행 고유한 `pickdeal-collector-verify-*` 프로젝트와 무작위 테스트 비밀번호/토큰을 생성한다.
  실제 `.env`·운영 토큰은 수정하거나 출력하지 않는다.
- `compose.collector-test.yml`의 PostgreSQL 17은 tmpfs만 사용한다. 기존 볼륨을 연결하지 않으며
  backend는 compose 프로필의 Flyway + Hibernate validate로 기동한다.
- 호스트에 포트를 공개하지 않고 내부 Docker 네트워크만 사용한다. 수집기 컨테이너는 backend의
  네트워크 공간을 공유하여 loopback HTTP로 테스트하므로 운영 HTTPS 제한을 완화하지 않는다.
- 실제 수집기 이미지에서 검증 전용 실행점 `LocalCollectorVerification`을 호출한다.
  기존 뽐뿌 fixture → RemoteCollectionRunner → API → PostgreSQL을 사용하며 외부 사이트 요청은 0회다.
  수집기 자체는 DB 연결·Spring 웹 서버를 시작하지 않는다.
- 확인 항목: 신규 4건 저장, 같은 4건 재전송 후 중복 없음, 기존 종료 상태 보존,
  토큰 누락/불일치 401, 공개 쓰기 403, 공개 조회 200, Flyway 성공 이력.
- 성공/실패와 관계없이 해당 실행에서 생성한 테스트 컨테이너·네트워크를 정리한다.
  tmpfs DB도 사라진다. 빌드한 테스트 이미지와 Docker 빌드 캐시는 남겨 둔다.
- 2026-09-20 로컬 실행 결과 위 항목 모두 통과했다. 이 검증은 실제 출처 통신과 OCI 연결 검증을 대신하지 않는다.

## 6. 배포와 검증

### 로컬 배포 형태 확인

1. 운영과 같은 이미지·환경변수로 backend를 기동한다.
2. Flyway 적용과 JPA 검증을 확인한다.
3. 재기동 후 데이터 유지 여부를 확인한다.
4. health check와 API를 호출한다.

### OCI 최초 배포

1. 네트워크와 HTTPS 진입점을 구성한다.
2. backend 이미지와 운영 환경변수를 배포한다.
3. 선택한 PostgreSQL 연결과 백업을 확인한다.
4. Vercel production·preview 환경에 맞는 API 주소와 CORS를 설정한다.
5. 브라우저에서 목록·상세·키워드·출처 설정을 확인한다.
6. OCI 네트워크에서 출처별 수집을 한 번 실행해 파싱·upsert와 요청 상한을 확인한다.

SSR 성공만으로 통합 검증을 끝내지 않는다. 출처 토글과 더 보기처럼 브라우저가 직접 수행하는 요청도 실제 도메인에서 확인한다.

## 7. 백업과 복구

- OCI DB 컨테이너: 정기 `pg_dump`, 영속 볼륨 백업, 복구 명령 확인
- Supabase Free: 수동 논리 백업과 복구 확인
- 자동 백업을 제공하는 관리형 플랜: 보존 기간과 실제 복구 절차 확인

백업 생성 성공만 보지 않고 빈 테스트 DB에 복구할 수 있는지 한 번 확인한다.

## 8. 지금 만들지 않는 것

- Kubernetes와 다중 인스턴스
- Redis와 별도 worker
- 무중단 배포·고가용성 구성
- 본격적인 모니터링 스택
- 복잡한 CI/CD와 이미지 레지스트리 자동화

운영에서 반복 배포나 장애 대응 비용이 확인되면 필요한 항목부터 추가한다.

## 9. 관련 문서

- 다음 작업과 완료 조건: `docs/roadmap.md`
- DB 스키마와 Flyway 방향: `docs/04-database-design.md`
- 수집 설정과 실수집 확인: `docs/05-collector-design.md`
- 검토 근거: `docs/notes/2026-09-05-pickdeal-review.md`
