package me.whereareiam.toolkit.discovery

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.io.File

/** Tracks the repository tree so configuration cache refreshes when modules change. */
abstract class ProjectPaths : ValueSource<List<String>, ProjectPaths.Parameters> {
	interface Parameters : ValueSourceParameters {
		@get:InputDirectory
		@get:PathSensitive(PathSensitivity.RELATIVE)
		val rootDirectory: DirectoryProperty
	}

	override fun obtain(): List<String> {
		val root = parameters.rootDirectory.get().asFile
		return root.walkTopDown()
			.onEnter { directory -> directory == root || !isExcluded(directory) }
			.filter { it.name == "build.gradle.kts" && it.parentFile != root }
			.map { it.parentFile.relativeTo(root).invariantSeparatorsPath }
			.toList()
			.sorted()
	}

	private fun isExcluded(directory: File): Boolean = directory.name == "build-logic"
		|| directory.name == "build"
		|| directory.name.startsWith('.')
		|| isSeparateBuild(directory)

	private fun isSeparateBuild(directory: File): Boolean =
		directory.resolve("settings.gradle.kts").isFile || directory.resolve("settings.gradle").isFile
}
