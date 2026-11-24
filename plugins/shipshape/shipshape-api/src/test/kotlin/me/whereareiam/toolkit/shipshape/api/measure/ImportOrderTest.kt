package me.whereareiam.toolkit.shipshape.api.measure

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ImportOrderTest {
	@Test
	fun `the imports stand by group, each group sorted, a blank line between two`() {
		val written = listOf(
			"import java.util.List;",
			"import static org.junit.Assert.fail;",
			"",
			"import com.example.B;",
			"import com.example.A;",
		)

		assertThat(ImportOrder.ordered(written)).containsExactly(
			"import com.example.A;",
			"import com.example.B;",
			"",
			"import java.util.List;",
			"",
			"import static org.junit.Assert.fail;",
		)
	}

	@Test
	fun `a file's import block is put in that order, and the rest of the file kept`() {
		val java = "package com.example;\n\nimport java.util.List;\nimport com.example.other.B;\n\nclass A {\n}\n"

		assertThat(ImportOrder.sorted(java))
			.isEqualTo("package com.example;\n\nimport com.example.other.B;\n\nimport java.util.List;\n\nclass A {\n}\n")
		assertThat(ImportOrder.sorted("class A {\n}\n")).isEqualTo("class A {\n}\n")
	}
}
