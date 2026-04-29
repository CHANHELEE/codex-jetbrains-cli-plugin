# Codex JetBrains 플러그인

[Codex](https://github.com/openai/codex) CLI 도구를 JetBrains IDE에 통합하는 플러그인입니다. 액션을 실행하면 IDE 내장 터미널에서 Codex가 인터랙티브하게 실행되고, Codex가 파일을 직접 수정합니다.

## 프로젝트 개요

- **플러그인 ID**: `com.codex.jetbrains-plugin`
- **플러그인 이름**: Codex Assistant
- **언어**: Kotlin (JVM 17)
- **빌드**: Gradle 8.7 + `org.jetbrains.intellij` 플러그인 v1.17.4
- **대상 IDE**: IntelliJ IDEA Community 2024.1+ (`sinceBuild=241`, `untilBuild=253.*`)
- **주요 의존성**: `org.jetbrains.plugins.terminal` (IntelliJ 내장)

## 아키텍처

### 핵심 흐름

1. 사용자가 액션 실행 (`Ctrl+Shift+X` / 툴바 버튼) — `CodexService.isRunning()` 이 `true`면 버튼 비활성화
2. `CodexService.runInTerminal()` 호출 (다이얼로그 없음, 즉시 실행)
3. `TerminalToolWindowManager`로 IDE 내장 터미널 탭을 열고 `codex [--ask-for-approval untrusted] [extraArgs]` 실행
4. Codex가 터미널에서 인터랙티브하게 실행되며 파일을 직접 수정
5. 에디터에서 코드를 선택한 뒤 `Ctrl+Alt+X`로 `@파일경로#L줄번호` 참조를 실행 중인 터미널에 전송 가능

### 패키지 구조

```
src/main/kotlin/com/codex/plugin/
├── actions/
│   ├── OpenCodexTerminalAction.kt    # 진입점 — 단축키/툴바에서 Codex 터미널 실행
│   └── CodexCopyAction.kt            # 에디터 팝업에서 선택 영역 참조를 Codex 터미널로 복사
├── services/
│   └── CodexService.kt               # TerminalToolWindowManager로 codex 실행 (object)
└── ui/
    ├── CodexSettings.kt              # 설정 상태 저장 (ApplicationService)
    └── CodexSettingsConfigurable.kt  # Tools > Codex Assistant 설정 UI
```

### plugin.xml 확장 포인트

| 확장 포인트 | 클래스 | 역할 |
|---|---|---|
| `applicationService` | `CodexSettings` | 설정 영속 저장 (실행 경로, 플래그 등) |
| `applicationConfigurable` | `CodexSettingsConfigurable` | Tools > Codex Assistant 설정 페이지 |
| `notificationGroup` | — | `"Codex Assistant"` BALLOON 알림 그룹 |
| 액션 `Codex.OpenTerminal` | `OpenCodexTerminalAction` | `Ctrl+Shift+X` + 툴바 버튼 |
| 액션 `Codex.CopyToCodex` | `CodexCopyAction` | `Ctrl+Alt+X` + 에디터 우클릭 팝업 |

## 액션 트리거 포인트

| 액션 | 트리거 | 설정 |
|---|---|---|
| `Codex.OpenTerminal` | 키보드 단축키 | `Ctrl+Shift+X` |
| `Codex.OpenTerminal` | 상단 툴바 우측 (New UI) | `add-to-group group-id="MainToolbarRight" anchor="last"` |
| `Codex.OpenTerminal` | 상단 툴바 우측 (Classic UI) | `add-to-group group-id="NavBarToolBar" anchor="last"` |
| `Codex.CopyToCodex` | 에디터 우클릭 팝업 | `add-to-group group-id="EditorPopupMenu" anchor="first"` |
| `Codex.CopyToCodex` | 키보드 단축키 | `Ctrl+Alt+X` |

`Codex.CopyToCodex`는 `CodexService.isRunning()` 이 `true`이고 에디터에서 텍스트가 선택된 경우에만 활성화됩니다.

## Codex CLI 연동

- 기본 실행 파일: `codex` (`PATH`에서 탐색)
- 설정에서 경로 변경 가능: Tools > Codex Assistant > Codex 실행 파일 경로
- 호출 패턴:
  ```
  codex [--ask-for-approval untrusted] [extraArgs] ["코드 수정 전 확인 프롬프트"]
  ```
- `confirmCodeChanges=true` 이면 `--ask-for-approval untrusted` 플래그와 확인 요청 시스템 프롬프트가 자동 추가됨
- 터미널 실행: `TerminalToolWindowManager.getInstance(project).createLocalShellWidget(workDir, "Codex")`
- 작업 디렉토리: `project.basePath` → 홈 디렉토리 순으로 fallback
- 실행 중인 터미널은 `ConcurrentHashMap<Project, ShellTerminalWidget>`으로 프로젝트별 추적
- 터미널 종료(Dispose) 시 자동으로 맵에서 제거 (`Disposer.register` 활용)
- `copyToTerminal()`은 `widget.executeWithTtyConnector { connector -> connector.write(text) }`로 텍스트를 직접 전송
- shell quoting: `shellQuote()`로 경로/인자를 single-quote로 감쌈 (내부 `'` 처리 포함)

## 설정

`CodexSettings` (PersistentStateComponent, 저장 파일: `CodexAssistant.xml`)에 저장:

| 필드 | 기본값 | UI 노출 | 설명 |
|---|---|---|---|
| `codexPath` | `codex` | O | codex 실행 파일 경로 |
| `extraArgs` | `""` | O | 매 실행마다 추가되는 CLI 플래그 |
| `confirmCodeChanges` | `true` | O | 코드 수정 전 사용자 확인 요청 (`--ask-for-approval untrusted` 연동) |
| `autoApplyThreshold` | `0` | X | (예약) 향후 자동 적용 임계값 |

## 빌드 및 실행

```bash
# 배포용 zip 빌드 → build/distributions/*.zip
./gradlew buildPlugin

# 샌드박스 IDE 인스턴스에서 플러그인 실행
./gradlew runIde

# 테스트 실행
./gradlew test

# 플러그인 호환성 검증
./gradlew verifyPlugin
```

### 기존 IntelliJ에 직접 설치

1. `./gradlew buildPlugin` 실행
2. `Settings → Plugins → ⚙️ → Install Plugin from Disk...`
3. `build/distributions/codex-jetbrains-plugin-1.0.0.zip` 선택 후 IDE 재시작

## 주요 컨벤션

- 설정 영속화: `CodexSettings`에 `@State` + `@Storage` 어노테이션 사용, 직접 파일 I/O 금지
- 오류 알림: `NotificationGroupManager` (말풍선) 사용, `System.err`나 다이얼로그 사용 금지
- `CodexService`는 `object`로 선언하되 프로젝트별 상태는 내부 `ConcurrentHashMap`으로 관리

## 주의 파일

- `src/main/resources/META-INF/plugin.xml` — 모든 확장 포인트와 액션이 선언됨. Kotlin 클래스명과 항상 동기화 유지 필요
- `build.gradle.kts` — `intellij { plugins.set(listOf("terminal")) }` 필수. 제거하면 `TerminalToolWindowManager` 컴파일 불가