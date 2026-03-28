plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.3.0"
    id("org.jetbrains.intellij.platform") version "2.10.5"
}

group = "com.github.mmrsic.idea.plugins"
version = "1.0.1-SNAPSHOT"

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
                <li>Initial release of TI-Basic / TI Extended Basic language support for IntelliJ IDEA</li>
                <li><b>Syntax highlighting</b> for keywords, operators, string literals, and line numbers</li>
                <li><b>Error and warning annotations</b>: invalid/out-of-range line numbers, type mismatches,
                    undefined labels, bad file I/O specifications, duplicate line numbers, non-ascending order,
                    and many more</li>
                <li><b>Code completion</b> (Ctrl+Space) for variables and array subscripts</li>
                <li><b>Shift+Enter</b> smart handler: auto-inserts the next available line number</li>
                <li><b>Format TI-Basic Code</b> action: uppercases keywords, normalizes whitespace,
                    preserves string literals</li>
                <li><b>Resequence Line Numbers</b> action: renumbers all lines with configurable start and step</li>
                <li>Full statement set: LET, DEF, PRINT (incl. file output), INPUT (incl. file), DISPLAY,
                    READ, DATA, RESTORE (incl. file), CALL, GOTO, GOSUB/RETURN, ON GOTO/GOSUB,
                    IF/THEN/ELSE, FOR/NEXT, DIM, OPTION BASE, RANDOMIZE, OPEN, CLOSE, REM, END, STOP, …</li>
                <li>All 10 CALL subprograms: CLEAR, SCREEN, COLOR, HCHAR, VCHAR, GCHAR, CHAR, KEY, JOYST, SOUND</li>
                <li>All built-in numeric and string functions: ABS, SIN, COS, TAN, ATN, EXP, LOG, SQR, INT, RND,
                    SGN, ASC, LEN, POS, VAL, STR$, CHR$, SEG$</li>
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
