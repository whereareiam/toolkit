plugins {
    `kotlin-dsl`
    `maven-publish`
}

dependencies {
    api(project(":plugins:shipshape:shipshape-api"))

    testImplementation(testFixtures(project(":plugins:shipshape:shipshape-common")))
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.3")
}
