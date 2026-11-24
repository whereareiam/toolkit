package me.whereareiam.toolkit.shipshape.verification.task

import me.whereareiam.toolkit.shipshape.api.java.JavaSources
import me.whereareiam.toolkit.shipshape.api.java.ParsedSource
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

/**
 * The Java sources of the build as the JDK's parser reads them, parsed once per build however many categories' tasks
 * read them: the first task that asks parses, and every later one takes what it parsed.
 */
abstract class ParsedSources : BuildService<ParsedSources.Parameters> {
	/** What the parser is told. */
	interface Parameters : BuildServiceParameters {
		/** The language level the sources are written in; without it, the level of the JDK that runs Gradle. */
		val javaRelease: Property<Int>
	}

	private val parser = JdkParser(parameters.javaRelease.orNull)
	private var parsed: List<ParsedSource>? = null

	/** Why the JDK that runs Gradle cannot parse the sources, or `null` when it can. */
	val obstacle: String?
		get() = parser.obstacle

	/**
	 * Every file of [sources] the parser reads without an error. Every task of one build hands the same sources, so the
	 * first call parses them and every later call is answered from it.
	 */
	@Synchronized
	fun of(sources: JavaSources): List<ParsedSource> = parsed ?: parser.parse(sources.all).also { parsed = it }

	companion object {
		/** The name the service is registered under. */
		const val NAME = "shipshapeParsedSources"
	}
}
