# envfile

`.env` / `.env.json` 파일을 읽어  
Gradle 실행 태스크(`bootRun`, `test`, `JavaExec`)에 **환경 변수를 자동 주입**하는 Gradle 플러그인입니다.

로컬 개발 및 테스트 환경에서 OS 환경 변수를 직접 설정하지 않고도 Spring Boot 애플리케이션을 실행할 수 있습니다.

---

## Plugin

| Plugin | Plugin ID | Description |
|--------|-----------|-------------|
| envfile-spring | `io.github.jaehyunup.envfile-spring` | Spring Boot / JVM 실행 태스크에 env 주입 |

---

## Getting Started

```kotlin
plugins {
    id("io.github.jaehyunup.envfile-spring") version "0.0.1"
}
```

> 로컬에서 테스트할 경우 `settings.gradle(.kts)`에 `mavenLocal()`이 필요합니다.

---

## Env File Resolution

루트 프로젝트 기준으로 아래 순서로 `.env*` 파일을 자동 탐색합니다.

```text
.env.local
.env
.env.local.json
.env.json
```

- 가장 먼저 발견된 파일 **하나만** 사용
- 파일이 없으면 아무 동작도 하지 않음
- dotenv 형식이 JSON 형식보다 항상 우선됩니다

---

## Supported Formats

### dotenv

```env
DB_HOST=localhost
export API_KEY=abcdef
```

### JSON

```json
{
  "DB_HOST": "localhost",
  "API_KEY": "abcdef"
}
```

---

## Applied Tasks

기본적으로 아래 태스크에 env가 주입됩니다.
 
- `JavaExec`
- `Test`

bootRun에만 적용하고 싶은 경우 `ENV_FILE_APPLY_TO_ALL_JAVAEXEC` 환경변수(default: `true`)를 `false`로 설정하여 사용합니다.

```bash
ENV_FILE_APPLY_TO_ALL_JAVAEXEC=false ./gradlew bootRun
```

---

## Env Override

기본적으로 기존 OS 환경 변수는 덮어쓰지 않습니다.

덮어쓰기 모드 활성화 를 원할 경우 `ENV_FILE_OVERRIDE` 환경변수(default: `false`)를 `true`로 설정하여 사용합니다.

```bash
ENV_FILE_OVERRIDE=true ./gradlew bootRun
```

---

## Multi-module Support

- 멀티모듈 프로젝트의 모든 하위 모듈에 자동 적용
- env 파일은 항상 **루트 프로젝트 기준**으로 탐색

---

## Compatibility

- Java **17 이상** 호환

---

## License

MIT