package me.whereareiam.toolkit.publish.maven

import me.whereareiam.toolkit.publish.maven.extension.ToolkitPublishExtension
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
        extension.component.convention("java")
        extension.includeComponent.convention(true)
        extension.sourcesJar.convention(true)
        extension.repositoryBaseUrl.convention(
            project.providers.environmentVariable("PUBLISH_MAVEN_BASE_URL")
                .orElse(project.providers.environmentVariable("MAVEN_REPOSITORY_BASE_URL"))
                .orElse("https://registry.whereareiam.me/maven")
        )
        extension.repositoryVisibility.convention(
            project.providers.environmentVariable("PUBLISH_VISIBILITY")
                .orElse(project.providers.environmentVariable("MAVEN_VISIBILITY"))
                .orElse("public")
        )
        extension.repositoryKeyOverride.convention(
            project.providers.environmentVariable("PUBLISH_MAVEN_REPOSITORY")
                .orElse(project.providers.environmentVariable("MAVEN_REPOSITORY"))
        )
        extension.javadoc.jar.convention(true)
        extension.javadoc.doclint.convention(false)
        extension.pom.name.convention(project.name)
        extension.pom.description.convention(project.provider {
            project.description ?: project.name
        })

        project.afterEvaluate {
            if (extension.includeComponent.get() && extension.component.get() == "java") {
                configureJavaArtifacts(project, extension)
                configureJavadocs(project, extension)
            }
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
        publishing.repositories.maven(
            object : Action<MavenArtifactRepository> {
                override fun execute(repository: MavenArtifactRepository) {
                    repository.name = "whereAreIAm"
                    repository.url = project.uri(
                        "${extension.repositoryBaseUrl.get().trimEnd('/')}/${resolveRepositoryKey(project, extension)}"
                    )
                    repository.credentials(
                        object : Action<PasswordCredentials> {
                            override fun execute(credentials: PasswordCredentials) {
                                credentials.username = firstNonBlank(
                                    System.getenv("PUBLISH_USER"),
                                    System.getenv("MAVEN_USERNAME"),
                                    project.findProperty("publishUser")?.toString()
                                ).orEmpty()
                                credentials.password = firstNonBlank(
                                    System.getenv("PUBLISH_TOKEN"),
                                    System.getenv("MAVEN_PASSWORD"),
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

        if (extension.includeComponent.get()) {
            val componentName = extension.component.get()
            val component = project.components.findByName(componentName)
                ?: throw GradleException(
                    "The toolkit publish plugin requires a $componentName component on ${project.path}."
                )

            publication.from(component)
        }

        extension.artifactId.orNull
            ?.takeIf(String::isNotBlank)
            ?.let { publication.artifactId = it }

        publication.pom {
            name.set(extension.pom.name)
            description.set(extension.pom.description)
        }

        extension.configurePublication(publication)
    }

    private fun resolveRepositoryKey(
        project: Project,
        extension: ToolkitPublishExtension
    ): String {
        extension.repositoryKeyOverride.orNull
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?.let { return it }

        return when (extension.repositoryVisibility.get().trim().lowercase()) {
            "public" -> "packages"
            "private" -> "packages-private"
            else -> throw GradleException(
                "Toolkit Maven publishing on ${project.path} requires repositoryVisibility to be public or private."
            )
        }
    }

    private fun firstNonBlank(vararg values: String?): String? =
        values.firstOrNull { !it.isNullOrBlank() }
}
