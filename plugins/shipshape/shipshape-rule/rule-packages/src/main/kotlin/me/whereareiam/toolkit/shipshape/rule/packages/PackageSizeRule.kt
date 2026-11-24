package me.whereareiam.toolkit.shipshape.rule.packages

import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.SourceRule
import me.whereareiam.toolkit.shipshape.api.java.JavaSource
import me.whereareiam.toolkit.shipshape.api.java.JavaSources
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement

/**
 * No package is a long flat list, in main code and in tests. Once more than seven files lie
 * directly in a package it is grouped below by what the files are about (`model/network`), not by another
 * role word. That holds for every package, the root of an api with its contracts among them.
 */
internal class PackageSizeRule : SourceRule {
	override val id = "package-size"
	override val category = PackageRules.CATEGORY

	override val enforcement = Enforcement.FAILS

	override fun check(sources: JavaSources, context: RuleContext): List<Breach> = sources.all.groupBy(JavaSource::folder)
		.filterValues { it.size > LIMIT }
		.map { (folder, files) ->
			Breach(
				folder, null,
				"holds ${files.size} files directly; a package holds at most $LIMIT, the rest grouped below it by what they are about",
			)
		}

	private companion object {
		/** How many files a package holds directly. */
		const val LIMIT = 7
	}
}
