package me.whereareiam.toolkit.shipshape.rule.format

import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.SourceRule
import me.whereareiam.toolkit.shipshape.api.java.JavaSources
import me.whereareiam.toolkit.shipshape.api.measure.LineWidth
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement

/** A line is at most [LineWidth.LIMIT] columns wide, in main code and in tests, a comment as much as code. */
internal class LineLengthRule : SourceRule {
	override val id = "line-length"

	override val enforcement = Enforcement.FAILS

	override fun check(sources: JavaSources, context: RuleContext): List<Breach> = sources.all.flatMap { source ->
		LineWidth.tooWide(source.lines).map { (line, width) ->
			Breach(
				source.path, line,
				"is $width columns wide; a line has at most ${LineWidth.LIMIT}, a tab counted as ${LineWidth.TAB}",
			)
		}
	}
}
