# RecallRelay · 리콜 업무 의사결정 보조

> AIGEN 지원용 공개 스냅샷입니다. 기존 RecallRelay의 현재 코드와 AI 실험 자료를 정리했으며, 원본 저장소의 과거 커밋 기록은 포함하지 않습니다. 고객관리 자료는 mock 데이터입니다.

고객에게 한 약속, 부품 입고 예정일, 정비 예약이 어긋나는 상황을 발견하고 담당자가 확인할 근거를 제공하는 서비스입니다. Vue 3 + Spring Boot + MariaDB에 LLM 분석을 연결했습니다. **AI는 기록의 추출·설명·확인 질문을 보조하고, 공급 위험과 다음 행동의 핵심 판단은 코드 규칙으로 유지합니다.**

## 포트폴리오에서 볼 부분

| 근거 | 구현 |
| --- | --- |
| [AI Lab](ai-lab/README.md) | 프롬프트 버전 비교, 원문 응답, 자동 재검산과 평가 한계 |
| [AI 서비스](backend/src/main/java/com/recallrelay/ai) | 약속 추출, 과도한 약속, 변경 영향, 다음 확인 질문, 모순 탐지 |
| [접근 제어](backend/src/main/java/com/recallrelay/casework/CaseAccessGuard.java) | DB의 현재 직원 상태·조직·딜러사 범위 확인 후 데이터/AI 접근 |
| [DB 설계](database/README_DB.md) | 업무 데이터와 직원 계정 수명주기 |
| [화면](src/views) | 업무 대시보드, 케이스 상세, 다국어 UI |

개인별 기여 범위는 커밋과 코드로 확인할 수 있는 수준만 제시합니다. 팀 전체 기능을 개인 단독 성과로 주장하지 않습니다.

고객관리 예시는 mock 데이터입니다. 실제 API 키 보관과 공개 데모 자격증명의 사용 범위는 [공개 저장소 안내](docs/publication-security.md)를 확인하세요.

## 빠른 실행: 화면 프로토타입

Node.js 22 이상에서 루트의 다음 명령을 실행합니다.

```sh
npm ci
cp .env.example .env.local
npm run dev
```

`VITE_USE_MOCK=true`이면 화면의 데모 데이터로 동작합니다. 화면 미리보기는 조직 `OHC-001-001`, 직원 `jlee`로 열 수 있으며 비밀번호를 입력하지 않아도 됩니다. 실제 백엔드 계정은 초기 비활성 상태이며 로컬에서 따로 설정해야 합니다. 미리보기 역할 선택은 백엔드 권한 검증이 아닙니다.

## 실제 백엔드 연결

Java 17, Maven, Docker/MariaDB가 필요합니다. [DB 준비](database/README_DB.md), [백엔드 설정](backend/README_BACKEND.md), [전체 실행](RUN_FULLSTACK.md)을 순서대로 확인하세요. 환경은 `VITE_USE_MOCK=false`, `VITE_API_BASE_URL=http://localhost:8080`으로 설정합니다. JWT 서명키는 `RECALLRELAY_JWT_SECRET` 환경 변수에 별도 32바이트 이상의 난수로 지정해야 합니다. AI 호출은 서버의 `OPENAI_API_KEY`를 사용합니다. 프런트엔드 VITE 변수에 키를 넣으면 안 됩니다.

실제 API와 목 화면이 함께 남아 있는 프로토타입이며 모든 화면이 DB 기반 운영 서비스인 것은 아닙니다. 메일·비밀번호 재설정·외부 LLM까지의 통합에는 별도 환경 설정이 필요합니다.

## 검증

```sh
npm run build
(cd backend && mvn test)
python -m unittest discover -s ai-lab/tests -p 'test_*.py' -v
python ai-lab/evaluate.py --output ai-lab/results/offline_recheck.json
```

CI는 프런트엔드 빌드, 백엔드 테스트와 오프라인 평가를 실행합니다. 권한 테스트는 인메모리 H2에서 조직 간 접근, 딜러사 간 접근, 비활성·삭제 직원, 오래된 JWT 역할, 익명 접근을 확인합니다. 케이스 조회와 5개 AI 기능이 같은 검사를 거치며 범위를 벗어나면 고객 데이터 조회 전에 차단합니다.

## AI 평가 해석과 한계

기존 v3의 `100%`는 **개발 예시 3건의 과거 수기 판정**입니다. 일반화 성능·실제 업무 효과·독립 테스트 정확도를 입증하지 않습니다. [오프라인 재검산](ai-lab/results/offline_recheck.json)은 저장된 9개 응답의 JSON·스키마·원문 근거만 검사하며 API를 새로 실행하지 않습니다. 새 응답에 옛 수기 점수를 붙이지 않도록 응답 해시 검증을 추가했습니다. 미사용 평가 집합, 반복 실행과 독립 검수는 후속 과제입니다.

실제 고객 데이터를 넣는 운영 배포 전에는 전체 업무 API의 권한 범위, 로그·보관·개인정보 처리, 외부 AI 전송 정책, 취약점과 통합 테스트를 추가 검증해야 합니다. 이번 케이스/AI 조회 보강을 전체 시스템 보안 감사 완료로 해석하지 않습니다. 과거 배포 안내와 고정 데모 자격증명은 이 공개 스냅샷에 포함하지 않습니다.
