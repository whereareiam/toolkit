package me.whereareiam.toolkit.shipshape.api

import java.io.Serializable

/**
 * What a rule is about, `imports` or `lombok`: the rules of one category run in one task, `verifyImports`, which reads
 * whatever its rules read, and report together in `build/verify-imports/report.txt`. The built-in rules name the
 * categories of their subjects; a plugin's rule names one of those, such as `ImportRules.CATEGORY`, or a category of
 * its own.
 *
 * ```kotlin
 * val DIGESTS = RuleCategory("digests", "Verifies who computes a digest.")
 * ```
 */
data class RuleCategory(
	/** The category's id, a word or words in lower case with hyphens between them: `imports`, `build-files`. */
	val id: String,
	/** What the category's task verifies, as the task's description says it. */
	val description: String,
) : Serializable {
	init {
		require(ID.matches(id)) { "A category's id is written as imports or build-files, not '$id'" }
		require(description.isNotBlank()) { "The category $id says what its task verifies" }
	}

	/** The name of the category's task: `verify` and its id in camel case, `verifyBuildFiles`. */
	val taskName: String
		get() = "verify" + id.split('-').joinToString("") { word -> word.replaceFirstChar(Char::uppercaseChar) }

	override fun toString(): String = id

	private companion object {
		private const val serialVersionUID = 1L

		val ID = Regex("[a-z][a-z0-9]*(-[a-z0-9]+)*")
	}
}
