package me.whereareiam.toolkit.shipshape.verification

import me.whereareiam.toolkit.shipshape.api.BuildRule
import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.ShapeRule
import me.whereareiam.toolkit.shipshape.api.SourceRule
import me.whereareiam.toolkit.shipshape.api.buildfile.BuildFile
import me.whereareiam.toolkit.shipshape.api.exemption.ExemptionDeclaration
import me.whereareiam.toolkit.shipshape.api.exemption.ExemptionKind
import me.whereareiam.toolkit.shipshape.api.java.JavaSources
import me.whereareiam.toolkit.shipshape.api.java.ParsedSource
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.gradle.api.GradleException
import org.junit.jupiter.api.Test

class RuleRegistryTest {
	private val registry = RuleRegistry()

	@Test
	fun `rules of every kind are kept in the order they were registered, each with the kinds it reads`() {
		registry.register(OWNS_DIGEST)
		registry.register(Source("digest-owner", setOf(OWNS_DIGEST)))
		registry.register(Source("digest-name"))
		registry.register(Shape("class-size"))
		registry.register(Build("plugin-order"))
		registry.declare(OWNS_DIGEST, "com.example.Digest", "The SHA-256 of a token", "exemptions")

		assertThat(registry.sourceRules().map { it.id }).containsExactly("digest-owner", "digest-name")
		assertThat(registry.shapeRules().map { it.id }).containsExactly("class-size")
		assertThat(registry.buildRules().map { it.id }).containsExactly("plugin-order")
		assertThat(registry.declared(setOf(OWNS_DIGEST)))
			.containsExactly(ExemptionDeclaration(OWNS_DIGEST, "com.example.Digest", "The SHA-256 of a token", "exemptions"))
	}

	@Test
	fun `a second rule of one name is refused, whatever it reads`() {
		registry.register(Source("wildcard-import"))

		assertThatThrownBy { registry.register(Shape("wildcard-import")) }
			.isInstanceOf(GradleException::class.java)
			.hasMessageStartingWith("shipshape already has a rule wildcard-import: ")
	}

	@Test
	fun `a rule's id is lower case with hyphens between its words`() {
		assertThatThrownBy { registry.register(Source("Wildcard_Import")) }
			.isInstanceOf(GradleException::class.java)
			.hasMessage("A rule's id is written as wildcard-import, not 'Wildcard_Import'")
	}

	@Test
	fun `no rule takes the id of shipshape's own rule of unused exemptions`() {
		assertThatThrownBy { registry.register(Source("unused-exemption")) }
			.isInstanceOf(GradleException::class.java)
			.hasMessage("shipshape names its own rule unused-exemption")
	}

	@Test
	fun `a second kind of one method is refused`() {
		registry.register(OWNS_DIGEST)

		assertThatThrownBy { registry.register(ExemptionKind("ownsDigest")) }
			.isInstanceOf(GradleException::class.java)
			.hasMessage("shipshape already has the exemption kind ownsDigest")
	}

	@Test
	fun `a declaration names a registered kind`() {
		assertThatThrownBy { registry.declare(OWNS_DIGEST, "com.example.Digest", "why", "shipshape") }
			.hasMessage("shipshape has no exemption kind ownsDigest; register it first")
	}

	@Test
	fun `a rule reads registered kinds only`() {
		registry.register(Source("digest-owner", setOf(OWNS_DIGEST)))

		assertThatThrownBy { registry.sourceRules() }
			.hasMessage("A rule reads the exemption kind ownsDigest, which is not registered with shipshape")
	}

	@Test
	fun `a kind belongs to the rules of one task`() {
		registry.register(OWNS_DIGEST)
		registry.register(Source("digest-owner", setOf(OWNS_DIGEST)))
		registry.register(Shape("digest-shape", setOf(OWNS_DIGEST)))

		assertThatThrownBy { registry.buildRules() }
			.hasMessage("The exemption kind ownsDigest is read by verifyShape and verifySource; a kind belongs to the rules of one task")
	}

	@Test
	fun `a kind no rule reads is refused`() {
		registry.register(OWNS_DIGEST)

		assertThatThrownBy { registry.shapeRules() }
			.hasMessage("No rule reads the exemption kind ownsDigest; a kind is registered with the rule that reads it")
	}

	private class Source(override val id: String, override val exemptions: Set<ExemptionKind> = emptySet()) : SourceRule {
		override val enforcement = Enforcement.FAILS

		override fun check(sources: JavaSources, context: RuleContext): List<Breach> = emptyList()
	}

	private class Shape(override val id: String, override val exemptions: Set<ExemptionKind> = emptySet()) : ShapeRule {
		override val enforcement = Enforcement.FAILS

		override fun check(parsed: ParsedSource, context: RuleContext): List<Breach> = emptyList()
	}

	private class Build(override val id: String) : BuildRule {
		override val enforcement = Enforcement.WARNS

		override fun check(file: BuildFile, context: RuleContext): List<Breach> = emptyList()
	}

	private companion object {
		val OWNS_DIGEST = ExemptionKind("ownsDigest")
	}
}
