# Codex Assistant — JetBrains Plugin

[Codex](https://github.com/openai/codex) CLI를 JetBrains IDE의 내장 터미널에서 바로 실행하는 플러그인입니다.

## 주요 기능

- **터미널 즉시 실행** — 단축키(`Ctrl+Shift+X`) 또는 툴바 버튼 한 번으로 Codex 세션 시작
- **코드 참조 전송** — 에디터에서 코드를 선택하고 `Ctrl+Alt+X`로 실행 중인 Codex 터미널에 `@파일경로#L줄번호` 또는 `@파일경로#L시작줄-끝줄` 형식의 참조 전송
- **코드 변경 승인 모드** — 설정에서 활성화하면 Codex가 파일을 수정하기 전에 사용자에게 확인 요청
- **프로젝트별 세션 관리** — 각 프로젝트마다 독립적인 Codex 터미널 세션 유지

## 요구 사항

- JetBrains IDE 2024.1 이상 (IntelliJ IDEA, PyCharm, WebStorm 등)
- [Codex CLI](https://github.com/openai/codex) 설치 및 `PATH` 등록

## 설치

### JetBrains Marketplace

> 업로드 예정

### 수동 설치

1. GitHub Releases 페이지에서 최신 `.zip` 파일 다운로드
2. IDE에서 `Settings → Plugins → ⚙️ → Install Plugin from Disk...` 선택
3. 다운로드한 `.zip` 파일 선택 후 IDE 재시작

### 소스에서 빌드

```bash
git clone <repository-url>
cd codex-jetbrains-plugin
./gradlew buildPlugin
# build/distributions/codex-jetbrains-plugin-1.0.0.zip 생성됨
```

## 사용법

### Codex 터미널 열기

| 방법 | 동작 |
|------|------|
| `Ctrl+Shift+X` | 현재 프로젝트에서 Codex 터미널 실행 |
| 상단 툴바 아이콘 클릭 | 동일 |

이미 실행 중인 세션이 있으면 새 Codex 터미널을 열 수 없도록 실행 액션이 비활성화됩니다.

### 코드 참조 전송

1. 에디터에서 코드 블록 선택
2. `Ctrl+Alt+X` 또는 우클릭 메뉴 또는 코드 드래그 시 표출되는 툴바 → **Copy to Codex Terminal**
3. `@파일경로#L시작줄-끝줄` 형식의 참조가 실행 중인 Codex 터미널로 전송됨

> Codex가 실행 중이지 않은 경우 이 액션은 비활성화됩니다. 텍스트를 선택하지 않은 경우에는 `@파일경로` 형식의 파일 참조가 전송됩니다.

## 설정

`Settings → Tools → Codex Assistant`에서 변경할 수 있습니다.

| 항목 | 기본값 | 설명 |
|------|--------|------|
| Codex 실행 파일 경로 | `codex` | `PATH` 이외의 경로에 설치된 경우 절대 경로 입력 |
| 추가 CLI 인자 | (없음) | 매 실행마다 `codex` 명령에 추가할 플래그 |
| 코드 변경 전 확인 요청 | 활성화 | `--ask-for-approval untrusted` 플래그 자동 추가 |

## 빌드

```bash
# 플러그인 zip 빌드
./gradlew buildPlugin

# 샌드박스 IDE에서 실행 (개발용)
./gradlew runIde

# 테스트
./gradlew test

# 호환성 검증
./gradlew verifyPlugin
```
