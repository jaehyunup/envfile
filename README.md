# envfile-spring

🌏 **Languages**: [한국어](README.kr.md) | English

---

> 💡 **Why envfile-spring?**
>
> envfile-spring is built for teams that want to manage environment variables
> using **JSON as a first-class configuration format**.
>
> Instead of treating `.env.json` as an afterthought, this plugin embraces
> structured configuration and integrates it naturally into Gradle-based
> execution environments.
>
> * System environment variables are never overridden
> * JSON-based envfiles have the highest priority by default

---

## Plugin Specification

| Plugin | Plugin ID | Description |
|------|-----------|-------------|
| envfile-spring | `io.github.jaehyunup.envfile-spring` | Injects environment variables into Spring Boot / JVM execution tasks |

---

## Getting Started

### 1. Apply the plugin

Add the plugin to your `build.gradle` or `build.gradle.kts` file:

```kotlin
plugins {
    id("io.github.jaehyunup.envfile-spring") version "0.0.1"
}
```

---

### 2. Create envfiles in the project root

Place your envfiles in the **project root directory**:

```text
<project-root>
├─ build.gradle.kts
├─ settings.gradle.kts
├─ .env
├─ .env.local
├─ .env.json
└─ .env.local.json
```

Only existing files are loaded; missing files are safely ignored.

> ⚠️ envfile-spring **never overrides OS-level system environment variables**.

---

## Environment Variable Files

envfile-spring automatically detects envfiles in the project root directory
based on predefined naming conventions.

---

## Supported File Styles

### JSON Style (Primary Feature)

JSON-based envfiles allow **structured, explicit, and scalable**
environment configuration.

```json
{
  "DB_HOST": "localhost",
  "API_KEY": "abcdef"
}
```

Supported filenames:
- `.env.json`
- `.env.local.json`

> 💡 Supporting JSON envfiles as **first-class configuration** is a core design
> goal of this plugin.

---

### DOTENV Style

Traditional key-value envfile format, compatible with existing dotenv workflows.
The `export` syntax is fully supported.

```env
DB_HOST=localhost
export API_KEY=abcdef
```

Supported filenames:
- `.env`
- `.env.local`

---

## Supported File Priority

When multiple envfiles are present, the final value is resolved using
a **two-level priority model**.

---

### 1️⃣ Filename Priority (`.local` > base)

Within the same file style, `.local` files always take precedence
over base files.

```text
.env            → lower
.env.local      → higher

.env.json       → lower
.env.local.json → higher
```

This rule is **always enforced** and cannot be overridden.

---

### 2️⃣ Style Priority (JSON vs DOTENV)

By default, **JSON-style envfiles have higher priority** than dotenv files.

Default load order:

```text
.env
.env.local
.env.json
.env.local.json
```

If the same key exists in multiple files,
the value from the **last loaded file** is used.

---

## Customizing Style Priority (DSL)

Style priority can be customized via **Gradle DSL**.

### Kotlin DSL (`build.gradle.kts`)

```kotlin
envfileSpring {
    priority.set(EnvFileStyle.DOTENV)
}
```

### Groovy DSL (`build.gradle`)

```groovy
import io.github.jaehyunup.envfile.spring.enums.EnvFileStyle

envfileSpring {
    priority = EnvFileStyle.DOTENV
}
```

With this configuration, the load order becomes:

```text
.env.json
.env.local.json
.env
.env.local
```

> 📌 The `.local` > base rule is always preserved,
> regardless of DSL configuration.

---

## Notes

- Priority is configurable **only via Gradle DSL**
- OS system environment variables are **never overridden**
- This plugin affects only **JavaExec-based tasks**
  (e.g. `bootRun`, `run`, and custom JavaExec tasks)
