package me.whereareiam.toolkit.shipshape.api.exemption

import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.SourceRule
import me.whereareiam.toolkit.shipshape.api.java.JavaSources
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.report.Finding
import me.whereareiam.toolkit.shipshape.api.type.Enforcement
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.gradle.api.GradleException
import org.junit.jupiter.api.Test

class DeclaredExemptionsTest {
	@Test
	fun `a declared place excuses the breaches marked with it, and only those`() {
		val exemptions = DeclaredExemptions(listOf(WORKER_DECLARED), BUILD)
		val found = listOf(
			RULE to breach(10, ExemptionPlace(BOUNDARY, WORKER)),
			RULE to breach(20, ExemptionPlace(BOUNDARY, "com.example.Other")),
			RULE to breach(30, null),
		)

		assertThat(exemptions.excuse(found, setOf(BOUNDARY)).map(Finding::sentence)).containsExactly(
			"broad-catch Worker.java:20 catches Exception",
			"broad-catch Worker.java:30 catches Exception",
		)
	}

	@Test
	fun `a declaration that excuses nothing is a breach of unused-exemption at the root build file, which fails`() {
		val exemptions = DeclaredExemptions(listOf(WORKER_DECLARED), BUILD)

		val findings = exemptions.excuse(emptyList(), setOf(BOUNDARY))

		assertThat(findings.map(Finding::sentence)).containsExactly(
			"unused-exemption build.gradle.kts declares catchesEverything(\"$WORKER\") in shipshape, " +
				"and nothing there needs it; remove the declaration",
		)
		assertThat(findings.single().enforcement).isEqualTo(Enforcement.FAILS)
	}

	@Test
	fun `a declaration handed on by another block is named in the block it was written in`() {
		val handed = DeclaredExemptions(listOf(WORKER_DECLARED.copy(block = "exemptions")), BUILD)

		assertThat(handed.excuse(emptyList(), setOf(BOUNDARY)).map(Finding::sentence)).containsExactly(
			"unused-exemption build.gradle.kts declares catchesEverything(\"$WORKER\") in exemptions, " +
				"and nothing there needs it; remove the declaration",
		)
	}

	@Test
	fun `a declaration of a kind the task's rules do not read is left to the task whose rules read it`() {
		val exemptions = DeclaredExemptions(listOf(WORKER_DECLARED), BUILD)

		assertThat(exemptions.excuse(emptyList(), emptySet())).isEmpty()
	}

	@Test
	fun `a rule marks its breaches with the kinds it reads`() {
		val unread = ExemptionKind("ownsDigest")
		val exemptions = DeclaredExemptions(emptyList(), BUILD)

		assertThatThrownBy { exemptions.excuse(listOf(RULE to breach(10, ExemptionPlace(unread, WORKER))), setOf(BOUNDARY)) }
			.isInstanceOf(GradleException::class.java)
			.hasMessage("broad-catch marks a breach with the exemption kind ownsDigest, which it does not read")
	}

	private fun breach(line: Int, exemption: ExemptionPlace?): Breach = Breach("Worker.java", line, "catches Exception", exemption)

	private object Catching : SourceRule {
		override val id = "broad-catch"
		override val enforcement = Enforcement.FAILS
		override val exemptions = setOf(BOUNDARY)

		override fun check(sources: JavaSources, context: RuleContext): List<Breach> = emptyList()
	}

	private companion object {
		val BOUNDARY = ExemptionKind("catchesEverything")
		val RULE: SourceRule = Catching
		const val WORKER = "com.example.DeliveryWorker"
		const val BUILD = "build.gradle.kts"
		val WORKER_DECLARED = ExemptionDeclaration(BOUNDARY, WORKER, "A delivery must not end the worker", "shipshape")
	}
}
