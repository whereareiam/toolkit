package me.whereareiam.toolkit.shipshape.api

import me.whereareiam.toolkit.shipshape.api.exemption.ExemptionKind
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

/**
 * The `shipshape` block of the root build file: what the rules may know of the build, the rules another plugin adds,
 * and the places the build excuses from a rule, each with its reason.
 *
 * ```kotlin
 * shipshape {
 * 	packageRoots.add("com.example")
 * 	exempt(LanguageRules.CATCHES_EVERYTHING, "com.example.delivery.DeliveryWorker", "A delivery that fails must not end the worker")
 * }
 * ```
 *
 * A plugin adds its rules and the exemption kinds they read through the same block:
 *
 * ```kotlin
 * project.extensions.configure<ShipshapeExtension> {
 * 	exemptionKind(OWNS_DIGEST)
 * 	source(DigestOwnerRule())
 * }
 * ```
 */
interface ShipshapeExtension {
	/**
	 * The packages the build's own types lie in or below, `com.example`; by default the root project's group. A rule
	 * tells the build's own types from foreign ones by them, as `mocked-own-type` does of what Mockito stands in for.
	 */
	val packageRoots: SetProperty<String>

	/**
	 * The simple names of the annotations that mark a wiring method, `Bean`: a method a container calls with the
	 * collaborators it makes. Its parameters are collaborators (`collaborators`) and its signature is the container's
	 * (`stated-nullability`).
	 */
	val wiringAnnotations: SetProperty<String>

	/**
	 * The id namespaces of the build's own convention plugins, `com.example.convention`: an id that is one of them or
	 * lies below one is a convention, as an id of one word always is (`plugin-order`).
	 */
	val conventionPlugins: SetProperty<String>

	/**
	 * The language level the Java sources are written in. The shape of the classes is parsed at that level, and no rule
	 * of the shape is held, with a warning, where Gradle runs on an older JDK. Without it the sources are read at the
	 * level of the JDK that runs Gradle.
	 */
	val javaRelease: Property<Int>

	/**
	 * Adds a rule that reads the shape of the Java classes to the task of its category.
	 *
	 * @throws GradleException when a rule of the same name is registered already
	 */
	fun shape(rule: ShapeRule)

	/**
	 * Adds a rule that reads the Java source as text to the task of its category.
	 *
	 * @throws GradleException when a rule of the same name is registered already
	 */
	fun source(rule: SourceRule)

	/**
	 * Adds a rule that reads the build files to the task of its category.
	 *
	 * @throws GradleException when a rule of the same name is registered already
	 */
	fun build(rule: BuildRule)

	/**
	 * Registers a kind of place a rule reads declarations of. Each kind is read by the rules of one category.
	 *
	 * @throws GradleException when a kind of the same method is registered already
	 */
	fun exemptionKind(kind: ExemptionKind)

	/**
	 * Excuses [place] from the rules that read [kind]. A declaration names one place, never a whole rule, and one that
	 * excuses nothing fails the build (`unused-exemption`).
	 *
	 * @param place the place as the kind names it: a class by its full name, a module by its Gradle path
	 * @param reason why the rule does not hold there; it is what a reader of the build file is told
	 * @param block the block of the root build file the declaration is written in: `shipshape`, or the block of a
	 * plugin that hands its declarations on, which a breach then names
	 * @throws GradleException when the kind is not registered, the place or the reason is blank, or the place is
	 * declared twice
	 */
	fun exempt(kind: ExemptionKind, place: String, reason: String, block: String = NAME)


	companion object {
		/** The name of the block in the root build file, and of the extension. */
		const val NAME = "shipshape"
	}
}
