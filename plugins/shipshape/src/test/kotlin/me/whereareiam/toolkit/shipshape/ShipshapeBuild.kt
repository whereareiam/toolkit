package me.whereareiam.toolkit.shipshape

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File
import java.nio.file.Path
import java.util.Properties
import kotlin.io.path.createParentDirectories
import kotlin.io.path.deleteExisting
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * A build in a folder that applies shipshape, run with Gradle's TestKit: `session-service` with an API and its logic,
 * which keep every rule until a test writes otherwise.
 */
internal class ShipshapeBuild(private val directory: Path) {
	/** Writes a file by its path from the root project's folder. */
	fun write(path: String, text: String): ShipshapeBuild = apply {
		directory.resolve(path).createParentDirectories().writeText(text)
	}

	/** Removes the file at [path]. */
	fun remove(path: String): ShipshapeBuild = apply { directory.resolve(path).deleteExisting() }

	/** What the file at [path] holds. */
	fun read(path: String): String = directory.resolve(path).readText()

	/**
	 * Lays out a plugin in `buildSrc` that applies shipshape and registers what its Java [sources] declare, each by its
	 * class's name below `src/main/java`. The plugin and shipshape then share the build's classpath, as a convention
	 * plugin and shipshape do.
	 */
	fun pluginInBuildSrc(plugins: Map<String, String>, sources: Map<String, String>): ShipshapeBuild = apply {
		val shipshape = classpath().joinToString(", ") { "\"${it.invariantSeparatorsPath}\"" }
		val declared = plugins.entries.joinToString("\n") { (id, type) ->
			"\t\tcreate(\"$id\") {\n\t\t\tid = \"$id\"\n\t\t\timplementationClass = \"$type\"\n\t\t}"
		}

		write(
			"buildSrc/build.gradle.kts",
			"plugins {\n\t`java-gradle-plugin`\n}\n\ndependencies {\n\timplementation(files($shipshape))\n}\n\n" +
				"gradlePlugin {\n\tplugins {\n$declared\n\t}\n}\n",
		)
		sources.forEach { (type, text) -> write("buildSrc/src/main/java/${type.replace('.', '/')}.java", text) }
	}

	/** Runs [arguments] with shipshape on the plugin classpath of TestKit, and expects success. */
	fun build(vararg arguments: String): BuildResult = runner(arguments, injected = true).build()

	/** Runs [arguments] with shipshape on the plugin classpath of TestKit, and expects a failure. */
	fun fail(vararg arguments: String): BuildResult = runner(arguments, injected = true).buildAndFail()

	/** Runs [arguments] with shipshape on the classpath of `buildSrc` alone, and expects success. */
	fun buildWithBuildSrc(vararg arguments: String): BuildResult = runner(arguments, injected = false).build()

	/** Runs [arguments] with shipshape on the classpath of `buildSrc` alone, and expects a failure. */
	fun failWithBuildSrc(vararg arguments: String): BuildResult = runner(arguments, injected = false).buildAndFail()

	private fun runner(arguments: Array<out String>, injected: Boolean): GradleRunner {
		val runner = GradleRunner.create().withProjectDir(directory.toFile())
			.withArguments(*arguments, "--stacktrace", "--warning-mode=fail")

		return if (injected) runner.withPluginClasspath() else runner
	}

	/** The classpath TestKit injects: shipshape's classes and resources, and the architecture plugin. */
	private fun classpath(): List<File> {
		val metadata = Properties()
		ShipshapeBuild::class.java.classLoader.getResourceAsStream(METADATA)!!.use(metadata::load)

		return metadata.getProperty(CLASSPATH).split(File.pathSeparator).map(::File)
	}

	companion object {
		private const val METADATA = "plugin-under-test-metadata.properties"
		private const val CLASSPATH = "implementation-classpath"

		const val API = "session-api/src/main/java/com/example/session"
		const val COMMON = "session-common/src/main/java/com/example/session"

		/** `session-service` with its root build file [root] and Java that keeps every rule. */
		fun service(directory: Path, root: String = ROOT): ShipshapeBuild = ShipshapeBuild(directory)
			.write("settings.gradle.kts", "rootProject.name = \"session-service\"\n\ninclude(\":session-api\", \":session-common\")\n")
			.write("build.gradle.kts", root)
			.write("$API/SessionService.java", SERVICE)
			.write("$COMMON/DefaultSessionService.java", DEFAULT_SERVICE)
			.write("session-common/src/test/java/com/example/session/DefaultSessionServiceTest.java", TEST)

		/** A root build file that applies shipshape and names its group, which is the package root. */
		const val ROOT = "plugins {\n\tid(\"me.whereareiam.toolkit.shipshape\")\n}\n\ngroup = \"com.example\"\n"

		private const val SERVICE = "package com.example.session;\n\n/**\n * Opens the sessions of a user.\n */\n" +
			"public interface SessionService {\n\t/**\n\t * Opens a session of [user].\n\t */\n\tvoid open(int user);\n}\n"

		private const val DEFAULT_SERVICE = "package com.example.session;\n\n/**\n * Counts the sessions it opens.\n */\n" +
			"public class DefaultSessionService implements SessionService {\n\tprivate int opened;\n\n\t@Override\n" +
			"\tpublic void open(int user) {\n\t\topened++;\n\t}\n}\n"

		private const val TEST = "package com.example.session;\n\nimport org.junit.jupiter.api.Test;\n\n" +
			"class DefaultSessionServiceTest {\n\t@Test\n\tvoid opens() {\n\t\tnew DefaultSessionService().open(1);\n\t}\n}\n"
	}
}
