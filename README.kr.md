# envfile-spring


🌏 **Languages**: 한국어 | [English](README.md)

---

> 💡 **envfile 탄생배경**
>
> 이 플러그인은 **JSON을 환경 변수 설정의 주요 포맷으로 사용하는 팀**을 위해
> `.env.json`을 우선적으로 지원하는 기능이 추가된 Gradle 실행 환경에서의 환경 변수 주입 Gradle plugin 입니다.  
> 
> * 시스템 환경변수를 덮어씌우지 않습니다.
> * JSON 스타일의 envfile이 가장 높은 우선순위로 적용됩니다.


---

## 플러그인 스펙

| Plugin | Plugin ID | Description |
|--------|-----------|-------------|
| envfile-spring | `io.github.jaehyunup.envfile-spring` | Spring Boot / JVM 실행 태스크에 env 주입 |

---

## 시작하기

### 1. build.gradle(kts) 에 플러그인 추가
```kotlin
plugins {
    id("io.github.jaehyunup.envfile-spring") version "0.0.1"
}
```


### 2. 환경변수 파일 생성

```text
<project-root>
├─ build.gradle.kts
├─ settings.gradle.kts
├─ .env.json
```

프로젝트 루트 디렉터리에 환경변수 파일을 위치시킵니다.  
- **지원하는 파일 포맷**은 이 [섹션](#지원-파일-스타일)에서 확인할 수 있습니다.  
- 여러 파일이 존재 할 경우 우선순위에 따른 덮어씌움 로직이 존재합니다.
  - 덮어씌움 우선순위에 대한 자세한 내용은 이 [섹션](#지원-파일-우선순위)을 참고하세요.



> envfile-spirng 은 어떠한 일이 있어도 시스템 환경변수를 절대로 덮어씌우지 않습니다.



---
## 환경변수 파일
이 플러그인이 프로젝트 **루트 디렉터리**에서 정해진 규칙에 따라 envfile을 자동으로 탐지할 때
다음과 같은 envfile 스타일을 지원합니다.

### JSON 스타일

JSON 포맷을 사용하여 **구조화된 환경 변수 관리**가 가능한 스타일입니다.

```json
{
  "DB_HOST": "localhost",
  "API_KEY": "abcdef"
}
```


### DOTENV 스타일

평면적인 key-value 형태의 전통적인 envfile 포맷입니다. (export 스타일 지원)

```env
DB_HOST=localhost
export API_KEY=abcdef
```

지원 파일명:
- `.env`
- `.env.local`


지원 파일명:
- `.env.json`
- `.env.local.json`


---
## 지원 파일 우선순위

여러 envfile이 동시에 존재할 경우, 다음 규칙에 따라 최종 값이 결정됩니다.

### 우선순위 개념
설명 순으로 우선순위가 높습니다.
#### 1️⃣ 스타일 간 우선순위 (JSON vs DOTENV)

기본 설정에서는 **JSON 스타일이 DOTENV 스타일보다 높은 우선순위**를 가집니다.

기본 로드 순서:

```text
.env -> .env.json
```

.env와 .env.json에 같은 키가 존재하는 경우 이 우선순위에 따라
가장 마지막에 로드된 `.env.json` 파일에 선언된 환경변수가 주입됩니다.


#### 2️⃣ 파일명 간 우선순위 (`.local` > `base`)
다양성을 위해 .env.local 및 .env.local.json 파일명을 지원합니다.  
같은 스타일 내에서는 `.local` 파일이 항상 base 파일보다 높은 우선순위를 가집니다. 

```text
.env           → 낮음
.env.local     → 높음

.env.json      → 낮음
.env.local.json→ 높음
```


---
## 스타일 간 우선순위 설정 변경


dotenv 파일(`.env`, `.env.local`)과 json 파일(`.env.json`, `.env.local.json`)에 **같은 키가 동시에 존재할 경우**,
이 플러그인은 **우선순위(priority)** 규칙을 사용해 어떤 값을 최종적으로 사용할지 결정합니다.

### 기본 동작

기본적으로 **JSON 형식이 더 높은 우선순위**를 가집니다.


- `.env`, `.env.local` 파일이 먼저 로드되고
- `.env.json`, `.env.local.json` 파일이 나중에 로드됩니다
- 같은 키가 존재하면 **JSON 값이 dotenv 값을 덮어씁니다**

이는 구조화된 설정(JSON)을 더 신뢰 가능한 설정으로 간주하는 일반적인 기대에 맞춘 기본값입니다.
스타일 간 우선순위는 변경할 수 있습니다.


#### Kotlin DSL (`build.gradle.kts`)

```kotlin
envfileSpring {
    priority.set(EnvFileStyle.DOTENV)
}
```

#### Groovy DSL (`build.gradle`)

```groovy
import io.github.jaehyunup.envfile.spring.enums.EnvFileStyle

envfileSpring {
    priority = EnvFileStyle.DOTENV
}
```

별도설정을 하지 않을 시 기본값인 `EnvFileStyle.JSON` 가 적용된 로드 순서를 가집니다.

```text
.env.json
.env
```


설정 변경을 통해 `EnvFileStyle.DOTENV`의 우선순위를 높이면 로드 순서를 다음과 같이 변경할 수 있습니다:

```text
.env
.env.json
```

이 설정을 적용하면:
- `.env.json` 파일이 먼저 로드되고
- `.env` 파일이 나중에 로드되며
- 같은 키가 존재할 경우 **dotenv 값이 json 값을 덮어씁니다**

#### 참고 사항

- `.local` > `base` (파일 명 우선순위) 규칙은 항상 유지되며 변경할 수 없습니다
- 우선순위는 **Gradle DSL로만 설정할 수 있습니다**
- OS 시스템 환경 변수는 **절대 덮어쓰지 않습니다**
