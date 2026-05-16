plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.3.0"
    id("org.jetbrains.intellij.platform") version "2.10.5"
}

group = "com.github.mmrsic.idea.plugins"
version = providers.gradleProperty("pluginVersion").get()

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
                <li><b>Quick Documentation</b>: <code>Ctrl+Q</code> now explains character and color arguments, including ASCII mappings, TI color names, character-set ranges, and 8x8 hex-pattern previews</li>
                <li><b>Smarter editor assistance</b>: auto-pairing for parentheses and quotes, improved completion, configurable automatic line numbers, and optional TI-99/4A 28-column display guides</li>
                <li><b>New navigation and gutter features</b>: line-number declaration navigation, inbound line-reference markers, and <code>CALL SOUND</code> gutter playback</li>
                <li><b>New Character Definitions tool window</b>: browse statically resolvable <code>CALL CHAR</code> definitions with code, ASCII, pattern, icon, and source line</li>
                <li><b>Variables tool window enhancements</b>: array dimensions, effective <code>OPTION BASE</code>, DIM declaration lines, and constant scalar values are now shown directly in the table</li>
                <li><b>Reformat Code integration</b>: the standard IDEA <b>Reformat Code</b> action now routes TI-Basic files to the TI-Basic formatter while preserving default behavior for other file types</li>
                <li><b>Semantic fixes</b>: improved validation for <code>CALL GCHAR</code>, more precise <code>FOR</code>/<code>NEXT</code> imbalance detection, and broader static resolution for color, screen, and sound previews</li>
            </ul>
        """.trimIndent()
    }
    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
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
