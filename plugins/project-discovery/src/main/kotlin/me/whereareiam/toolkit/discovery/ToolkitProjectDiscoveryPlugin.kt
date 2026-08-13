package me.whereareiam.toolkit.discovery

import org.gradle.api.Plugin
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.initialization.Settings
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

/** Discovers Gradle subprojects from directories containing a build script. */
class ToolkitProjectDiscoveryPlugin : Plugin<Settings> {
	override fun apply(settings: Settings) {
		val root = settings.rootDir
		settings.providers.of(ProjectPaths::class.java) {
			parameters.rootDirectory.set(root)
		}.get().forEach { path ->
			settings.include(":${path.replace('/', ':')}")
		}
	}
}

/** Tracks the repository tree so configuration cache refreshes when modules change. */
abstract class ProjectPaths : ValueSource<List<String>, ProjectPaths.Parameters> {
	interface Parameters : ValueSourceParameters {
		@get:InputDirectory
		@get:PathSensitive(PathSensitivity.RELATIVE)
		val rootDirectory: DirectoryProperty
	}

	override fun obtain(): List<String> = parameters.rootDirectory.get().asFile.walkTopDown()
		.onEnter { directory -> !isExcluded(directory) }
		.filter { it.name == "build.gradle.kts" }
		.map { it.parentFile.relativeTo(parameters.rootDirectory.get().asFile).invariantSeparatorsPath }
		.filterNot { it == "." }
		.toList()
		.sorted()

	private fun isExcluded(directory: java.io.File): Boolean = directory.name == "build-logic"
		|| directory.name == "build"
		|| directory.name == ".gradle"
		|| directory.name.startsWith('.')
}
