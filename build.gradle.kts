import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    base
}

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
                    name = "whereAreIAm"
                    val visibility = providers.environmentVariable("PUBLISH_VISIBILITY")
                        .orElse("public")
                        .get()
                        .trim()
                        .lowercase()
                    val repositoryOverride = providers.environmentVariable("PUBLISH_MAVEN_REPOSITORY")
                        .orNull
                        ?.trim()
                        ?.takeIf(String::isNotBlank)
                    val repositoryKey = repositoryOverride ?: when (visibility) {
                        "public" -> "packages"
                        "private" -> "packages-private"
                        else -> error("PUBLISH_VISIBILITY must be public or private")
                    }
                    val baseUrl = providers.environmentVariable("PUBLISH_MAVEN_BASE_URL")
                        .orElse(providers.environmentVariable("MAVEN_REPOSITORY_BASE_URL"))
                        .orElse("https://registry.whereareiam.me/maven")
                        .get()
                        .trimEnd('/')
                    url = uri("$baseUrl/$repositoryKey")

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
