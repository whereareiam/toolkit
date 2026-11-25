plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
}

dependencies {
    testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.3")
}

gradlePlugin {
    plugins {
        create("projectDiscovery") {
            id = "me.whereareiam.toolkit.project-discovery"
            implementationClass = "me.whereareiam.toolkit.discovery.ToolkitProjectDiscoveryPlugin"
        }
    }
}
