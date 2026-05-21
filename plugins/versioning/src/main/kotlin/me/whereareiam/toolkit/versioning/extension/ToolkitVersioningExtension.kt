package me.whereareiam.toolkit.versioning.extension

import me.whereareiam.toolkit.versioning.ToolkitVersioningSupport
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import javax.inject.Inject

abstract class ToolkitVersioningExtension @Inject constructor(
    private val providers: ProviderFactory
) {

    abstract val channelOverride: Property<String>

    abstract val defaultVersion: Property<String>

    abstract val environmentVariableName: Property<String>

    abstract val releasePattern: Property<String>

    fun resolvedChannel(): Provider<String> = providers.provider {
        normalizedChannelOverride()
            ?: ToolkitVersioningSupport.classifyChannel(resolvedVersion().get(), releasePattern.get())
    }

    fun resolvedVersion(): Provider<String> = providers.provider {
        val envName = environmentVariableName.get()
        val envValue = envName
            .takeIf(String::isNotBlank)
            ?.let(System::getenv)
            ?.takeIf(String::isNotBlank)

        envValue ?: defaultVersion.get()
    }

    private fun normalizedChannelOverride(): String? =
        channelOverride.orNull
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?.lowercase()
}
