package me.whereareiam.toolkit.architecture

import me.whereareiam.toolkit.architecture.model.ArchitectureExtension
import me.whereareiam.toolkit.architecture.verification.ArchitectureVerifier
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

/** Adds architecture classification and root-project dependency verification. */
class ToolkitArchitecturePlugin : Plugin<Project> {
	override fun apply(project: Project) {
		project.pluginManager.apply("base")
		project.extensions.create("architecture", ArchitectureExtension::class.java)

		if (project != project.rootProject)
			return

		val verification = project.tasks.register("verifyArchitecture") {
			group = "verification"
			description = "Verifies module architecture and dependency boundaries."

			doLast {
				val violations = inputs.properties["violations"] as List<*>
				if (violations.isEmpty())
					return@doLast

				throw GradleException(
					buildString {
						appendLine("Architecture rules were violated:")
						violations.forEach { appendLine(" - $it") }
					}.trimEnd()
				)
			}
		}

		project.gradle.projectsEvaluated {
			verification.configure {
				inputs.property("violations", ArchitectureVerifier.violations(project.rootProject))
			}
		}

		project.tasks.named("check").configure {
			dependsOn(verification)
		}
	}
}
