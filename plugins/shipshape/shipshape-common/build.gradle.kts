plugins {
    `kotlin-dsl`
    `maven-publish`
    `java-test-fixtures`
}

dependencies {
    api(project(":plugins:shipshape:shipshape-api"))

    testFixturesApi(project(":plugins:shipshape:shipshape-api"))

    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.3")
}
