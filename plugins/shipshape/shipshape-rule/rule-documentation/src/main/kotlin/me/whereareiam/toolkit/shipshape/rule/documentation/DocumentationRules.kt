package me.whereareiam.toolkit.shipshape.rule.documentation

import me.whereareiam.toolkit.shipshape.api.ShipshapeExtension

/** The rules of documentation: a Javadoc on every type, and examples in `<pre>{@code …}</pre>`. */
object DocumentationRules {
	/** Registers the rules with [shipshape]. */
	fun registerWith(shipshape: ShipshapeExtension) {
		shipshape.source(SnippetTagRule())
		shipshape.source(TypeJavadocRule())
	}
}
