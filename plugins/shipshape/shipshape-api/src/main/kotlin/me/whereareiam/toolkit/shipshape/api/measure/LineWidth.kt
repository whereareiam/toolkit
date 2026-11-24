package me.whereareiam.toolkit.shipshape.api.measure

/**
 * How wide a line of source is, as `line-length` measures it: a tab counts as four columns, which is what an `.editorconfig`
 * with a tab width of four shows it as, and a line has at most 140. A build that holds files shipshape does not read,
 * such as its Kotlin, to the same width measures them here.
 */
object LineWidth {
	/** How many columns a line may have. */
	const val LIMIT = 140

	/** How many columns a tab counts as. */
	const val TAB = 4

	/** The width of [line] in columns. */
	fun of(line: String): Int = line.sumOf { if (it == '\t') TAB else 1 }

	/**
	 * Finds the lines that are wider than [LIMIT].
	 *
	 * @param lines the lines of a file, in their order
	 * @return each such line as its number, counted from 1, with its width in columns
	 */
	fun tooWide(lines: List<String>): List<Pair<Int, Int>> = lines.mapIndexedNotNull { index, line ->
		val width = of(line)

		(index + 1 to width).takeIf { width > LIMIT }
	}
}
