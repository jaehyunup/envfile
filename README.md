# envfile

`.env` / `.env.json` 파일을 읽어 Gradle 실행 태스크에 **환경 변수 주입**을 관리해주는  Gradle 플러그인입니다.
로컬 개발 및 테스트 환경에서 OS 환경 변수를 직접 설정하지 않고도 Spring Boot 애플리케이션을 실행할 수 있습니다.

---

## Plugins

| Plugin | Plugin ID | Description |
|--------|-----------|-------------|
| envfile-spring | `io.github.jaehyunup.envfile-spring` | Spring Boot / JVM 실행 태스크에 env 주입 |

---

# envfile-spring
## Getting Started

```kotlin
plugins {
    id("io.github.jaehyunup.envfile-spring") version "0.0.1"
}
```

<details>
<summary><strong>Local Maven 저장소에 배포하여 사용하는 방법</strong></summary>
1. 프로젝트 루트에서 다음 명령어 실행하여 local maven repository에 플러그인 빌드

```bash
./gradlew clean publishToMavenLocal
```

2. 사용하는 프로젝트의 `settings.gradle(.kts)`에 다음을 추가하고
```kotlin
pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
}
```

3. 플러그인 사용.

```kotlin
plugins {
    id("io.github.jaehyunup.envfile-spring") version "0.0.1"
}
```

</details>



---
## Supported Formats

### 1. DOTENV

```env
DB_HOST=localhost
export API_KEY=abcdef
```

### 2. JSON

```json
{
  "DB_HOST": "localhost",
  "API_KEY": "abcdef"
}
```

---

## Mode options

### 1. `ENV_FILE_APPLY_TO_ALL_JAVAEXEC` (default: true)
기본적으로 JavaExec 전체에 환경변수 주입이 시도됩니다.   
이 옵션을 `false`로 변경하면 bootRun/Test Task에만 환경변수 주입이 시도됩니다.

---
### 2. `ENV_FILE_OVERRIDE` (default: false)

기본적으로 OS system environment에 이미 정의 되어있다면 envfile에 정의해둔 값으로 덮어쓰지 않습니다.    
OS system environment 에 있는 값이더라도 env file 에 선언한 값으로 덮어쓰고 싶다면 이 옵션을 `true`로 설정합니다.

