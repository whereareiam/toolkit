package me.whereareiam.toolkit.architecture.model

import me.whereareiam.toolkit.architecture.type.ArchitectureKind

/**
 * Describes module ownership for project-dependency verification.
 * Omitted values are inferred from API modules and their owning project directories.
 */
abstract class ArchitectureExtension {
	/** Overrides the module role inferred from its name, sources, and plugins. */
	var kind: ArchitectureKind? = null

	/**
	 * Overrides the inferred API-family identifier, for example for a flat project layout.
	 * An API-owning project's override also applies to its unconfigured API children and implementations.
	 */
	var family: String? = null

	/**
	 * Marks the build's shared, dependency-free API. At most one project can have this role.
	 * By default, a direct root child named "api" or "<root-name>-api" is the shared API.
	 */
	var rootApi: Boolean? = null

	val api: ArchitectureKind
		get() = ArchitectureKind.API
	val implementation: ArchitectureKind
		get() = ArchitectureKind.IMPLEMENTATION
	val assembly: ArchitectureKind
		get() = ArchitectureKind.ASSEMBLY
}
