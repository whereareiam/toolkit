package me.whereareiam.toolkit.shipshape.rule.lombok

import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.SourceRule
import me.whereareiam.toolkit.shipshape.api.java.JavaSources
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement

/**
 * A build has one accessor style, Lombok's ordinary getters (`getUserId()`), set once for the whole build
 * in its `lombok.config`. No file sets a style of its own with `@Accessors`, in main code or in tests; a record keeps
 * the accessors of its components.
 */
internal class AccessorsAnnotationRule : SourceRule {
	override val id = "accessors-annotation"
	override val category = LombokRules.CATEGORY

	override val enforcement = Enforcement.FAILS

	override fun check(sources: JavaSources, context: RuleContext): List<Breach> = sources.all.flatMap { source ->
		source.find(ACCESSORS).map {
			Breach(
				source.path, it.line,
				"sets its own accessor style with @Accessors; remove it, an accessor is Lombok's getter everywhere",
			)
		}
	}

	private companion object {
		val ACCESSORS = Regex("""@(?:lombok\.experimental\.)?Accessors(?!\w)""")
	}
}
