plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.3.0"
    id("org.jetbrains.intellij.platform") version "2.10.5"
}

group = "com.github.mmrsic.idea.plugins"
version = "1.1.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        intellijIdea(providers.gradleProperty("intellijPlatformVersion").get())
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = providers.gradleProperty("intellijPlatformSinceBuild").get()
        }

        changeNotes = """
            <ul>
                <li><b>CALL CHAR gutter preview</b>: shows a preview of the custom character bitmap in the gutter</li>
                <li><b>CALL COLOR gutter preview</b>: shows a color swatch for foreground/background colors in the gutter</li>
                <li><b>Variables Tool Window</b>: lists all variables defined in the current TI-Basic file</li>
                <li><b>Find Variable Usages</b>: find all usages of a variable across the file</li>
                <li>IDEA standard formatter is now used for TI-Basic files</li>
                <li>Fix: auto-completion of variable names now works inside CALL statements</li>
                <li>Fix: incorrect statements like XDIR=! and FOR S=2 TO 16) are now highlighted as errors</li>
                <li>Fix: missing or superfluous right parenthesis is now highlighted as an error</li>
            </ul>
        """.trimIndent()
    }
    pluginVerification {
        ides { recommended() }
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
