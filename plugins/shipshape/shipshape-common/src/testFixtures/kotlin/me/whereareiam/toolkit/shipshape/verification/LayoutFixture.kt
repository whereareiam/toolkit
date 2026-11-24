package me.whereareiam.toolkit.shipshape.verification

import me.whereareiam.toolkit.architecture.type.ArchitectureKind
import me.whereareiam.toolkit.shipshape.api.ShipshapeExtension
import me.whereareiam.toolkit.shipshape.api.BuildRule
import me.whereareiam.toolkit.shipshape.api.Rule
import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.ShapeRule
import me.whereareiam.toolkit.shipshape.api.SourceRule
import me.whereareiam.toolkit.shipshape.api.buildfile.BuildFile
import me.whereareiam.toolkit.shipshape.api.exemption.DeclaredExemptions
import me.whereareiam.toolkit.shipshape.api.exemption.ExemptionDeclarations
import me.whereareiam.toolkit.shipshape.api.exemption.ExemptionKind
import me.whereareiam.toolkit.shipshape.api.java.JavaModule
import me.whereareiam.toolkit.shipshape.api.java.JavaSources
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.verification.task.JdkParser
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText

/**
 * A build laid out in a folder for a rule to read, without Gradle: its modules with their architecture kinds, its
 * files, and what its root build file excuses. The root project is an assembly unless a test says otherwise.
 */
class LayoutFixture(
	private val directory: Path,
	private val name: String,
	rootKind: ArchitectureKind = ArchitectureKind.ASSEMBLY,
	private val packageRoots: Set<String> = setOf(PACKAGE_ROOT),
	private val wiringAnnotations: Set<String> = setOf(WIRING),
) {
	private val modules = linkedMapOf(JavaModule(":", "", name) to rootKind)
	private val excused = ExemptionDeclarations()

	/** Adds a module at the folder its [path] names. */
	fun module(path: String, kind: ArchitectureKind): LayoutFixture = apply {
		val folder = path.trimStart(':').replace(':', '/')
		modules[JavaModule(path, folder, path.substringAfterLast(':'))] = kind
	}

	/** Writes a file by its path from the root project's folder. */
	fun file(path: String, text: String): LayoutFixture = apply {
		directory.resolve(path).createParentDirectories().writeText(text)
	}

	/**
	 * Writes a Java class at the place its package and name give it in a source set of a module, by the module's folder;
	 * the root is an empty [module].
	 *
	 * @param body what follows the line of the package; a class without a member where none is given
	 */
	fun java(
		module: String,
		sourceSet: String,
		type: String,
		body: String = "public class ${type.substringAfterLast('.')} {\n}\n",
	): LayoutFixture {
		val folder = if (module.isEmpty()) "" else "$module/"
		val packageName = type.substringBeforeLast('.')

		return file("${folder}src/$sourceSet/java/${type.replace('.', '/')}.java", "package $packageName;\n\n$body")
	}

	/** States what the root build file excuses of [kind]: each place with its reason. */
	fun excusing(kind: ExemptionKind, vararg declared: Pair<String, String>): LayoutFixture = apply {
		declared.forEach { (place, reason) -> excused.declare(kind, place, reason, ShipshapeExtension.NAME) }
	}

	/** What [rule] finds in the text of the build's sources, each breach as the line a build prints. */
	fun breaches(rule: SourceRule): List<String> = sentences(rule, rule.check(repository(), context()))

	/** What [rule] finds in the build's sources as the JDK's parser reads them, each breach as the line a build prints. */
	fun breaches(rule: ShapeRule): List<String> {
		val context = context()

		return sentences(rule, JdkParser(null).parse(repository().all).flatMap { rule.check(it, context) })
	}

	/** The build's sources as a rule reads them; each call reads the folder anew. */
	fun repository(): JavaSources = JavaSources(directory.toFile(), modules.keys.toList())

	/** What a rule may know of the build. */
	fun context(): RuleContext = RuleContext(modules, packageRoots, wiringAnnotations)

	/** [found] as a build prints it: excused as the root build file declares, and each breach as a line. */
	private fun sentences(rule: Rule, found: List<Breach>): List<String> =
		DeclaredExemptions(excused.of(rule.exemptions), BuildFile.NAME).excuse(found.map { rule to it }, rule.exemptions)
			.map { it.sentence }

	companion object {
		/** The package root of the builds the fixtures lay out. */
		const val PACKAGE_ROOT = "com.example"

		/** The annotation of a wiring method in the builds the fixtures lay out. */
		const val WIRING = "Bean"

		/** What [rule] finds in [file], each breach as the line a build prints. */
		fun breaches(rule: BuildRule, file: BuildFile): List<String> {
			val context = RuleContext(emptyMap(), setOf(PACKAGE_ROOT), setOf(WIRING))

			return rule.check(file, context).map { "${rule.id} ${it.place} ${it.asks}" }
		}

		/** `session-service`: an API, its logic, an adapter and an integration, below a root that assembles them. */
		fun service(directory: Path): LayoutFixture = LayoutFixture(directory, "session-service")
			.module(":session-api", ArchitectureKind.API)
			.module(":session-common", ArchitectureKind.IMPLEMENTATION)
			.module(":session-adapter:adapter-grpc", ArchitectureKind.IMPLEMENTATION)
			.module(":session-integration:integration-account", ArchitectureKind.IMPLEMENTATION)
	}
}
