package me.whereareiam.toolkit.shipshape.api.exemption

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ExemptionDeclarationsTest {
	private val declarations = ExemptionDeclarations()

	@Test
	fun `the declarations of the asked kinds are given by kind and place, each with its reason and its block`() {
		declarations.declare(OWNS_DIGEST, "com.example.Z", "The SHA-256 of a code", "exemptions")
		declarations.declare(OWNS_DIGEST, "com.example.A", "The SHA-256 of a token", "exemptions")
		declarations.declare(BOUNDARY, "com.example.Worker", "A delivery must not end the worker", "shipshape")

		assertThat(declarations.of(setOf(OWNS_DIGEST))).containsExactly(
			ExemptionDeclaration(OWNS_DIGEST, "com.example.A", "The SHA-256 of a token", "exemptions"),
			ExemptionDeclaration(OWNS_DIGEST, "com.example.Z", "The SHA-256 of a code", "exemptions"),
		)
	}

	@Test
	fun `a declaration names a place and a reason, and a place once`() {
		declarations.declare(OWNS_DIGEST, "com.example.Digest", "The SHA-256 of a token", "shipshape")

		assertThatThrownBy { declarations.declare(OWNS_DIGEST, " ", "why", "exemptions") }
			.hasMessage("exemptions declares ownsDigest without the place it excuses")
		assertThatThrownBy { declarations.declare(OWNS_DIGEST, "com.example.Other", "", "exemptions") }
			.hasMessage("exemptions declares ownsDigest(\"com.example.Other\") without a reason; say why the rule does not hold there")
		assertThatThrownBy { declarations.declare(OWNS_DIGEST, "com.example.Digest", "again", "exemptions") }
			.hasMessage(
				"exemptions declares ownsDigest(\"com.example.Digest\"), which ownsDigest(\"com.example.Digest\") in shipshape " +
					"declares already",
			)
	}

	private companion object {
		val OWNS_DIGEST = ExemptionKind("ownsDigest")
		val BOUNDARY = ExemptionKind("catchesEverything")
	}
}
