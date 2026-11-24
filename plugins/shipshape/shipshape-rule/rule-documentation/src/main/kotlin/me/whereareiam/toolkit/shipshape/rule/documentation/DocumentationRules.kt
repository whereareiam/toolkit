package me.whereareiam.toolkit.shipshape.rule.documentation

import me.whereareiam.toolkit.shipshape.api.RuleCategory
import me.whereareiam.toolkit.shipshape.api.ShipshapeExtension

/** The rules of documentation: a Javadoc on every type, and examples in `<pre>{@code …}</pre>`. */
object DocumentationRules {
	/** The category the rules lie in, which `verifyDocumentation` runs. */
	@JvmField
	val CATEGORY = RuleCategory("documentation", "Verifies the Javadoc: one on every type, and examples as <pre>{@code …}</pre>.")

	/** Registers the rules with [shipshape]. */
	fun registerWith(shipshape: ShipshapeExtension) {
		shipshape.source(SnippetTagRule())
		shipshape.source(TypeJavadocRule())
	}
}
