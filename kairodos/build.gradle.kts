plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

extra["kairoProduct"] = "kairodos"
// 개조(PM2): 앱 이름과 아이콘 바탕색
extra["kairoLabel"] = "프린세스메이커2"
extra["kairoLauncherBackground"] = "#FF99AE"
extra["kairoCatalogRules"] = "com.mrjackspade.kairodos.DosCatalog"
extra["kairoChd"] = true
apply(from = rootProject.file("shared/gradle/kairo-app.gradle"))

// 개조(PM2): 원래 KairoDos 옆에 따로 설치되는 앱 ID. 에뮬레이터 엔진은 디버그 빌드에서도 -O2로
// 지어진다(backend-dos/CMakeLists.txt:47-48). 앱 코드가 느려지지 않게 디버깅만 끈다(kairo-uitest.gradle 참고).
// 처음 설치한 com.pm2mod.kairodos 는 빌드마다 바뀌는 임시 키로 서명되어 덮어 설치가 안 된다.
// 그래서 이 ID부터는 Actions가 만든 APK를 사용자 PC의 고정 키로 다시 서명해 설치한다(키는 저장소 밖).
android.defaultConfig.applicationId = "com.pm2mod.pm2"
// 임시(설정 꺼내기용): 디버깅을 켜 adb run-as 로 앱 설정 파일을 읽는다. 꺼낸 뒤 false 로 되돌린다.
android.buildTypes.getByName("debug").isDebuggable = true

// DOSBox Staging's runtime resources, beside the shared catalog assets.
android.sourceSets.getByName("main").assets.srcDir(layout.buildDirectory.dir("generated/staging-assets"))

val stagingResources by tasks.registering(Sync::class) {
    from(rootProject.file("third_party/dosbox-staging/resources"))
    into(layout.buildDirectory.dir("generated/staging-assets/staging-resources"))
    exclude("meson.build")
}
// FluidSynth's default SoundFont (KairoDos#151), the shared GeneralUser GS (shared SoundFonts).
val soundFontAssets = layout.buildDirectory.dir("generated/soundfont-assets")
android.sourceSets.getByName("main").assets.srcDir(soundFontAssets)
val stageSoundFont by tasks.registering(Sync::class) {
    from(rootProject.file("shared/third_party/generaluser-gs/GeneralUser-GS.sf2")) { into("soundfonts") }
    into(soundFontAssets)
}
tasks.named("preBuild") { dependsOn(stagingResources, stageSoundFont) }

dependencies {
    implementation(project(":backend-dos"))
    // Game ZIPs read in place through their document's descriptor (DosZip.members).
    implementation("org.apache.commons:commons-compress:1.27.1")
    testImplementation("junit:junit:4.13.2")
    // Android's org.json is a stub on the JVM; tests parse JSON with the real library.
    testImplementation("org.json:json:20240303")
}
