package me.whereareiam.toolkit.shipshape.api.buildfile

import me.whereareiam.toolkit.shipshape.api.java.SourceText
import java.io.File

/**
 * One script of the build, a `build.gradle.kts` or a `settings.gradle.kts`, as the rules of build files read it: what
 * its block `plugins` applies, what its blocks `dependencies` declare and the strings it writes, each with its line. A
 * declaration stands directly in its block, on one line or over several, and what a closure or a loop inside the block
 * writes is none.
 *
 * The file is read as text and never run: running it would resolve and configure the build it describes, and
 * a script of Kotlin has no parser in the JDK or in what Gradle hands a plugin. So the reading knows the
 * forms a build file is commonly written in and no other; what it cannot see is said where the rule that needs it
 * is told.
 */
class BuildFile(
	/** The file's path from the root project's folder, for a breach to name. */
	val path: String,
	text: String,
	/** The id namespaces of the build's own convention plugins, which tell a convention from an external plugin. */
	conventionPlugins: Set<String> = emptySet(),
) {
	/** Which script of the build the file is, as its name says. */
	val script: Script = Script.named(path.substringAfterLast('/'))

	private val written: List<String> = text.lines()

	/** The file's lines without their comments; what a string literal says is kept. */
	val lines: List<String> = SourceText.strip(text, literals = true).lines()

	/** The same lines without what their literals say, so that every brace and parenthesis left is one of the code. */
	private val code: List<String> = SourceText.strip(text, literals = false).lines()

	/** What the block `plugins` of the file applies, in the order it is written. */
	val plugins: List<PluginDeclaration> = blocks(PLUGINS).filter { it.depth == 0 }
		.flatMap { block -> block.lines.mapNotNull { PluginDeclaration.of(it + 1, lines[it], conventionPlugins) } }

	private val dependencyBlocks: List<Block> = blocks(DEPENDENCIES)

	/** The blocks `dependencies`, each as what it declares in the order it is written. */
	val dependencies: List<List<DependencyDeclaration>> = dependencyBlocks.map(::declarations)

	/** What the blocks `constraints` declare, which stand inside a block `dependencies`, in the order they are written. */
	val constraints: List<DependencyDeclaration> = blocks(CONSTRAINTS).flatMap(::declarations)

	/**
	 * Every line of the blocks `dependencies` with its number, counted from one: the declarations, and with
	 * them what a closure or a loop inside a block writes.
	 */
	val dependencyLines: List<IndexedValue<String>> = dependencyBlocks.flatMap { it.first..it.last }.distinct()
		.map { IndexedValue(it + 1, lines[it]) }

	/**
	 * Every string the file writes outside its comments, with the line it stands on, counted from one: what stands between
	 * its quotes. A string written over several lines is none.
	 */
	val strings: List<IndexedValue<String>> = lines.flatMapIndexed { index, line ->
		STRING.findAll(line).map { IndexedValue(index + 1, it.groupValues[1]) }.toList()
	}

	/** Says whether the block `plugins` applies the plugin of the id or the accessor [name]. */
	fun applies(name: String): Boolean = plugins.any { it.name == name }

	/** What [block] adds to a configuration, each declaration told whether an empty line stands before it. */
	private fun declarations(block: Block): List<DependencyDeclaration> {
		if (block.first == block.last) {
			val between = lines[block.first].substringAfter(OPENING).substringBeforeLast(CLOSING)

			return listOfNotNull(DependencyDeclaration.of(block.first + 1, between, separated = false))
		}

		var before: Int? = null

		return block.lines.mapNotNull { index ->
			val separated = before?.let { last -> (last + 1 until index).any { written[it].isBlank() } } ?: false

			DependencyDeclaration.of(index + 1, statement(index), separated)?.also { before = index }
		}
	}

	/**
	 * What begins on the line at [index] of a block, as one line: that line with those that follow it up to
	 * the parenthesis that closes the one it opens.
	 */
	private fun statement(index: Int): String {
		var open = 0
		val last = (index until code.size).first { line ->
			open += code[line].count { it == '(' } - code[line].count { it == ')' }

			open <= 0 || line == code.lastIndex
		}

		return (index..last).joinToString(" ") { lines[it].trim() }
	}

	/** Every block that the word [name] and a brace open on a line, also one that closes on the same line. */
	private fun blocks(name: String): List<Block> {
		val opening = Regex("""\s*$name\s*\{\s*""")
		val inline = Regex("""\s*$name\s*\{.*\}\s*""")
		val blocks = mutableListOf<Block>()
		var open: Block? = null
		var depth = 0

		code.forEachIndexed { index, line ->
			val block = open
			val directly = block != null && depth == block.depth + 1 && line.isNotBlank() && !line.trimStart().startsWith(CLOSING)
			when {
				block == null && opening.matches(line) -> open = Block(depth, index)
				block == null && inline.matches(line) -> blocks += Block(depth, index)
				block != null && directly -> block.lines += index
			}

			depth += line.count { it in OPENERS } - line.count { it in CLOSERS }
			if (block != null && depth <= block.depth) {
				block.last = index
				blocks += block
				open = null
			}
		}

		return blocks
	}

	/** A block of the file: how many braces and parentheses are open around it, and the lines that stand directly in it. */
	private class Block(
		val depth: Int,
		/** The line the block opens on, by its index. */
		val first: Int,
	) {
		/** The line the block closes on, by its index; a block that is written on one line closes where it opens. */
		var last: Int = first

		/** The lines by their index, without the empty ones and without the one that closes the block. */
		val lines = mutableListOf<Int>()
	}

	/** The scripts of a build that are build files, each by the name it is written as. */
	enum class Script(
		/** What the script is called, `build.gradle.kts`. */
		val fileName: String,
	) {
		/** A project's build script, `build.gradle.kts`: the root's, a module's, or that of a build the build includes. */
		BUILD(NAME),

		/** A build's settings script, `settings.gradle.kts`: the root's, or that of a build the build includes. */
		SETTINGS("settings.gradle.kts");

		companion object {
			/** The script that is called [fileName]. */
			fun named(fileName: String): Script =
				entries.firstOrNull { it.fileName == fileName } ?: throw IllegalArgumentException("$fileName is no script of a build")
		}
	}

	companion object {
		/** What a project's build script is called, and the root build file declarations are written in. */
		const val NAME = "build.gradle.kts"

		/** The folders a build or a tool leaves behind, and git's own: a build file below one is none of the build. */
		val LEFT_BEHIND = setOf("build", ".gradle", ".git", ".kotlin", ".idea", "node_modules")

		private const val PLUGINS = "plugins"
		private const val DEPENDENCIES = "dependencies"
		private const val CONSTRAINTS = "constraints"
		private const val OPENING = "{"
		private const val CLOSING = "}"
		private const val OPENERS = "{("
		private const val CLOSERS = "})"

		/** A string on one line, what stands between its quotes in the group, a quote behind a backslash among it. */
		private val STRING = Regex(""""((?:[^"\\]|\\.)*)"""")

		/**
		 * Reads every build file below [directory], the root project's folder, each script of [Script]: its root's, its
		 * modules', and those of a build it includes.
		 *
		 * @param conventionPlugins the id namespaces of the build's own convention plugins
		 * @return the build files in the order of their paths
		 */
		fun below(directory: File, conventionPlugins: Set<String>): List<BuildFile> = directory.walkTopDown()
			.onEnter { it.name !in LEFT_BEHIND }
			.filter { file -> file.isFile && Script.entries.any { it.fileName == file.name } }
			.map { BuildFile(it.relativeTo(directory).invariantSeparatorsPath, it.readText(), conventionPlugins) }
			.sortedBy(BuildFile::path)
			.toList()
	}
}
