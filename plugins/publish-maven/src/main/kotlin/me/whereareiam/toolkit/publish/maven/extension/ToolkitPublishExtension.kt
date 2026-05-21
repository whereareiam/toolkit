package me.whereareiam.toolkit.publish.maven.extension

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class ToolkitPublishExtension @Inject constructor(
    objects: ObjectFactory
) {

    abstract val artifactId: Property<String>

    abstract val channelOverride: Property<String>

    val javadoc: ToolkitPublishJavadocExtension = objects.newInstance(ToolkitPublishJavadocExtension::class.java)

    abstract val name: Property<String>

    val pom: ToolkitPublishPomExtension = objects.newInstance(ToolkitPublishPomExtension::class.java)

    abstract val sourcesJar: Property<Boolean>

    fun javadoc(action: Action<in ToolkitPublishJavadocExtension>) {
        action.execute(javadoc)
    }

    fun pom(action: Action<in ToolkitPublishPomExtension>) {
        action.execute(pom)
    }
}

