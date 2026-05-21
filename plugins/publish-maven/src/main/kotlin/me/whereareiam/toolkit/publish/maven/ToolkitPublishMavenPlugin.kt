package me.whereareiam.toolkit.publish.maven

import me.whereareiam.toolkit.publish.maven.extension.ToolkitPublishExtension
import me.whereareiam.toolkit.versioning.extension.ToolkitVersioningExtension
import me.whereareiam.toolkit.versioning.ToolkitVersioningSupport
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.credentials.PasswordCredentials
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions

class ToolkitPublishMavenPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.pluginManager.apply("maven-publish")

        val extension = project.extensions.create("toolkitPublish", ToolkitPublishExtension::class.java)
        extension.name.convention("mavenJava")
        extension.sourcesJar.convention(true)
        extension.javadoc.jar.convention(true)
        extension.javadoc.doclint.convention(false)

        project.afterEvaluate {
            configureJavaArtifacts(project, extension)
            configureJavadocs(project, extension)
            configurePublishing(project, extension)
        }
    }

    private fun configureJavaArtifacts(project: Project, extension: ToolkitPublishExtension) {
        val java = project.extensions.findByType(JavaPluginExtension::class.java)
            ?: throw GradleException("The toolkit publish plugin requires the Java plugin on ${project.path}.")

        if (extension.sourcesJar.get())
            java.withSourcesJar()

        if (extension.javadoc.jar.get())
            java.withJavadocJar()
    }

    private fun configureJavadocs(project: Project, extension: ToolkitPublishExtension) {
        project.tasks.withType(Javadoc::class.java).configureEach {
            val options = options as StandardJavadocDocletOptions

            if (!extension.javadoc.doclint.get())
                options.addStringOption("Xdoclint:none", "-quiet")

            extension.javadoc.title.orNull
                ?.takeIf(String::isNotBlank)
                ?.let { options.docTitle = it }

            extension.javadoc.windowTitle.orNull
                ?.takeIf(String::isNotBlank)
                ?.let { options.windowTitle = it }
        }
    }

    private fun configurePublishing(project: Project, extension: ToolkitPublishExtension) {
        val publishing = project.extensions.getByType(PublishingExtension::class.java)
        val channel = resolveChannel(project, extension)

        publishing.repositories.maven(
            object : Action<MavenArtifactRepository> {
                override fun execute(repository: MavenArtifactRepository) {
                    repository.name = "whereAreIAm"
                    repository.url = project.uri("https://maven.whereareiam.me/$channel")
                    repository.credentials(
                        object : Action<PasswordCredentials> {
                            override fun execute(credentials: PasswordCredentials) {
                                credentials.username = firstNonBlank(
                                    System.getenv("PUBLISH_USER"),
                                    project.findProperty("publishUser")?.toString()
                                ).orEmpty()
                                credentials.password = firstNonBlank(
                                    System.getenv("PUBLISH_TOKEN"),
                                    project.findProperty("publishToken")?.toString()
                                ).orEmpty()
                            }
                        }
                    )
                }
            }
        )

        val publicationName = extension.name.get()
        val publication = publishing.publications.findByName(publicationName) as? MavenPublication
            ?: publishing.publications.create(publicationName, MavenPublication::class.java)

        val javaComponent = project.components.findByName("java")
            ?: throw GradleException("The toolkit publish plugin requires a java component on ${project.path}.")

        publication.from(javaComponent)

        extension.artifactId.orNull
            ?.takeIf(String::isNotBlank)
            ?.let { publication.artifactId = it }

        publication.pom {
            name.set(extension.pom.name)
            description.set(extension.pom.description)
        }
    }

    private fun resolveChannel(project: Project, extension: ToolkitPublishExtension): String {
        extension.channelOverride.orNull
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?.lowercase()
            ?.let { return it }

        val versioning = project.rootProject.extensions.findByType(ToolkitVersioningExtension::class.java)
            ?: project.extensions.findByType(ToolkitVersioningExtension::class.java)

        if (versioning != null)
            return versioning.resolvedChannel().get()

        return ToolkitVersioningSupport.classifyChannel(
            project.version.toString(),
            ToolkitVersioningSupport.DEFAULT_RELEASE_PATTERN
        )
    }

    private fun firstNonBlank(vararg values: String?): String? =
        values.firstOrNull { !it.isNullOrBlank() }
}
