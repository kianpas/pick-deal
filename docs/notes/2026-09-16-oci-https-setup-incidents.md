# OCI 배포와 HTTPS 구축 기록 (2026-09-08 ~ 09-16)

> 작성: 2026-10-21. 당시 다른 대화에서 진행한 구축 기록을 정리해 옮겼다. 각 항목은 당시 사용자 출력이나
> 대화 기록에 근거하며, 현재 서버 상태를 다시 확인한 결과가 아니다. 현재 구성과 절차는 `docs/06-deployment.md`를 따른다.
> 공인 IP·토큰·OCI 리소스 식별자 등은 의도적으로 남기지 않는다.

## 연표

| 날짜(KST) | 작업 |
| --- | --- |
| 09-08 | 일반 Compute VM + Docker Compose 방향 결정, Ubuntu·ARM64·포트 역할 정리 |
| 09-10 | 춘천 리전 A1 VM 생성. 1 OCPU / 6GB로 시도했으나 A1 호스트 용량 부족으로 4GB로 생성. SSH·Docker 설치 |
| 09-13~14 | Dockerfile·Compose·Flyway·운영 프로필 준비, OCI에서 backend·DB 기동과 Flyway V1·API 응답 확인 |
| 09-14 | 공개 조회 전용 처리. HTTPS용 무료 hostname으로 deSEC 생성, DNSSEC 조회 문제 진단 |
| 09-16 | 방화벽 수정 후 Caddy 정식 인증서 발급, 외부 HTTPS API 200 확인. 운영 CORS 환경변수화(PR #41) |
| 이후 | 메모리를 6GB로 증설(2026-10-21 사용자 확인) |

## 왜 무료 DNS hostname과 Caddy인가

Vercel 프론트는 HTTPS인데 브라우저가 backend를 직접 호출하는 기능(더 보기 등)이 있다. HTTP backend를 호출하면
브라우저가 Mixed Content로 차단하므로 backend에도 신뢰할 수 있는 HTTPS 진입점이 필요했다. 유료 도메인 구매를 미루고
deSEC의 무료 `dedyn.io` hostname을 사용했다. DuckDNS 등과 비교해 deSEC을 고른 별도 근거 기록은 없다.

Caddy는 인증서 발급·갱신과 HTTPS 전환을 자동으로 처리해 별도 Certbot 구성 없이 reverse proxy를 만들 수 있다.

## 장애 1: DNS가 SERVFAIL (09-14)

**현상.** deSEC 업데이트 API는 `good`을 반환했지만 일반 A·AAAA 조회가 `SERVFAIL`이었다. 오류에 `DNSSEC Bogus`,
`failed to verify an insecure referral proof`가 나왔고 Google DNS에서도 같았다. `+cdflag`로 DNSSEC 검증만 끄면 A 레코드가 보였다.

**판단.** 레코드는 존재하고 DNSSEC 검증이 실패한 것이다. Docker·DB·API 포트 문제로 보지 않았고, 도메인 삭제·재생성이나
방화벽 변경부터 하지 않고 검증 결과를 비교했다.

**결과.** 이후 정상 조회와 HTTPS까지 확인했지만 DNSSEC 문제가 정확히 무엇으로 해소됐는지는 기록에서 확정하지 못했다.
`+cdflag`는 진단용이었으며 DNSSEC를 끄는 운영 방침을 세우지 않았다.

**교훈.** "업데이트 성공"과 "조회 성공"은 다르다. 레코드 부재와 검증 실패를 먼저 구분한다.

## 장애 2: 외부에서 HTTPS 접속 불가 (09-16)

**현상.** 서버 안에서는 API가 응답하지만 외부 접속이 되지 않았다.

**확인 순서와 원인.**

1. OCI Security List에 TCP 80·443(전체 출발지, stateful)이 이미 올바르게 있었다.
2. Caddy가 80·443에서 대기 중이었다.
3. **Ubuntu 호스트의 INPUT 규칙이 SSH만 허용한 뒤 나머지를 REJECT했다.** ← 원인

**조치.** REJECT보다 앞에 80·443 허용을 삽입하고 저장했다.

```bash
sudo iptables -I INPUT 1 -p tcp --dport 80 -m conntrack --ctstate NEW -j ACCEPT
sudo iptables -I INPUT 1 -p tcp --dport 443 -m conntrack --ctstate NEW -j ACCEPT
sudo netfilter-persistent save
```

`-A`로 뒤에 붙이면 앞의 REJECT에 먼저 걸리므로 `-I`로 삽입했다. 기존 SSH·Docker 규칙은 건드리지 않았다.
위 명령은 당시 이력이며 현재 규칙을 확인하지 않고 반복 실행하지 않는다.

**이후.** 연결이 TLS 단계까지 진행된 뒤 처음에는 TLS 오류가 났고, Caddy 자동 재시도 후 Let’s Encrypt 정식 인증서가
발급됐다. 외부 HTTPS API에서 `HTTP/2 200`과 JSON 응답을 확인했고, 저장 파일(`/etc/iptables/rules.v4`)에 규칙이 있는 것도 확인했다.

**교훈.** 클라우드 보안 규칙과 호스트 방화벽은 별개다. 안쪽부터(프로세스 → 호스트 방화벽 → 클라우드 규칙) 차례로 좁힌다.

## 장애 3: CORS 설정값에 Markdown 링크 (09-16 무렵)

**현상.** 운영 CORS 허용 origin이 동작하지 않았다.

**원인.** `.env`에 주소 대신 채팅의 Markdown 링크 전체를 붙여 넣었다.

```dotenv
# 잘못된 예
CORS_ALLOWED_ORIGINS=[https://pick-deal.vercel.app](https://pick-deal.vercel.app)
# 올바른 예
CORS_ALLOWED_ORIGINS=https://pick-deal.vercel.app
```

**교훈.** 문서·채팅의 링크를 설정에 옮길 때는 코드 블록의 문자열만 복사한다. 첫 목록은 SSR로 받아 CORS가 틀려도
화면 일부가 보이므로, 브라우저가 직접 호출하는 요청(더 보기 등)으로 확인한다.

## 참고: 루트 경로 500은 장애가 아니었다

당시 `/`는 500, `/api/v1/deals`는 200이었다. 루트는 API가 아니므로 Caddy·인증서 장애로 판단하지 않았다.
없는 경로가 500으로 응답하던 애플리케이션 문제는 이후 404로 수정했다(PR #82).
