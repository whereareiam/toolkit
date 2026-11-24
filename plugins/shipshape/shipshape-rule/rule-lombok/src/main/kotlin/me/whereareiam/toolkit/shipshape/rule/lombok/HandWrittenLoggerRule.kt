package me.whereareiam.toolkit.shipshape.rule.lombok

import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.SourceRule
import me.whereareiam.toolkit.shipshape.api.java.JavaSource
import me.whereareiam.toolkit.shipshape.api.java.JavaSources
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement

/** Main code does not declare its logger by hand; Lombok's `@Slf4j` writes it, under one name everywhere. */
internal class HandWrittenLoggerRule : SourceRule {
	override val id = "hand-written-logger"
	override val category = LombokRules.CATEGORY

	override val enforcement = Enforcement.FAILS

	override fun check(sources: JavaSources, context: RuleContext): List<Breach> = sources.all.filter(JavaSource::main).flatMap { source ->
		source.find(LOGGER).map {
			Breach(source.path, it.line, "declares the logger ${it.group(1)} by hand; @Slf4j writes it")
		}
	}

	private companion object {
		val LOGGER = Regex("""\bLogger\s+(\w+)\s*=\s*LoggerFactory\s*\.\s*getLogger\s*\(""")
	}
}
