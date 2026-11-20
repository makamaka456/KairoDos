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

// 개조(PM2): 원래 KairoDos 옆에 따로 설치되는 앱 ID. 디버그 키로 서명한 빌드도 에뮬레이터가 빠르게
// 돌도록 네이티브 코드는 Release로 짓고, 디버깅은 끈다(shared/gradle/kairo-uitest.gradle 과 같은 이유).
android.defaultConfig.applicationId = "com.pm2mod.kairodos"
android.buildTypes.getByName("debug").apply {
    isDebuggable = false
    externalNativeBuild.cmake.arguments("-DCMAKE_BUILD_TYPE=Release")
}

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
