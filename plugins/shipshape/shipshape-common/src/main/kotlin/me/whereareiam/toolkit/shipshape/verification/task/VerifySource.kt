package me.whereareiam.toolkit.shipshape.verification.task

import me.whereareiam.toolkit.shipshape.api.Rule
import me.whereareiam.toolkit.shipshape.api.SourceRule
import me.whereareiam.toolkit.shipshape.verification.JavaVerification
import me.whereareiam.toolkit.shipshape.verification.type.VerificationTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Holds the Java sources of the build against the rules that can be read from their text: what is imported and what
 * is written in full, how many files a package holds, what Lombok would have written, where an exception is caught,
 * what a type is named, how a test waits, what carries a Javadoc. Every file is read once.
 */
@DisableCachingByDefault(because = "Reading the sources is as fast as fetching a result")
abstract class VerifySource : JavaVerification() {
	/** The rules, built in and registered. */
	@get:Internal
	abstract val rules: ListProperty<SourceRule>

	override fun checkedRules(): List<Rule> = rules.get()

	@TaskAction
	fun verify() {
		val sources = javaSources()
		val context = context()

		conclude(VerificationTask.SOURCE, rules.get().flatMap { rule -> rule.check(sources, context).map { rule to it } })
	}
}
