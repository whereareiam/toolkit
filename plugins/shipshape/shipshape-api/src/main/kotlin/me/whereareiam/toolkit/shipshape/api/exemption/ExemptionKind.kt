package me.whereareiam.toolkit.shipshape.api.exemption

import me.whereareiam.toolkit.shipshape.api.report.Breach
import java.io.Serializable

/**
 * A kind of place a build may excuse from a rule, as `catchesEverything` excuses a class from `broad-catch`. A rule names the
 * kinds it reads ([me.whereareiam.toolkit.shipshape.api.Rule.exemptions]) and marks each breach a declaration of
 * one could excuse ([Breach.exemption]). A declaration ([ExemptionDeclaration]) names one place with its reason, never a
 * whole rule, and one that excuses nothing is a breach itself. The kind says nothing of the block it is declared in: a
 * block of another plugin may hand its declarations on, and the declaration names the block it was written in.
 */
data class ExemptionKind(
	/** The method a build file declares a place of the kind with, `catchesEverything`; unique among the kinds of a build. */
	val method: String,
) : Serializable {
	init {
		require(METHOD.matches(method)) { "An exemption kind is named as a method, catchesEverything, not '$method'" }
	}

	/** How a build file declares [place], as a breach tells the reader who may excuse it: `catchesEverything("…", why)`. */
	fun declaration(place: String): String = "$method(\"$place\", why)"

	override fun toString(): String = method

	companion object {
		private const val serialVersionUID = 1L

		private val METHOD = Regex("[a-z][A-Za-z0-9]*")
	}
}
