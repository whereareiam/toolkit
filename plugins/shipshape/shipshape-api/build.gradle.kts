plugins {
    `kotlin-dsl`
    `maven-publish`
}

dependencies {
    api(project(":plugins:architecture"))

    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.3")
}

// LineWidthTest reads the Kotlin and the build file of every shipshape module, so they are the test's inputs too.
tasks.test {
    inputs.files(
        fileTree(layout.projectDirectory.dir("..")) {
            include("**/src/**/*.kt", "**/build.gradle.kts")
            exclude("**/build/**")
        },
    ).withPathSensitivity(PathSensitivity.RELATIVE).withPropertyName("shipshapeSources")
}
