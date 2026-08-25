plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
}

gradlePlugin {
    plugins {
        create("toolkitPublishDocker") {
            id = "me.whereareiam.toolkit.publish.docker"
            implementationClass = "me.whereareiam.toolkit.publish.docker.ToolkitPublishDockerPlugin"
        }
    }
}
