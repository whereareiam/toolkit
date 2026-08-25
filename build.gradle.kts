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
                        "public" -> channel
                        "private" -> "private-$channel"
                        else -> error("PUBLISH_VISIBILITY must be public or private")
                    }
                    val baseUrl = providers.environmentVariable("PUBLISH_MAVEN_BASE_URL")
                        .orElse(providers.environmentVariable("MAVEN_REPOSITORY_BASE_URL"))
                        .orElse("https://maven.whereareiam.me/maven")
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
