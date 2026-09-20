# 뽐뿌 impersonator 일회성 진단

사용자가 요청한 브라우저 통신 모방 비교 실험만을 위한 독립 Gradle 프로젝트다.
기존 '차단 우회하지 않음' 정책을 운영 수집기 전체에 대해 변경하는 것은 아니다.
backend 의존성/HtmlFetcher/Compose/DB는 변경하지 않는다.

`impersonator-okhttp:1.10.2`와 `macChrome` 프로필을 고정한다. TLS 인증서와 호스트명 검증을
끄지 않는다. 외부 DoH 조회는 끄고 시스템 DNS를 사용한다. 프록시, 기존 쿠키, JavaScript,
CAPTCHA 처리, 리디렉션, 연결 실패 재시도는 사용하지 않는다. 20초 call timeout과 2MiB
압축 해제 본문 상한을 둔다. 상태 코드/링크 후보 수만 출력하며 본문은 저장하지 않는다.

## 로컬 검증 (대상 사이트 요청 없음)

```powershell
backend/gradlew.bat -p tools/impersonator-diagnostic test installDist
```

## OCI (저장소 루트에서)

이 변경이 서버에 반영된 후 아래 명령만 실행한다. 운영 Compose는 실행하지 않는다.
빌드에는 네트워크/디스크/CPU/메모리가 필요하므로 여유 자원을 먼저 확인한다.

```bash
free -h
df -h .
docker stats --no-stream
docker build -f tools/impersonator-diagnostic/Dockerfile -t pickdeal-impersonator-diagnostic:local .
docker run --rm --memory=256m --cpus=1 --read-only --tmpfs /tmp:rw,nosuid,size=32m pickdeal-impersonator-diagnostic:local
echo "exit=$?"
```

이미지는 빌드 후 남고, 컨테이너는 실행 종료 후 삭제된다. 포트/볼륨/환경변수/DB 접속은 없다.
주소/프로필 인수를 받지 않으므로 자동 탐색이나 여러 프로필 반복 시도가 불가능하다.
한 번 실행 후 결과를 확인하고 실패하면 반복하지 않는다.

- 종료 0: 게시글 링크 후보 존재. 실제 최신 목록/파서 호환/수집 허용 확인은 별개다.
- 종료 1: HTTP 거부/연결/TLS/런타임 오류. 오류 종류만 출력하며 상세 메시지는 숨긴다.
- 종료 2: 잘못된 인수, 요청하지 않음.
- 종료 3: 검증 페이지/본문 상한/게시글 링크 없음. 성공으로 간주하지 않는다.

Java 17 컴파일/실행 여부와 OCI ARM64 실접속 결과는 구분해서 판단한다.
상류 라이브러리: https://github.com/zhkl0228/impersonator
상류 저장소에는 GPL/LGPL 라이선스 파일이 있으므로 제품 통합/배포 전 적용 조건을 검토한다.
뽐뿌 접근 성공이 자동 수집 및 공개 재사용 허락을 의미하지 않는다.

## 목록 파서 개발 (2026-09-20)

`PpomppuListSnapshot`은 명시적으로 실행할 때만 고정 주소에 1회 요청하고, 2MiB 이하의
목록 후보 응답을 이 프로젝트의 `build/ppomppu-list.raw.html`에 저장한다. 기존 진단의
기본 실행은 바뀌지 않는다. 원본에는 작성자 등 불필요한 정보가 있으므로 커밋하지 않는다.
robots.txt와 접근 정책을 확인한 로컬 개발 환경에서만 실행하고, 실패하면 중단한다.

```powershell
# 저장소 루트에서 빌드 (대상 사이트에 요청하지 않음)
backend/gradlew.bat -p tools/impersonator-diagnostic test installDist
cd tools/impersonator-diagnostic
# 아래 명령만 실제 목록 요청 1회. 이미 확보한 HTML이 있다면 재실행하지 않는다.
java -Xmx128m -cp "build/install/pickdeal-impersonator-diagnostic/lib/*" com.pickdeal.diagnostic.PpomppuListSnapshot
cd ../..

# 이후는 확보된 파일만 사용: 네트워크/Spring/DB/상세 조회 없음
backend/gradlew.bat -p backend test diagnosticDist
java "-Dfile.encoding=UTF-8" -cp "backend/build/diagnostic/classes;backend/build/diagnostic/lib/*" com.pickdeal.collector.ppomppu.PpomppuListDiagnostic tools/impersonator-diagnostic/build/ppomppu-list.raw.html
```

Linux/macOS에서는 Java classpath의 `;`를 `:`로 바꾼다. HTML의 EUC-KR 인코딩은
오프라인 진단에서 meta charset으로 감지한다. Parser 자체는 `String → 목록` 순수 함수다.
테스트 fixture는 실제 목록의 일부 행에서 작성자·스크립트·이벤트 속성을 제거한 UTF-8 발췌본이다.
별도 경계조건 테스트의 합성 HTML과 실제 발췌 fixture를 구분한다.

게시판 ID가 `ppomppu`이고 표시 번호가 일치하는 행만 받으며, 인기글 중복은 외부 ID로 제거한다.
공지·다른 게시판·쇼핑뽐뿌 영역은 제외한다. 일반 게시글 자체의 상업성까지 판별하는 것은 아니다.
가격은 `(금액원/배송비)`만 인정하며 조건부 가격·외화·단위 생략은 null이다.
댓글 표시가 없으면 0으로 추측하지 않는다. 판매처는 구조화된 말머리를 그대로 가져온다.
카테고리와 게시 시각은 원문이며 상품 URL·종료 상태·DB 저장·자동 수집은 이번 범위가 아니다.

2026-09-20 로컬 Java 17 목록 요청 1회: HTTP 200, 71,546 bytes.
그 응답을 오프라인 파싱한 결과 고유 딜 21건, 가격 19건, 판매처·썸네일 각각 21건,
댓글 수 표시 6건을 확보했다. 원본은 build 경로에만 보관하며, 저장소에는 최소 발췌 fixture만 추가했다.
이 단계의 실요청은 Windows Java에서 수행했으며 Docker 재실행·OCI 접근·운영 저장 검증은 하지 않았다.

## 검증 기록 (2026-09-19)

- 로컬 Windows Java 17: `clean test installDist` 통과.
- 로컬 서버 테스트: 403/302 단일 요청, 쿠키/본문 비노출, challenge/2MiB 상한,
  링크 후보 중복 제거 및 미검증 HTML 분류 확인. 대상 사이트 요청 없음.
- 로컬 실제 요청 1회: HTTP 200, HTTP/1.1, 게시글 링크 후보 42개, 약 2.2초.
  최신 데이터와 운영 안정성을 검증한 것은 아니며 반복 요청하지 않았다.
- 로컬 Docker 엔진 미기동으로 이미지 빌드/컨테이너 실행 미검증.
- OCI ARM64 네트워크에서의 접근 및 메모리 사용량은 미검증.
