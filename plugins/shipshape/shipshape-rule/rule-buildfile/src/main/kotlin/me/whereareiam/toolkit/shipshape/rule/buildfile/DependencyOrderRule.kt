package me.whereareiam.toolkit.shipshape.rule.buildfile

import me.whereareiam.toolkit.shipshape.api.BuildRule
import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.buildfile.BuildFile
import me.whereareiam.toolkit.shipshape.api.buildfile.DependencyDeclaration
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement

/**
 * The dependencies of a block stand in the order of java-conventions, "Gradle script style": the roles as
 * `api`, `implementation`, `compileOnly`, `embedded`, `runtimeOnly`, `annotationProcessor`, those of the main
 * code before those of the test fixtures and those of the tests; in a role the projects before what the
 * catalog names; and each of the two sorted by what it names.
 *
 * A configuration a build file makes itself has no place in that order and is not held. Neither is a library
 * that is written out by its coordinates: it has a place in the order once the catalog names it.
 */
internal class DependencyOrderRule : BuildRule {
	override val id = "dependency-order"
	override val category = BuildFileRules.CATEGORY

	override val enforcement = Enforcement.FAILS

	override fun check(file: BuildFile, context: RuleContext): List<Breach> = file.dependencies.flatMap { block ->
		val walk = Walk()

		block.mapNotNull { declared ->
			val next = place(declared.configuration) ?: return@mapNotNull null

			walk.misplaced(declared, next)?.let { Breach(file.path, declared.line, it) }
		}
	}

	/** Goes through the declarations of one block and keeps what the place of the next one depends on. */
	private class Walk {
		private var configuration: String? = null
		private var place = 0
		private var project: String? = null
		private var accessor: String? = null

		/**
		 * Takes the next declaration of the block.
		 *
		 * @param next the place of its configuration in the order of the roles
		 * @return what is out of order with it, or `null`
		 */
		fun misplaced(declared: DependencyDeclaration, next: Int): String? {
			val before = configuration
			val early = before != null && next < place
			if (declared.configuration != before) {
				configuration = declared.configuration
				project = null
				accessor = null
			}
			place = next

			return if (early) "declares ${declared.configuration} behind $before; $ROLES" else unsorted(declared)
		}

		/** Holds [declared] against what its role named before it: a project, or an alias of the catalog. */
		private fun unsorted(declared: DependencyDeclaration): String? {
			val named = declared.project ?: declared.accessor ?: return null
			val last = if (declared.project != null) project else accessor
			val sentence = when {
				declared.project != null && accessor != null -> "names the project $named behind $accessor; $PROJECTS_FIRST"
				last != null && named < last -> "names $named behind $last; $SORTED"
				else -> null
			}
			if (declared.project != null) project = named else accessor = named

			return sentence
		}
	}

	private companion object {
		const val ROLES = "the roles stand as api, implementation, compileOnly, embedded, runtimeOnly, annotationProcessor: " +
			"of the main code, then of the test fixtures, then of the tests"
		const val PROJECTS_FIRST = "in a role the projects stand before what the catalog names"
		const val SORTED = "in a role the projects, and then what the catalog names, are sorted by the alphabet"

		/** The roles in their order. `compileOnlyApi` stands with `compileOnly`. */
		val ORDER = listOf("api", "implementation", "compileOnly", "embedded", "runtimeOnly", "annotationProcessor")
		const val COMPILE_ONLY_API = "compileOnlyApi"

		/** What the configurations of a source set begin with, in the order of the source sets; the main code's begin with nothing. */
		val SOURCE_SETS = listOf("", "testFixtures", "test")

		/**
		 * The place of [configuration] in the order: its source set first, then its role.
		 *
		 * @return the place, or `null` for a configuration that is none of a known source set's roles
		 */
		fun place(configuration: String): Int? = SOURCE_SETS.indices.reversed().firstNotNullOfOrNull { sourceSet ->
			val prefix = SOURCE_SETS[sourceSet]
			val role = configuration.takeIf { it.startsWith(prefix) }?.removePrefix(prefix)
				?.let { if (prefix.isEmpty()) it else it.replaceFirstChar(Char::lowercaseChar) }
				?.let { if (it == COMPILE_ONLY_API) "compileOnly" else it }

			role?.let(ORDER::indexOf)?.takeIf { it >= 0 }?.let { sourceSet * ORDER.size + it }
		}
	}
}
