import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    base
}

val releaseVersionPattern = Regex(
    "^v?(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-[0-9A-Za-z.-]+)?(?:\\+[0-9A-Za-z.-]+)?$"
)

allprojects {
    group = "me.whereareiam.toolkit"
    version = providers.environmentVariable("VERSION").orElse("dev").get()

    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

subprojects {
    plugins.withId("java-base") {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(21))
            }
        }
    }

    plugins.withId("maven-publish") {
        extensions.configure<PublishingExtension> {
            repositories {
                maven {
                    val channel = providers.environmentVariable("PUBLISH_CHANNEL")
                        .orElse(providers.environmentVariable("PUBLISH_REALM"))
                        .orElse(
                            providers.provider {
                                if (releaseVersionPattern.matches(project.version.toString())) "release" else "development"
                            }
                        )
                        .get()
                        .lowercase()

                    name = "whereAreIAm"
                    url = uri("https://maven.whereareiam.me/$channel")

                    credentials {
                        username = providers.environmentVariable("PUBLISH_USER")
                            .orElse(providers.gradleProperty("publishUser"))
                            .orNull
                            .orEmpty()
                        password = providers.environmentVariable("PUBLISH_TOKEN")
                            .orElse(providers.gradleProperty("publishToken"))
                            .orNull
                            .orEmpty()
                    }
                }
            }
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
