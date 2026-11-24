package me.whereareiam.toolkit.shipshape.rule.packages

import me.whereareiam.toolkit.shipshape.api.RuleCategory
import me.whereareiam.toolkit.shipshape.api.ShipshapeExtension

/** The rules of packages: their size, no package for one file, role folders and `util` packages. */
object PackageRules {
	/** The category the rules lie in, which `verifyPackages` runs. */
	@JvmField
	val CATEGORY = RuleCategory("packages", "Verifies the packages: their size, no package for one file, role folders and util packages.")

	/** Registers the rules with [shipshape]. */
	fun registerWith(shipshape: ShipshapeExtension) {
		shipshape.source(LonePackageRule())
		shipshape.source(PackageSizeRule())
		shipshape.shape(RoleFolderRule())
		shipshape.shape(UtilPackageRule())
	}
}
