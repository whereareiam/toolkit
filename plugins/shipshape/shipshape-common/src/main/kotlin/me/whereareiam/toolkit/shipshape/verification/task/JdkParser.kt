package me.whereareiam.toolkit.shipshape.verification.task

import com.sun.source.util.JavacTask
import com.sun.source.util.Trees
import me.whereareiam.toolkit.shipshape.api.java.JavaSource
import me.whereareiam.toolkit.shipshape.api.java.ParsedSource
import java.io.File
import javax.tools.Diagnostic
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.ToolProvider

/**
 * Reads Java files with the parser of the JDK this build runs on (`javax.tools` and `com.sun.source`). It only parses:
 * no name is resolved, no annotation processor runs and nothing is compiled, so Lombok and the classpath of a module play
 * no part, and a rule sees the file as it is written.
 *
 * A library that parses Java was looked at and not taken: each follows a new language level by a release of its own,
 * and the JDK that compiles the code reads it already.
 */
class JdkParser(
	/** The language level the sources are written in, or `null` to read them at the level of the JDK that parses. */
	private val release: Int?,
) {
	private val options: List<String> = listOf("-proc:none") + (release?.let { listOf("--release", it.toString()) } ?: emptyList())

	/**
	 * What keeps the Java that runs this build from parsing the sources, as the rest of a sentence that begins with that
	 * Java, or `null` when nothing does: it is older than [release], or it is a runtime without the compiler.
	 */
	val obstacle: String?
		get() = when {
			release != null && Runtime.version().feature() < release -> "cannot parse Java $release"
			ToolProvider.getSystemJavaCompiler() == null -> "is a runtime without the compiler"
			else -> null
		}

	/**
	 * Parses [sources].
	 *
	 * @return every file with its syntax tree, in the order of [sources]; a file with a syntax error is left out, and it
	 * is the compiler that says what is wrong with it
	 * @throws IllegalStateException when the Java that runs this build cannot parse them; see [obstacle]
	 */
	fun parse(sources: List<JavaSource>): List<ParsedSource> {
		obstacle?.let { error("The Java that runs this build, Java ${Runtime.version().feature()}, $it") }
		if (sources.isEmpty()) return emptyList()

		val compiler = ToolProvider.getSystemJavaCompiler()
		val diagnostics = DiagnosticCollector<JavaFileObject>()

		return compiler.getStandardFileManager(diagnostics, null, Charsets.UTF_8).use { manager ->
			val files = manager.getJavaFileObjectsFromFiles(sources.map(JavaSource::file))
			val task = compiler.getTask(null, manager, diagnostics, options, null, files) as JavacTask
			val units = task.parse().associateBy { written(it.sourceFile) }
			val positions = Trees.instance(task).sourcePositions
			val broken = diagnostics.diagnostics.filter { it.kind == Diagnostic.Kind.ERROR }.mapNotNull { it.source }.map(::written).toSet()

			sources.filterNot { it.file.absoluteFile in broken }
				.mapNotNull { source -> units[source.file.absoluteFile]?.let { ParsedSource(source, it, positions) } }
		}
	}

	/** The file on disk that [parsed] was read from. */
	private fun written(parsed: JavaFileObject): File = File(parsed.toUri()).absoluteFile
}
