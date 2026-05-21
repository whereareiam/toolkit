package me.whereareiam.toolkit.publish.maven.extension

import org.gradle.api.provider.Property

abstract class ToolkitPublishPomExtension {

    abstract val description: Property<String>

    abstract val name: Property<String>
}

