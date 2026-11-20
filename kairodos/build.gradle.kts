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
android.defaultConfig.applicationId = "com.pm2mod.kairodos"
android.buildTypes.getByName("debug").isDebuggable = false

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
