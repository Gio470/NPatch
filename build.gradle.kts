import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.kotlin.dsl.extra

plugins {
    alias(libs.plugins.agp.lib) apply false
    alias(libs.plugins.agp.app) apply false
    alias(npatch.plugins.compose.compiler) apply false
    alias(npatch.plugins.kotlin.android) apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.eclipse.jgit:org.eclipse.jgit:7.3.0.202506031305-r")
    }
}

val commitCount = run {
    val repo = FileRepository(rootProject.file(".git"))
    val refId = repo.refDatabase.exactRef("refs/remotes/origin/master").objectId!!
    Git(repo).log().add(refId).call().count()
}

val (coreCommitCount, coreLatestTag) = FileRepositoryBuilder().setGitDir(rootProject.file(".git/modules/core"))
    .runCatching {
        build().use { repo ->
            val git = Git(repo)
            val coreCommitCount = git.log().add(repo.refDatabase.exactRef("HEAD").objectId).call().count()
            val ver = git.describe().setTags(true).setAbbrev(0).call().removePrefix("v")
            coreCommitCount to ver
        }
    }.getOrNull() ?: (1145 to "1.0")

val defaultManagerPackageName by extra("org.lsposed.npatch")
val apiCode by extra(100)
val verCode by extra(commitCount)
val verName by extra("0.7.4-Frankenstein")
val coreVerCode by extra(coreCommitCount)
val coreVerName by extra(coreLatestTag)
val androidMinSdkVersion by extra(24)
val androidTargetSdkVersion by extra(36)
val androidCompileSdkVersion by extra(36)
val androidBuildToolsVersion by extra("36.1.0")
val androidSourceCompatibility by extra(JavaVersion.VERSION_17)
val androidTargetCompatibility by extra(JavaVersion.VERSION_17)

tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}

fun Project.configureBaseExtension() {
    extensions.findByType(BaseExtension::class)?.run {
        compileSdkVersion(androidCompileSdkVersion)
        buildToolsVersion = androidBuildToolsVersion

        defaultConfig {
            minSdk = androidMinSdkVersion
            targetSdk = androidTargetSdkVersion
            versionCode = verCode
            versionName = verName
            multiDexEnabled = true
        }

        compileOptions {
            sourceCompatibility = androidSourceCompatibility
            targetCompatibility = androidTargetCompatibility
            isCoreLibraryDesugaringEnabled = true
        }
    }
}

subprojects {
    plugins.withId("com.android.application") { configureBaseExtension() }
    plugins.withId("com.android.library") { configureBaseExtension() }

    afterEvaluate {
        if (plugins.hasPlugin("com.android.application") || plugins.hasPlugin("com.android.library")) {
            dependencies {              
                add("coreLibraryDesugaring", "com.android.tools:desugar_jdk_libs_nio:2.1.5")
            }
        }
    }
}

allprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
}

project(":core") {
    afterEvaluate {
        if (property("android") is LibraryExtension) {
            val android = property("android") as LibraryExtension
            android.run {
                buildTypes {
                    getByName("release") {
                        proguardFiles(rootProject.file("share/lspatch-rules.pro"))
                    }
                }
            }
        }
    }
}
