package me.whereareiam.toolkit.shipshape.verification.task

import me.whereareiam.toolkit.shipshape.api.BuildRule
import me.whereareiam.toolkit.shipshape.api.Rule
import me.whereareiam.toolkit.shipshape.api.buildfile.BuildFile
import me.whereareiam.toolkit.shipshape.verification.StandardVerification
import me.whereareiam.toolkit.shipshape.verification.type.VerificationTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Holds the build files of the build, `build.gradle.kts` of the root, of every module and of every build it includes,
 * against the rules of build files. Each file is read once, as text, and never run.
 */
@DisableCachingByDefault(because = "Reading the build files is as fast as fetching a result")
abstract class VerifyBuild : StandardVerification() {
	/** The rules, built in and registered. */
	@get:Internal
	abstract val rules: ListProperty<BuildRule>

	/** The id namespaces of the build's own convention plugins. */
	@get:Input
	abstract val conventionPlugins: SetProperty<String>

	/** Every `build.gradle.kts` of the build: what [BuildFile.below] reads. */
	@get:InputFiles
	@get:PathSensitive(PathSensitivity.RELATIVE)
	abstract val buildFiles: ConfigurableFileCollection

	override fun checkedRules(): List<Rule> = rules.get()

	@TaskAction
	fun verify() {
		val context = context()
		val files = BuildFile.below(directory.get().asFile, conventionPlugins.get())
		val found = files.flatMap { file ->
			rules.get().flatMap { rule -> rule.check(file, context).map { rule to it } }.sortedBy { (_, breach) -> breach.line }
		}

		conclude(VerificationTask.BUILD, found)
	}
}
