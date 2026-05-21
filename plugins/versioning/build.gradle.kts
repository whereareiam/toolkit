plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
}

dependencies {
    testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.12.2")
}

gradlePlugin {
    plugins {
        create("toolkitVersioning") {
            id = "me.whereareiam.toolkit.versioning"
            implementationClass = "me.whereareiam.toolkit.versioning.ToolkitVersioningPlugin"
        }
    }
}
