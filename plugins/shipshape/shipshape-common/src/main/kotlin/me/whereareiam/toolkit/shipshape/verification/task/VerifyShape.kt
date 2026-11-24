package me.whereareiam.toolkit.shipshape.verification.task

import me.whereareiam.toolkit.shipshape.api.Rule
import me.whereareiam.toolkit.shipshape.api.ShapeRule
import me.whereareiam.toolkit.shipshape.verification.JavaVerification
import me.whereareiam.toolkit.shipshape.verification.type.VerificationTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Holds the Java sources of the build against the rules that need the shape of a class and cannot be read from its
 * text. Every file is parsed once, by the JDK that runs Gradle, and nothing is compiled.
 *
 * A Java older than [javaRelease] cannot read the sources, and a runtime without the compiler cannot parse: on such
 * a one the task says so and holds nothing, and a build that runs on a newer JDK holds the rules.
 */
@DisableCachingByDefault(because = "Parsing the sources is as fast as fetching a result")
abstract class VerifyShape : JavaVerification() {
	/** The rules, built in and registered. */
	@get:Internal
	abstract val rules: ListProperty<ShapeRule>

	/** The language level the sources are written in; without it they are read at the level of the JDK that parses. */
	@get:Input
	@get:Optional
	abstract val javaRelease: Property<Int>

	/** The feature version of the JDK that parses: a newer one may read what an older one could not. */
	@get:Input
	abstract val parser: Property<Int>

	override fun checkedRules(): List<Rule> = rules.get()

	@TaskAction
	fun verify() {
		val jdk = JdkParser(javaRelease.orNull)
		val obstacle = jdk.obstacle
		if (obstacle != null) {
			logger.warn(
				"$name holds nothing: Gradle runs on Java ${parser.get()}, which $obstacle. " +
					"Run Gradle on a JDK that can to have the shape of the classes read.",
			)
			report.get().asFile.apply { parentFile.mkdirs() }
				.writeText("Not read: the Java that runs Gradle, Java ${parser.get()}, $obstacle.\n")
			return
		}

		val context = context()
		val found = jdk.parse(javaSources().all).flatMap { file ->
			rules.get().flatMap { rule -> rule.check(file, context).map { rule to it } }
		}

		conclude(VerificationTask.SHAPE, found)
	}
}
