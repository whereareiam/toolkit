package me.whereareiam.toolkit.publish.docker

import me.whereareiam.toolkit.publish.docker.extension.ToolkitPublishDockerExtension
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject
import java.io.ByteArrayInputStream

/** Adds Docker CLI build, login, and push tasks with private-by-default routing. */
class ToolkitPublishDockerPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create(
            "toolkitPublishDocker",
            ToolkitPublishDockerExtension::class.java
        )
        val providers = project.providers

        extension.registry.convention(
            providers.environmentVariable("PUBLISH_DOCKER_REGISTRY")
                .orElse(providers.environmentVariable("DOCKER_REGISTRY"))
                .orElse("registry.whereareiam.me")
        )
        extension.visibility.convention(
            providers.environmentVariable("PUBLISH_VISIBILITY")
                .orElse("private")
        )
        extension.namespace.convention(
            providers.environmentVariable("PUBLISH_NAMESPACE")
                .orElse("whereareiam")
        )
        extension.image.convention(project.name)
        extension.repositoryOverride.convention(
            providers.environmentVariable("PUBLISH_DOCKER_REPOSITORY")
                .orElse(providers.environmentVariable("DOCKER_REPOSITORY"))
        )
        extension.tags.convention(
            providers.provider { listOf(project.version.toString()) }
        )
        extension.dockerfile.convention(project.layout.projectDirectory.file("Dockerfile"))
        extension.context.convention(project.layout.projectDirectory)
        extension.username.convention(
            providers.environmentVariable("PUBLISH_USER")
                .orElse(providers.environmentVariable("REGISTRY_USER"))
        )
        extension.token.convention(
            providers.environmentVariable("PUBLISH_TOKEN")
                .orElse(providers.environmentVariable("REGISTRY_TOKEN"))
        )

        val login = project.tasks.register("toolkitDockerLogin", ToolkitDockerLoginTask::class.java) {
            registry.set(extension.registry)
            username.set(extension.username)
            token.set(extension.token)
        }
        val build = project.tasks.register("toolkitDockerBuild", ToolkitDockerBuildTask::class.java) {
            repository.set(project.provider { resolveRepository(extension) })
            tags.set(extension.tags)
            dockerfile.set(extension.dockerfile)
            context.set(extension.context)
        }
        project.tasks.register("toolkitDockerPush", ToolkitDockerPushTask::class.java) {
            dependsOn(login, build)
            repository.set(project.provider { resolveRepository(extension) })
            tags.set(extension.tags)
        }
    }

    private fun resolveRepository(extension: ToolkitPublishDockerExtension): String {
        extension.repositoryOverride.orNull
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?.let { return it }

        val visibility = extension.visibility.get().trim().lowercase()
        if (visibility != "private" && visibility != "public")
            throw GradleException("Toolkit Docker publishing requires visibility=private or visibility=public.")

        val repository = if (visibility == "public") "images" else "images-private"
        return "${extension.registry.get().trimEnd('/')}/$repository/" +
                "${extension.namespace.get().trim('/')}/${extension.image.get().trim('/')}"
    }
}

abstract class ToolkitDockerLoginTask : DefaultTask() {

    @get:Inject
    abstract val execOperations: ExecOperations

    abstract val registry: org.gradle.api.provider.Property<String>
    abstract val username: org.gradle.api.provider.Property<String>
    abstract val token: org.gradle.api.provider.Property<String>

    @TaskAction
    fun login() {
        requireCredentials(username.get(), token.get())
        execOperations.exec {
            commandLine("docker", "login", registry.get(), "--username", username.get(), "--password-stdin")
            standardInput = ByteArrayInputStream("${token.get()}\n".toByteArray())
        }
    }
}

abstract class ToolkitDockerBuildTask : DefaultTask() {

    @get:Inject
    abstract val execOperations: ExecOperations

    abstract val repository: org.gradle.api.provider.Property<String>
    abstract val tags: org.gradle.api.provider.ListProperty<String>
    abstract val dockerfile: org.gradle.api.file.RegularFileProperty
    abstract val context: org.gradle.api.file.DirectoryProperty

    @TaskAction
    fun buildImage() {
        val args = mutableListOf("docker", "build", "--file", dockerfile.get().asFile.absolutePath)
        tags.get().filter(String::isNotBlank).forEach { tag ->
            args += listOf("--tag", "${repository.get()}:$tag")
        }
        if (args.size == 4)
            throw GradleException("Toolkit Docker publishing requires at least one image tag.")
        args += context.get().asFile.absolutePath
        execOperations.exec { commandLine(args) }
    }
}

abstract class ToolkitDockerPushTask : DefaultTask() {

    @get:Inject
    abstract val execOperations: ExecOperations

    abstract val repository: org.gradle.api.provider.Property<String>
    abstract val tags: org.gradle.api.provider.ListProperty<String>

    @TaskAction
    fun pushImages() {
        tags.get().filter(String::isNotBlank).forEach { tag ->
            execOperations.exec { commandLine("docker", "push", "${repository.get()}:$tag") }
        }
    }
}

private fun requireCredentials(username: String, token: String) {
    if (username.isBlank() || token.isBlank())
        throw GradleException("Toolkit Docker publishing requires PUBLISH_USER and PUBLISH_TOKEN.")
}
