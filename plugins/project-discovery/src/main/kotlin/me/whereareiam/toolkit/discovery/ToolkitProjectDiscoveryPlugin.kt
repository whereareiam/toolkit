package me.whereareiam.toolkit.discovery

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

/**
 * Discovers Gradle subprojects from directories containing a build script.
 *
 * A directory that holds its own settings script is a separate build and is left out together with everything below
 * it. Directories named in the `projectDiscovery` extension are left out as well. Projects are included once the
 * settings script has run, so the script decides what to exclude before any project exists.
 */
class ToolkitProjectDiscoveryPlugin : Plugin<Settings> {
	override fun apply(settings: Settings) {
		val discovery = settings.extensions.create("projectDiscovery", ProjectDiscoveryExtension::class.java)
		val root = settings.rootDir
		val paths = settings.providers.of(ProjectPaths::class.java) {
			parameters.rootDirectory.set(root)
		}

		settings.gradle.settingsEvaluated {
			val excluded = discovery.excludedDirectories.get()
			val discovered = paths.get()
			val unknown = excluded.filterNot { directory -> discovered.any { it.isWithin(directory) } }
			if (unknown.isNotEmpty())
				throw GradleException(
					"projectDiscovery excludes ${unknown.sorted().joinToString()}, which hold no discovered project"
				)

			discovered
				.filterNot { path -> excluded.any { path.isWithin(it) } }
				.forEach { path -> settings.include(":${path.replace('/', ':')}") }
		}
	}
}

private fun String.isWithin(directory: String): Boolean = this == directory || startsWith("$directory/")
