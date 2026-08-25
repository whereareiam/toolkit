package me.whereareiam.toolkit.publish.docker.extension

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/** Configuration for publishing a Docker/OCI image through the local Docker CLI. */
abstract class ToolkitPublishDockerExtension {

    /** Registry hostname, without a scheme. */
    abstract val registry: Property<String>

    /** Artifact Keeper visibility repository: `private` or `public`. */
    abstract val visibility: Property<String>

    /** Namespace inside the registry repository. */
    abstract val namespace: Property<String>

    /** Image name. Defaults to the Gradle project name. */
    abstract val image: Property<String>

    /** Optional complete image repository path, bypassing derived naming. */
    abstract val repositoryOverride: Property<String>

    /** Tags to build and push. */
    abstract val tags: ListProperty<String>

    /** Dockerfile used by the build task. */
    abstract val dockerfile: RegularFileProperty

    /** Docker build context. */
    abstract val context: DirectoryProperty

    /** Registry username, normally supplied through `PUBLISH_USER`. */
    abstract val username: Property<String>

    /** Registry token, normally supplied through `PUBLISH_TOKEN`. */
    abstract val token: Property<String>

}
