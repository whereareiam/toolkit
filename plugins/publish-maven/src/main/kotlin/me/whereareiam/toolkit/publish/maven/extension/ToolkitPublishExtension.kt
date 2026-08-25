package me.whereareiam.toolkit.publish.maven.extension

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.publish.maven.MavenPublication
import javax.inject.Inject

abstract class ToolkitPublishExtension @Inject constructor(
    objects: ObjectFactory
) {

    abstract val artifactId: Property<String>

    /** Name of the Gradle software component to publish; `java` is the default. */
    abstract val component: Property<String>

    /** Whether the selected component contributes its default artifacts and dependencies. */
    abstract val includeComponent: Property<Boolean>

    abstract val channelOverride: Property<String>

    /** Base URL containing the Maven repository namespaces. */
    abstract val repositoryBaseUrl: Property<String>

    /** Visibility of the selected Maven repository: `public` or `private`. */
    abstract val repositoryVisibility: Property<String>

    /** Optional exact repository key, bypassing channel/visibility mapping. */
    abstract val repositoryKeyOverride: Property<String>

    val javadoc: ToolkitPublishJavadocExtension = objects.newInstance(ToolkitPublishJavadocExtension::class.java)

    abstract val name: Property<String>

    val pom: ToolkitPublishPomExtension = objects.newInstance(ToolkitPublishPomExtension::class.java)

    private val publicationActions = mutableListOf<Action<in MavenPublication>>()

    abstract val sourcesJar: Property<Boolean>

    fun javadoc(action: Action<in ToolkitPublishJavadocExtension>) {
        action.execute(javadoc)
    }

    fun pom(action: Action<in ToolkitPublishPomExtension>) {
        action.execute(pom)
    }

    /** Adds configuration to the Maven publication created by this plugin. */
    fun publication(action: Action<in MavenPublication>) {
        publicationActions += action
    }

    internal fun configurePublication(publication: MavenPublication) {
        publicationActions.forEach { it.execute(publication) }
    }
}
