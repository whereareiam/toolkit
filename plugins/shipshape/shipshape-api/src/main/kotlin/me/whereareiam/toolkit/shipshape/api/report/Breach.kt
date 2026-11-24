package me.whereareiam.toolkit.shipshape.api.report

import me.whereareiam.toolkit.shipshape.api.exemption.ExemptionPlace

/** One place of a build that breaks a rule. The rule that found it is the rule a report names it with. */
class Breach(
	/** The file or the folder by its path from the root project's folder, with `/` between its parts. */
	val path: String,
	/** The line in the file, counted from 1; `null` where the whole file or a folder is the place. */
	val line: Int?,
	/** What the place does and what is asked instead, as the rest of the sentence that begins with the place. */
	val asks: String,
	/** The place a declaration could excuse the breach by, or `null` where nothing excuses it. */
	val exemption: ExemptionPlace? = null,
) {
	/** The place as a build names it: the path, with the line where there is one. */
	val place: String
		get() = line?.let { "$path:$it" } ?: path

	override fun toString(): String = "$place $asks"
}
