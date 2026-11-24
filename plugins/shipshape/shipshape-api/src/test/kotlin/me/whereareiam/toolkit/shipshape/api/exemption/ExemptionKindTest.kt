package me.whereareiam.toolkit.shipshape.api.exemption

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ExemptionKindTest {
	@Test
	fun `a kind is named as the method that declares it, and tells how a place is declared`() {
		val kind = ExemptionKind("ownsDigest")

		assertThat(kind.declaration("com.example.Digest")).isEqualTo("ownsDigest(\"com.example.Digest\", why)")
	}

	@Test
	fun `a kind's method is written as a method`() {
		assertThatThrownBy { ExemptionKind("owns-digest") }
			.hasMessage("An exemption kind is named as a method, catchesEverything, not 'owns-digest'")
	}
}
