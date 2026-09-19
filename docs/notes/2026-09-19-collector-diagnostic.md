# 뽐뿌 일회성 접근 진단

curl과 실제 수집기의 결과가 다를 수 있어 기존 `HtmlFetcher`를 그대로 호출한다.
Spring 컨텍스트, DB, 스케줄러를 시작하지 않고 고정된 뽐뿌 목록 URL만 한 번 fetch한다.
재시도·상세 조회·쿠키 가져오기·프록시·검증 우회는 없다. Jsoup의 기존 리디렉션 처리는 유지하므로
한 번의 fetch가 리디렉션에 따른 여러 HTTP 요청을 포함할 수 있다.

## OCI 실행

머지 후 저장소 루트에서 실행한다. `git status --short`로 기존 수정부터 확인한다.
운영 서버의 Compose에 루리웹 비활성화 설정을 수동 추가했다면 덮어쓰지 않는다.

```bash
cd ~/pick-deal
git status --short
git pull --ff-only origin main
docker build -f backend/Dockerfile.diagnostic -t pickdeal-diagnostic:local backend
docker run --rm --memory=256m --cpus=1 --read-only --tmpfs /tmp:rw,nosuid,size=32m pickdeal-diagnostic:local
echo "exit=$?"
```

빌드에는 다운로드·CPU·메모리가 필요하다. 기존 backend/postgres는 재생성하지 않는다.
Compose나 `.env`를 사용하지 않으며 포트·DB 볼륨·Docker 소켓을 연결하지 않는다.
Java 17/JRE 이미지와 프로젝트에서 관리하는 Jsoup 버전을 사용하지만, 운영 이미지의
커밋/이미지 버전·JVM 옵션·네트워크 조건이 다르면 완전히 동일한 환경은 아니다.
별도 수집기가 아니므로 운영 수집 스케줄에 추가되지 않는다.

## 해석

- 종료 0: HTML에 뽐뿌 게시글 링크 후보가 있음. 최신 목록·파싱·수집 허용 여부를 보장하지 않는다.
- 종료 1: 요청 실패. `httpStatus=403`이면 멈추고 반복 요청하지 않는다.
- 종료 2: 잘못된 인수. 인수 없이 실행한다.
- 종료 3: HTTP fetch는 성공했지만 게시글 링크가 없음. 보안 페이지/마크업 변경 가능성.
- 그 외: 컨테이너/JVM 등 실행 환경 오류를 확인한다.

본문·쿠키·게시글 제목은 출력/저장하지 않는다. 출력된 요약만 공유한다.
실행 성공이 공개 서비스의 재사용 허락을 뜻하지 않는다. 뽐뿌 이용약관 제13조의
사전 승낙 없는 영리 목적 이용/제3자 이용 제한 등은 도입 전에 별도 확인한다.
이 도구는 접근 가능성 진단이며 출처 추가나 운영 배포가 아니다.
