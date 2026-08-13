package me.whereareiam.toolkit.publish.maven

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class ToolkitPublishMavenPluginTest {

    @TempDir
    lateinit var projectDir: Path

    @Test
    fun `uses versioning derived release channel and configured publication metadata`() {
        writeJavaSource()
        writeBuild(
            """
            import org.gradle.api.artifacts.repositories.MavenArtifactRepository
            import org.gradle.api.publish.PublishingExtension
            import org.gradle.api.publish.maven.MavenPublication
            import org.gradle.external.javadoc.StandardJavadocDocletOptions

            plugins {
                id 'java-library'
                id 'me.whereareiam.toolkit.versioning'
                id 'me.whereareiam.toolkit.publish.maven'
            }

            toolkitPublish {
                name.set('releasePublication')
                artifactId.set('demo-artifact')
                pom {
                    description.set('Demo description')
                    name.set('Demo publication')
                }
                javadoc {
                    title.set('Demo API')
                    windowTitle.set('Demo API Window')
                }
            }

            tasks.register('verifyPublishing') {
                doLast {
                    def publishing = project.extensions.getByType(PublishingExtension)
                    def publication = publishing.publications.getByName('releasePublication') as MavenPublication
                    def repository = publishing.repositories.getByName('whereAreIAm') as MavenArtifactRepository
                    def options = tasks.named('javadoc').get().options as StandardJavadocDocletOptions

                    println("repo=${'$'}{repository.url}")
                    println("publication=${'$'}{publication.name}")
                    println("artifactId=${'$'}{publication.artifactId}")
                    println("pomName=${'$'}{publication.pom.name.get()}")
                    println("pomDescription=${'$'}{publication.pom.description.get()}")
                    println("sourcesJar=${'$'}{tasks.findByName('sourcesJar') != null}")
                    println("javadocJar=${'$'}{tasks.findByName('javadocJar') != null}")
                    println("javadocTitle=${'$'}{options.docTitle}")
                    println("javadocWindowTitle=${'$'}{options.windowTitle}")
                }
            }
            """.trimIndent()
        )

        val result = runner(mapOf("VERSION" to "v1.2.3-RC1")).withArguments("verifyPublishing").build()

        assertTrue(result.output.contains("repo=https://maven.whereareiam.me/release"))
        assertTrue(result.output.contains("publication=releasePublication"))
        assertTrue(result.output.contains("artifactId=demo-artifact"))
        assertTrue(result.output.contains("pomName=Demo publication"))
        assertTrue(result.output.contains("pomDescription=Demo description"))
        assertTrue(result.output.contains("sourcesJar=true"))
        assertTrue(result.output.contains("javadocJar=true"))
        assertTrue(result.output.contains("javadocTitle=Demo API"))
        assertTrue(result.output.contains("javadocWindowTitle=Demo API Window"))
    }

    @Test
    fun `uses versioning derived development channel`() {
        writeJavaSource()
        writeBuild(
            """
            import org.gradle.api.artifacts.repositories.MavenArtifactRepository
            import org.gradle.api.publish.PublishingExtension

            plugins {
                id 'java-library'
                id 'me.whereareiam.toolkit.versioning'
                id 'me.whereareiam.toolkit.publish.maven'
            }

            toolkitPublish {
                pom {
                    description.set('Demo description')
                    name.set('Demo publication')
                }
            }

            tasks.register('verifyPublishing') {
                doLast {
                    def publishing = project.extensions.getByType(PublishingExtension)
                    def repository = publishing.repositories.getByName('whereAreIAm') as MavenArtifactRepository
                    println("repo=${'$'}{repository.url}")
                }
            }
            """.trimIndent()
        )

        val result = runner(mapOf("VERSION" to "dev-abcdef1")).withArguments("verifyPublishing").build()

        assertTrue(result.output.contains("repo=https://maven.whereareiam.me/development"))
    }

    @Test
    fun `publishes a java platform component`() {
        writeBuild(
            """
            import org.gradle.api.publish.PublishingExtension
            import org.gradle.api.publish.maven.MavenPublication

            plugins {
                id 'java-platform'
                id 'me.whereareiam.toolkit.publish.maven'
            }

            javaPlatform {
                allowDependencies()
            }

            dependencies {
                constraints {
                    api 'example:api:1.0.0'
                }
            }

            toolkitPublish {
                component.set('javaPlatform')
                artifactId.set('bom')
                pom {
                    description.set('Example BOM')
                    name.set('Example BOM')
                }
            }

            tasks.register('verifyPublishing') {
                doLast {
                    def publishing = project.extensions.getByType(PublishingExtension)
                    def publication = publishing.publications.getByName('mavenJava') as MavenPublication
                    println("platformComponent=${'$'}{components.findByName('javaPlatform') != null}")
                    println("artifactId=${'$'}{publication.artifactId}")
                    println("pomName=${'$'}{publication.pom.name.get()}")
                }
            }
            """.trimIndent()
        )

        val result = runner(mapOf("VERSION" to "1.2.3")).withArguments("verifyPublishing").build()

        assertTrue(result.output.contains("platformComponent=true"))
        assertTrue(result.output.contains("artifactId=bom"))
        assertTrue(result.output.contains("pomName=Example BOM"))
    }

    @Test
    fun `publish plugin override takes precedence over versioning override`() {
        writeJavaSource()
        writeBuild(
            """
            import org.gradle.api.artifacts.repositories.MavenArtifactRepository
            import org.gradle.api.publish.PublishingExtension

            plugins {
                id 'java-library'
                id 'me.whereareiam.toolkit.versioning'
                id 'me.whereareiam.toolkit.publish.maven'
            }

            toolkitVersioning {
                channelOverride.set('development')
            }

            toolkitPublish {
                channelOverride.set('release')
                pom {
                    description.set('Demo description')
                    name.set('Demo publication')
                }
            }

            tasks.register('verifyPublishing') {
                doLast {
                    def publishing = project.extensions.getByType(PublishingExtension)
                    def repository = publishing.repositories.getByName('whereAreIAm') as MavenArtifactRepository
                    println("repo=${'$'}{repository.url}")
                }
            }
            """.trimIndent()
        )

        val result = runner(mapOf("VERSION" to "dev-abcdef1")).withArguments("verifyPublishing").build()

        assertTrue(result.output.contains("repo=https://maven.whereareiam.me/release"))
    }

    @Test
    fun `creates sources and javadoc jars only when enabled`() {
        writeJavaSource()
        writeBuild(
            """
            import org.gradle.api.publish.PublishingExtension

            plugins {
                id 'java-library'
                id 'me.whereareiam.toolkit.versioning'
                id 'me.whereareiam.toolkit.publish.maven'
            }

            toolkitPublish {
                sourcesJar.set(false)
                pom {
                    description.set('Demo description')
                    name.set('Demo publication')
                }
                javadoc {
                    jar.set(false)
                }
            }

            tasks.register('verifyPublishing') {
                doLast {
                    println("sourcesJar=${'$'}{tasks.findByName('sourcesJar') != null}")
                    println("javadocJar=${'$'}{tasks.findByName('javadocJar') != null}")
                }
            }
            """.trimIndent()
        )

        val result = runner(mapOf("VERSION" to "1.2.3")).withArguments("verifyPublishing").build()

        assertTrue(result.output.contains("sourcesJar=false"))
        assertTrue(result.output.contains("javadocJar=false"))
    }

    private fun runner(environment: Map<String, String>): GradleRunner =
        GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withPluginClasspath()
            .withEnvironment(environment)
            .forwardOutput()

    private fun writeBuild(buildScript: String) {
        Files.writeString(projectDir.resolve("settings.gradle"), "rootProject.name = 'publish-test'\n")
        Files.writeString(projectDir.resolve("build.gradle"), buildScript)
    }

    private fun writeJavaSource() {
        val sourceDir = projectDir.resolve("src/main/java/example")
        Files.createDirectories(sourceDir)
        Files.writeString(
            sourceDir.resolve("Example.java"),
            """
            package example;

            public class Example {
                public String value() {
                    return "demo";
                }
            }
            """.trimIndent()
        )
    }
}
