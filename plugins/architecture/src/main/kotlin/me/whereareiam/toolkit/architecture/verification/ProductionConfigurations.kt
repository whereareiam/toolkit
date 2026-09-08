package me.whereareiam.toolkit.architecture.verification

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

/** Selects declarations contributing to production, including custom inherited configurations. */
internal object ProductionConfigurations {
	private val names = setOf(
		"api", "implementation", "compileOnly", "compileOnlyApi", "runtimeOnly", "embedded",
		"compileClasspath", "runtimeClasspath", "apiElements", "runtimeElements",
	)

	fun of(project: Project): List<Configuration> = names
		.mapNotNull(project.configurations::findByName)
		.flatMap { it.hierarchy }
		.distinctBy(Configuration::getName)
		.sortedBy(Configuration::getName)

	fun exportedByApi(project: Project): Set<Configuration> =
		project.configurations.findByName("api")?.hierarchy ?: emptySet()
}
