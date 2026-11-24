package me.whereareiam.toolkit.shipshape.api.java

/** Reads Java and Kotlin source as the checks need it: without what a comment or a literal only mentions. */
object SourceText {
	/**
	 * Removes every comment from [text] and keeps its lines.
	 *
	 * @param literals whether the content of string and character literals is kept; without it a literal
	 * is left as its two quotes, so that nothing written inside one is taken for code
	 */
	fun strip(text: String, literals: Boolean): String = read(text, code = true, literals = literals)

	/**
	 * Keeps what [text] writes inside its string and character literals and nothing else, each on the line it
	 * is written on. Where a literal began or ended stands a space, so two literals never run into one word.
	 */
	fun literals(text: String): String = read(text, code = false, literals = true)

	/** Returns what stands outside of every pair of braces in [code], which is where top-level types are declared. */
	fun topLevel(code: String): String {
		val result = StringBuilder()
		var depth = 0
		for (current in code) {
			when {
				current == '{' -> depth++
				current == '}' -> {
					depth = maxOf(0, depth - 1)
					result.append(' ')
				}
				depth == 0 -> result.append(current)
			}
		}

		return result.toString()
	}

	/** Walks [text] once, without its comments: [code] keeps what stands outside of literals, [literals] what stands inside. */
	private fun read(text: String, code: Boolean, literals: Boolean): String {
		val result = StringBuilder(text.length)
		var index = 0
		while (index < text.length) {
			val rest = text.length - index
			val current = text[index]
			when {
				rest >= 2 && current == '/' && text[index + 1] == '/' -> {
					index = text.indexOf('\n', index).takeIf { it >= 0 } ?: text.length
				}
				rest >= 2 && current == '/' && text[index + 1] == '*' -> {
					val end = text.indexOf("*/", index + 2).takeIf { it >= 0 }?.plus(2) ?: text.length
					result.append(' ').append(text.substring(index, end).filter { it == '\n' })
					index = end
				}
				text.startsWith("\"\"\"", index) -> {
					val end = text.indexOf("\"\"\"", index + 3).takeIf { it >= 0 }?.plus(3) ?: text.length
					result.append(if (code) '"' else ' ')
					if (literals) result.append(text, index + 3, maxOf(index + 3, end - 3))
					else result.append(text.substring(index, end).filter { it == '\n' })
					result.append(if (code) '"' else ' ')
					index = end
				}
				current == '"' || current == '\'' -> {
					val end = literalEnd(text, index)
					result.append(if (code) current else ' ')
					if (literals) result.append(text, index + 1, maxOf(index + 1, end - 1))
					result.append(if (code) current else ' ')
					index = end
				}
				else -> {
					if (code || current == '\n') result.append(current)
					index++
				}
			}
		}

		return result.toString()
	}

	/** The index after the quote that closes the literal opened at [start], on the same line at the latest. */
	private fun literalEnd(text: String, start: Int): Int {
		val quote = text[start]
		var index = start + 1
		while (index < text.length) {
			when (text[index]) {
				'\\' -> index++
				'\n' -> return index
				quote -> return index + 1
			}
			index++
		}

		return text.length
	}
}
