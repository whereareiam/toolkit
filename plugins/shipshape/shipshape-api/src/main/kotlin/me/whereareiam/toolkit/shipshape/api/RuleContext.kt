package me.whereareiam.toolkit.shipshape.api

import me.whereareiam.toolkit.architecture.type.ArchitectureKind
import me.whereareiam.toolkit.shipshape.api.java.JavaModule
import me.whereareiam.toolkit.shipshape.api.java.JavaSource

/**
 * What a rule may know of the build beside the file it reads: the role of each module, the packages that are the
 * build's own, and the annotations of its wiring methods.
 *
 * A module's role is its architecture kind, as `me.whereareiam.toolkit.architecture` classifies it: the kind its
 * `architecture` block states, else the one it infers from the module's name, plugins and sources. The architecture
 * plugin need not be applied; only the `architecture` block needs it.
 */
class RuleContext(
	kinds: Map<JavaModule, ArchitectureKind>,
	/** The packages the build's own types lie in or below, `me.whereareiam.anvil`. */
	val packageRoots: Set<String>,
	/**
	 * The simple names of the annotations that mark a wiring method, `Bean`: a method a container calls with the
	 * collaborators it creates, whose parameters are collaborators and whose signature the container dictates.
	 */
	val wiringAnnotations: Set<String>,
) {
	private val kinds: Map<String, ArchitectureKind> = kinds.mapKeys { (module, _) -> module.path }

	/** Every module the build holds, each with its role. */
	val modules: Set<JavaModule> = kinds.keys

	/** The role of [module]. */
	fun kindOf(module: JavaModule): ArchitectureKind =
		kinds[module.path] ?: throw IllegalArgumentException("${module.path} is no module of this build")

	/** The role of the module [source] lies in. */
	fun kindOf(source: JavaSource): ArchitectureKind = kindOf(source.module)

	/** Says whether the type of the full name [type] is one of the build's own: it lies in or below one of [packageRoots]. */
	fun isOwn(type: String): Boolean = packageRoots.any { type.startsWith("$it.") }
}
