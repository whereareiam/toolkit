package me.whereareiam.toolkit.shipshape

import me.whereareiam.toolkit.shipshape.api.ShipshapeExtension
import me.whereareiam.toolkit.shipshape.verification.DefaultShipshapeExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.language.base.plugins.LifecycleBasePlugin

/**
 * Holds a build's Java and build files to the general rules of the standard: `verifyShape`, `verifySource` and
 * `verifyBuild`, each a part of `check`, over the root project and every module below it.
 */
class ToolkitShipshapePlugin : Plugin<Project> {
	override fun apply(project: Project) {
		if (project != project.rootProject)
			throw GradleException("$ID is applied to the root project, which holds every module; ${project.path} is none")

		project.pluginManager.apply(LifecycleBasePlugin::class.java)

		val extension = project.objects.newInstance(DefaultShipshapeExtension::class.java)
		project.extensions.add(ShipshapeExtension::class.java, ShipshapeExtension.NAME, extension)
		extension.packageRoots.convention(project.provider { setOf(project.group.toString()) - "" })
		BuiltInRules.registerWith(extension)
		ShipshapeTasks(project, extension).register()
	}

	companion object {
		/** The id this plugin is applied by. */
		const val ID = "me.whereareiam.toolkit.shipshape"
	}
}
