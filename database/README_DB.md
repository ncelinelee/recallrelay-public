# RecallRelay 로컬 데이터베이스

공개 스냅샷에는 고객관리 mock 자료만 포함합니다. 공유 DB 비밀번호, 유효한 직원 비밀번호 해시, 사전 발급 관리자 코드는 포함하지 않습니다. seed 직원은 모두 비활성 상태입니다.

## DB 실행

이 폴더에서 `.env.example`을 `.env.local`로 복사하고 DB 사용자와 root에 서로 다른 비밀번호를 직접 설정합니다. `.env.local`은 Git에서 제외됩니다. 기존 운영 비밀번호를 사용하지 마세요.

```sh
cp .env.example .env.local
# .env.local의 빈 값을 로컬에서 채운 뒤 실행
docker compose --env-file .env.local up -d
```

MariaDB는 `127.0.0.1:3306`에서만 노출되며 DB와 사용자는 `recallrelay`입니다. 초기 빈 볼륨에서 `schema.sql`, `seed.sql`이 실행됩니다. 기존 볼륨은 이 설정 변경으로 비밀번호나 직원 상태가 자동 변경되지 않습니다.

백엔드에는 같은 `DB_PASSWORD`와 별도의 `RECALLRELAY_JWT_SECRET` 환경변수를 설정합니다. AI 기능은 서버 `OPENAI_API_KEY`를 사용합니다.

## 실제 API 로그인용 로컬 계정

화면 mock 모드는 계정 설정 없이 확인할 수 있습니다. 실제 API 로그인을 검증하려면 로컬에서 새 비밀번호의 BCrypt 해시를 생성한 뒤 `local-employee-setup.sql.example`을 `local-employee-setup.sql`로 복사해 해시를 넣고 해당 로컬 DB에서만 실행합니다. 이 파일도 Git에서 제외됩니다. 비밀번호나 해시를 공개 예시 파일에 저장하면 안 됩니다.

BCrypt 해시는 신뢰할 수 있는 로컬 도구로 생성합니다. 예를 들어 Python의 `bcrypt` 패키지에서 숨김 입력을 사용할 수 있습니다.

```sh
python3 -c 'import bcrypt,getpass; print(bcrypt.hashpw(getpass.getpass("Local password: ").encode(),bcrypt.gensalt()).decode())'
```

선택한 `jlee` 계정만 활성화되며 첫 로그인 후 비밀번호 변경이 필요합니다. 공개 seed에는 작동하는 관리자 활성화 코드가 없습니다. 계정·코드는 별도 로컬 설정으로 발급해야 합니다.

## 구조

조직, 딜러사, 직원, 고객, 차량, 리콜 캠페인과 대상, 케이스, 부품, 예약, 활동 기록, 등록 요청과 비밀번호 재설정 토큰을 관계형 구조로 관리합니다. [스키마](schema.sql), [API 매핑](API_DB_MAPPING.md), [마이그레이션](migrations)을 참고하세요. 실제 고객 데이터나 운영 DB에 데모 초기화 SQL을 실행하지 마세요.
