# 인계 문서: PM2 런처 정식 릴리즈 · 고정 키 서명 자동화

- 작성: Antigravity (2026-11-23)
- 저장소: `makamaka456/KairoDos`, 브랜치 `pm2-mod`
- 받는 사람: Claude Code (이후 PM2 개조판 작업 담당)

## 1. 요약

- `pm2-v1.0.0` **정식 릴리즈를 게시했다** (Latest, prerelease 아님).
- **PM2 전용 고정 서명 키를 새로 만들어** 저장소 Secret에 등록했다.
- 이제 **Actions가 모든 빌드를 이 키로 직접 서명한다.** PC에서 다시 서명하거나 수동으로 업로드할 필요가 없다.
- `versionName`이 바뀌면 `pm2-v<버전>` 릴리즈가 **자동으로 생성된다.**

> 방침: 사용자의 휴대폰에 설치된 기존 앱은 시험용이므로 보존할 필요가 없다. 기준은 "앞으로 모든 릴리즈가 같은 고정 키로 서명되는 것"이다. 이전에 PC에서 쓰던 고정 키는 **더 이상 쓰지 않는다.**

## 2. 커밋 (`pm2-mod`에 푸시됨)

| 커밋 | 내용 |
|---|---|
| `cba5c04f` | `PM2 mod: publish each version's APK as a pm2-v<version> release` — 버전별 정식 릴리즈 단계를 추가 |
| `2e10b1b1` | `PM2 mod: sign every build with the fixed PM2 key from repository secrets` — 고정 키 서명 단계를 추가 |

로컬 작업 사본은 먼저 `git pull`로 맞출 것.

## 3. 저장소 Secret (등록 완료)

| 이름 | 내용 |
|---|---|
| `PM2_KEYSTORE_BASE64` | PKCS12 키스토어(`pm2-release.jks`)를 base64로 인코딩한 값 |
| `PM2_KEYSTORE_PASSWORD` | 키스토어와 키에 공통으로 쓰는 비밀번호 |
| `PM2_KEY_ALIAS` | `pm2` |

- 키: RSA 4096, 유효기간 100년, `CN=PM2 Launcher, O=makamaka456`
- 서명 인증서 SHA-256: `8d6d6bc592ed510bf5461bd9ea9eb9d7ed8fbab952da2e4ed29b2f36d15f435f`
- 키 파일 원본과 비밀번호는 사용자에게 백업용으로 전달했다. **저장소에 키 파일을 커밋하지 말 것.**

## 4. `.github/workflows/pm2-build.yml`의 현재 동작

트리거: `pm2-mod` 브랜치에 push, 또는 workflow_dispatch.

1. `shared` 서브모듈 체크아웃 → `pm2-patches/shared-*.patch` 적용
2. `setup-android` (step id `android`. 서명 단계에서 `steps.android.outputs.build-tools`를 쓴다)
3. Staging 의존성 준비 → `:kairodos:assembleWithoutImagesDebug` 빌드
4. **Sign the APK with the fixed PM2 key** (신규)
   - Secret 3개 중 하나라도 비어 있으면 **빌드를 실패시킨다.**
   - `$ANDROID_HOME/build-tools/<ver>/apksigner sign`으로 다시 서명한 뒤, `verify --print-certs`로 인증서 지문을 로그에 남긴다.
5. artifact `pm2-kairodos-apk` 업로드
6. `pm2-latest` (prerelease) 릴리즈에 `pm2-latest.apk`를 `--clobber`로 덮어 올림 (기존 동작)
7. **Publish the APK to the pm2-v<version> release** (신규, `pm2-mod` 브랜치에서만 실행)
   - `kairodos/build.gradle.kts`의 `android.defaultConfig.versionName = "x.y.z"` 줄을 sed로 읽는다. **이 줄 형식을 바꾸면 이 단계가 깨진다.**
   - `pm2-v<버전>` 릴리즈가 없으면 `GITHUB_TOKEN`으로 생성한다 (`--latest`). 태그가 없으면 현재 커밋에 만든다.
   - 그 릴리즈에 `PM2Launcher-<버전>.apk`가 **이미 있으면 건드리지 않는다.** 버전을 올리지 않은 push는 `pm2-latest`만 갱신한다.
   - `GITHUB_TOKEN`으로 만든 릴리즈는 release 이벤트를 일으키지 않는다. 그래서 `discord-release.yml` (원작자 Discord 공지)은 실행되지 않는다.

## 5. 다음 릴리즈 내는 법

1. `kairodos/build.gradle.kts`에서 `versionCode`를 1 올리고 `versionName`을 새 버전으로 바꾼다.
2. 커밋해서 `pm2-mod`에 push한다.
3. 빌드(약 10분)가 끝나면 `pm2-v<새 버전>` 릴리즈와 APK가 자동으로 생긴다.
4. 필요하면 `gh release edit pm2-v<버전> --notes-file ...`로 릴리즈 노트를 다듬는다.

태그를 수동으로 만들 필요는 없다. 미리 만들어 둬도 된다 (기존 태그를 그대로 쓴다).
업로드된 APK를 교체하려면 `gh release delete-asset pm2-v<버전> PM2Launcher-<버전>.apk --yes`로 지운 뒤 워크플로를 다시 돌린다.

## 6. 현재 릴리즈 상태

- `pm2-v1.0.0`: https://github.com/makamaka456/KairoDos/releases/tag/pm2-v1.0.0
  - 태그: 이전에 만들어 둔 annotated tag → 커밋 `a1a45a18` (versionName 1.0.0, versionCode 2)
  - 첨부: `PM2Launcher-1.0.0.apk`. 고정 키로 서명됨, Actions run `45205874813`, 커밋 `2e10b1b1`에서 빌드
  - 태그 커밋과 빌드 커밋은 워크플로 파일만 다르고 앱 코드는 같다.
  - 처음에 디버그 키로 서명한 APK를 올렸다가 삭제하고 교체했다 (그 사이 받은 사람은 거의 없다).
- `pm2-latest`: 매 push마다 갱신되는 prerelease. 이것도 이제 고정 키로 서명된다.

## 7. 알려진 사소한 문제 / TODO

- `PM2_KEY_ALIAS` 값이 `pm2`라서, Actions 로그에서 "pm2"라는 글자가 전부 `***`로 가려진다. 동작에는 영향이 없다. 원하면 alias를 워크플로에 직접 쓰고 이 Secret을 지우면 된다.
- `pm2-latest` 태그는 `335fd03`에 고정되어 있고, 첨부 APK만 갱신된다 (기존 동작이며 손대지 않았다).
- 시험용으로 이미 설치된 앱(`com.pm2mod.pm2`, 이전 PC 키로 서명)은 서명이 달라서 한 번 지우고 다시 설치해야 한다. 이후 버전부터는 덮어 설치된다.
