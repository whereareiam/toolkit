plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
}

dependencies {
    implementation(project(":plugins:versioning"))

    testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.12.2")
}

gradlePlugin {
    plugins {
        create("toolkitPublishMaven") {
            id = "me.whereareiam.toolkit.publish.maven"
            implementationClass = "me.whereareiam.toolkit.publish.maven.ToolkitPublishMavenPlugin"
        }
    }
}
