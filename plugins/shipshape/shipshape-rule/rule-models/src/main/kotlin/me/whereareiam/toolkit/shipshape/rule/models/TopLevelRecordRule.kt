package me.whereareiam.toolkit.shipshape.rule.models

import com.sun.source.tree.Tree
import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.ShapeRule
import me.whereareiam.toolkit.shipshape.api.java.ParsedSource
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement

/**
 * No top-level `record`, in main code and in tests. A model is a class of its own, and a record stays a small
 * carrier inside its owner, which [NestedRecordRule] holds.
 */
internal class TopLevelRecordRule : ShapeRule {
	override val id = "top-level-record"

	override val enforcement = Enforcement.FAILS

	override fun check(parsed: ParsedSource, context: RuleContext): List<Breach> =
		parsed.topLevel.filter { it.kind == Tree.Kind.RECORD }.map {
			Breach(
				parsed.source.path, null,
				"declares a top-level record; write a class, or nest the record in the type it belongs to",
			)
		}
}
