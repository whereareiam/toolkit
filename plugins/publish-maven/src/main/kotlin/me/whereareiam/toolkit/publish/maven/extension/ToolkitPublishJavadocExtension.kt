package me.whereareiam.toolkit.publish.maven.extension

import org.gradle.api.provider.Property

abstract class ToolkitPublishJavadocExtension {

    abstract val doclint: Property<Boolean>

    abstract val jar: Property<Boolean>

    abstract val title: Property<String>

    abstract val windowTitle: Property<String>
}
