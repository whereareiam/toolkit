package me.whereareiam.toolkit.shipshape.rule.buildfile

import me.whereareiam.toolkit.shipshape.api.BuildRule
import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.buildfile.BuildFile
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement

/**
 * A build file names its libraries from the version catalog (`gradle/libs.versions.toml`) and writes no coordinates with
 * a version, so that every version has one place. A string that holds a group, a name and a version, `"group:name:1.0"`,
 * is refused wherever the build script or the settings script writes it, the version also taken from a value
 * (`"group:name:$version"`), and so is a declaration of a block `dependencies` that names its library by `group`, `name`
 * and `version`. A comment is not read, and neither is a library written without a version, whose version a platform or
 * a constraint gives, nor the module a plugin's request is resolved to at the version it asks for
 * (`useModule("group:name:${requested.version}")`), which the `plugins` block takes from the catalog.
 */
internal class CatalogCoordinateRule : BuildRule {
	override val id = "catalog-coordinate"
	override val category = BuildFileRules.CATEGORY

	override val enforcement = Enforcement.FAILS

	override val scripts = BuildFile.Script.entries.toSet()

	override fun check(file: BuildFile, context: RuleContext): List<Breach> {
		val written = file.strings.filter { COORDINATE.matches(it.value) && !REQUESTED.containsMatchIn(it.value) }
		val writtenModules = written.map { it.value.split(':').take(2).joinToString(":") }.toSet()
		val named = (file.dependencies.flatten() + file.constraints).mapNotNull { declared ->
			declared.coordinate?.takeIf { declared.version != null && declared.module !in writtenModules }
				?.let { IndexedValue(declared.line, it) }
		}

		return (written + named).sortedBy(IndexedValue<String>::index)
			.map { Breach(file.path, it.index, "writes out ${it.value} with its version; $ASKED") }
	}

	private companion object {
		const val ASKED = "name the library from the version catalog, gradle/libs.versions.toml, so that every version has one place"

		/**
		 * Coordinates with a version: a group and a name that are written out, and a version that begins as one does,
		 * with a digit, a range, or a value it is taken from.
		 */
		val COORDINATE = Regex("""[A-Za-z0-9_.-]+:[A-Za-z0-9_.-]+:(?:[0-9\[(]|\$).*""")

		/** The version a plugin's request asks for, which its alias in the catalog gives, in a resolution strategy. */
		val REQUESTED = Regex(""":\$\{requested\.version}$""")
	}
}
