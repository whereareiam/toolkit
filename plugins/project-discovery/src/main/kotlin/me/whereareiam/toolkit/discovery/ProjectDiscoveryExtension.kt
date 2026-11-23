package me.whereareiam.toolkit.discovery

import org.gradle.api.provider.SetProperty

/**
 * Settings of project discovery, configured in the settings script as `projectDiscovery { ... }`.
 */
abstract class ProjectDiscoveryExtension {
	/** Directories, relative to the root directory with `/` separators, whose projects are not included. */
	abstract val excludedDirectories: SetProperty<String>

	/**
	 * Leaves the projects in [directories] and below them out of the build. Each directory, such as
	 * `modules/ide-plugin`, is relative to the root directory and must hold at least one discovered project.
	 */
	fun exclude(vararg directories: String) {
		excludedDirectories.addAll(directories.map { it.trim('/') })
	}
}
