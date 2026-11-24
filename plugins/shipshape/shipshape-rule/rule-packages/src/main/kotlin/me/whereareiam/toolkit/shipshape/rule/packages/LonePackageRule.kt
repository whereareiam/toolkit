package me.whereareiam.toolkit.shipshape.rule.packages

import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.SourceRule
import me.whereareiam.toolkit.shipshape.api.java.JavaSource
import me.whereareiam.toolkit.shipshape.api.java.JavaSources
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement
import me.whereareiam.toolkit.shipshape.api.type.RoleFolder

/**
 * No package is made for one file. The fixed role folders are the exception, because a predictable
 * place is worth more than a saved folder: `config`, `exception`, `model` and `type`. A module's own package
 * is no such folder, nor is one that groups packages below it, nor the package of a test whose class has it.
 */
internal class LonePackageRule : SourceRule {
	override val id = "lone-package"

	override val enforcement = Enforcement.FAILS

	override fun check(sources: JavaSources, context: RuleContext): List<Breach> = sources.modules.flatMap { module ->
		val files = sources.of(module)
		val own = ownPackage(files)
		val folders = files.groupBy(JavaSource::folder)
		val delivered = folders.values.map(List<JavaSource>::first).filter(JavaSource::main).map(JavaSource::packageName).toSet()

		folders.filterValues { it.size == 1 }.values.map(List<JavaSource>::single)
			.filter { it.packageName != own && it.packageName.substringAfterLast('.') !in FIXED }
			.filter { it.main || it.packageName !in delivered }
			.filter { lone -> folders.keys.none { it.startsWith("${lone.folder}/") } }
			.map {
				Breach(
					it.folder, null,
					"is a package made for ${it.typeName} alone; put the file where it belongs, a package is made for two or more",
				)
			}
	}

	/**
	 * The package of a module: the one every package of its main code lies in or below, or of its tests where it has
	 * no main code. `com.example.session` for a module of `com.example.session` and `com.example.session.model`.
	 */
	private fun ownPackage(files: List<JavaSource>): String {
		val packages = files.filter(JavaSource::main).ifEmpty { files }.map { it.packageName.split('.') }.distinct()
		val shared = packages.firstOrNull() ?: return ""
		val length = shared.indices.takeWhile { index -> packages.all { it.getOrNull(index) == shared[index] } }.size

		return shared.take(length).joinToString(".")
	}

	private companion object {
		/** The role folders that exist under the same name everywhere, also with a single file. */
		val FIXED = RoleFolder.entries.filter(RoleFolder::heldByOne).map(RoleFolder::folder).toSet()
	}
}
