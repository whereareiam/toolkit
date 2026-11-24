package me.whereareiam.toolkit.shipshape.api.measure

/**
 * The order `import-order` asks of a file's imports: sorted alphabetically in one group, then `java.*` and `javax.*` in a second
 * group after a blank line, then the static imports of tests after another; no blank line stands inside a group. A
 * module import (`import module java.base;`) stands first. A build that writes Java, as a test that moves a sample's
 * packages does, puts the imports it changed in this order.
 */
object ImportOrder {
	private val IMPORT = Regex("""import\s+(?:(static|module)\s+)?([\w.]+(?:\.\*)?)\s*;""")
	private const val KIND = 1
	private const val NAME = 2
	private val JDK = listOf("java.", "javax.")

	/** Whether [line], trimmed, is one import. */
	fun isImport(line: String): Boolean = IMPORT.matches(line)

	/**
	 * How [written], the lines of an import block, stand in their order: each group sorted, a blank line between two
	 * groups.
	 */
	fun ordered(written: List<String>): List<String> {
		val imports = written.map(String::trim).filter(String::isNotEmpty)

		return Group.entries.map { group -> imports.filter { Group.of(it) == group }.sortedBy(::named) }
			.filter(List<String>::isNotEmpty)
			.flatMapIndexed { index, group -> if (index == 0) group else listOf("") + group }
	}

	/**
	 * [java], the text of a Java file, with the block from its first import to its last in the order [ordered] gives.
	 * A file without imports is returned as it is.
	 */
	fun sorted(java: String): String {
		val lines = java.lines()
		val first = lines.indexOfFirst { isImport(it.trim()) }.takeIf { it >= 0 } ?: return java
		val last = lines.indexOfLast { isImport(it.trim()) }

		return (lines.take(first) + ordered(lines.subList(first, last + 1)) + lines.drop(last + 1)).joinToString("\n")
	}

	private fun named(line: String): String = IMPORT.matchEntire(line)?.groupValues?.get(NAME).orEmpty()

	/** The groups of imports, in their order. */
	private enum class Group {
		MODULE,
		OWN,
		JAVA,
		STATIC;

		companion object {
			fun of(line: String): Group {
				val match = IMPORT.matchEntire(line) ?: return OWN
				if (match.groupValues[KIND] == "module") return MODULE
				if (match.groupValues[KIND] == "static") return STATIC

				return if (JDK.any(match.groupValues[NAME]::startsWith)) JAVA else OWN
			}
		}
	}
}
