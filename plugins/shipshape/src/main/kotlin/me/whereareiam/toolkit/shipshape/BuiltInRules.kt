package me.whereareiam.toolkit.shipshape

import me.whereareiam.toolkit.shipshape.api.ShipshapeExtension
import me.whereareiam.toolkit.shipshape.rule.buildfile.BuildFileRules
import me.whereareiam.toolkit.shipshape.rule.documentation.DocumentationRules
import me.whereareiam.toolkit.shipshape.rule.flow.FlowRules
import me.whereareiam.toolkit.shipshape.rule.format.FormatRules
import me.whereareiam.toolkit.shipshape.rule.imports.ImportRules
import me.whereareiam.toolkit.shipshape.rule.language.LanguageRules
import me.whereareiam.toolkit.shipshape.rule.lombok.LombokRules
import me.whereareiam.toolkit.shipshape.rule.models.ModelRules
import me.whereareiam.toolkit.shipshape.rule.packages.PackageRules
import me.whereareiam.toolkit.shipshape.rule.size.SizeRules
import me.whereareiam.toolkit.shipshape.rule.tests.TestRules
import me.whereareiam.toolkit.shipshape.rule.types.TypeRules

/** The subjects of rules shipshape holds every build to, each of which registers its own rules. */
internal object BuiltInRules {
	/** Registers every subject's rules and the exemption kinds they read with [extension]. */
	fun registerWith(extension: ShipshapeExtension) {
		BuildFileRules.registerWith(extension)
		DocumentationRules.registerWith(extension)
		FlowRules.registerWith(extension)
		FormatRules.registerWith(extension)
		ImportRules.registerWith(extension)
		LanguageRules.registerWith(extension)
		LombokRules.registerWith(extension)
		ModelRules.registerWith(extension)
		PackageRules.registerWith(extension)
		SizeRules.registerWith(extension)
		TestRules.registerWith(extension)
		TypeRules.registerWith(extension)
	}
}
