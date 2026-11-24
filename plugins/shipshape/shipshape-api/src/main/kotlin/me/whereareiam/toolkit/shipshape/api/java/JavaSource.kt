package me.whereareiam.toolkit.shipshape.api.java

import me.whereareiam.toolkit.shipshape.api.type.RoleFolder
import java.io.File

/** One Java file as the rules read it. */
class JavaSource(
	val file: File,
	private val sourceRoot: File,
	/** The root project's folder, which [path] is taken from. */
	directory: File,
	/** The module the file lies in. */
	val module: JavaModule,
) {
	private val written: String by lazy { file.readText() }

	/** The file's path from the root project's folder, for a breach to name. */
	val path: String = file.relativeTo(directory).invariantSeparatorsPath

	/** The name of the type the file declares. */
	val typeName: String = file.nameWithoutExtension

	/** The file as it is written, line by line, its comments included. */
	val lines: List<String> by lazy { written.lines() }

	/** The file without comments; the text of its string literals is kept. */
	val text: String by lazy { SourceText.strip(written, literals = true) }

	/** The file without comments and without the text of its literals: what the compiler takes for code. */
	val code: String by lazy { SourceText.strip(written, literals = false) }

	/** The package the file declares, or an empty string. */
	val packageName: String by lazy { PACKAGE.find(code)?.groupValues?.get(1).orEmpty() }

	/** What the file imports, a wildcard import as `some.package.*`. */
	val imports: List<String> by lazy { IMPORT.findAll(code).map { it.groupValues[1] }.toList() }

	/** The full name of the type the file declares. */
	val qualifiedName: String
		get() = if (packageName.isEmpty()) typeName else "$packageName.$typeName"

	/** The package its folder below the source root stands for. */
	val folderPackage: String
		get() = file.parentFile.relativeTo(sourceRoot).invariantSeparatorsPath.replace('/', '.')

	/** The folder of the file by its path from the root project's folder, which is how a breach names a package. */
	val folder: String
		get() = path.substringBeforeLast('/')

	/** The source set the file belongs to, `main` or `test`. */
	val sourceSet: String
		get() = sourceRoot.parentFile.name

	/** Whether the file is delivered: of the source set `main`. Every other source set holds the code of tests. */
	val main: Boolean
		get() = isOf(MAIN)

	/** Whether the type is a mapper by its name: a class that turns the types of one family into those of another. */
	val mapper: Boolean
		get() = typeName.endsWith(MAPPER)

	/** Says whether the file is of the source set [name], `main` or `test`. */
	fun isOf(name: String): Boolean = sourceSet == name

	/** Says whether the file lies in the role folder [folder] of its module, or below it. */
	fun isBelow(folder: RoleFolder): Boolean = folder.folder in packageName.split('.')

	/** The folder a file of [packageName] has in the source set [name] of the same module. */
	fun siblingFolder(name: String): File =
		sourceRoot.parentFile.parentFile.resolve("$name/java/${packageName.replace('.', '/')}")

	/**
	 * Finds [pattern] in the file without its comments.
	 *
	 * @param literals whether the text of string literals is read as well; without it a literal is its two
	 * quotes, and nothing written inside one is taken for code
	 * @return every match with the line it begins on, in the order of the file
	 */
	fun find(pattern: Regex, literals: Boolean = false): List<Occurrence> {
		val read = if (literals) text else code
		var line = 1
		var counted = 0

		return pattern.findAll(read).map { match ->
			line += read.substring(counted, match.range.first).count { it == '\n' }
			counted = match.range.first

			Occurrence(line, match)
		}.toList()
	}

	/** One match of a pattern in the file. */
	class Occurrence(
		/** The line the match begins on, counted from 1. */
		val line: Int,
		val match: MatchResult,
	) {
		/** What the group [index] of the pattern matched, or an empty string. */
		fun group(index: Int): String = match.groupValues[index]
	}

	companion object {
		/** The source set of what is delivered. */
		const val MAIN = "main"

		/** The source set of the tests of a module. */
		const val TEST = "test"

		/** What the name of a mapper ends in. */
		const val MAPPER = "Mapper"

		private val PACKAGE = Regex("""(?m)^\s*package\s+([\w.]+)\s*;""")
		private val IMPORT = Regex("""(?m)^\s*import\s+(?:static\s+)?([\w.]+(?:\.\*)?)\s*;""")
	}
}
