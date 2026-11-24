package me.whereareiam.toolkit.shipshape.api.measure

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

class LineWidthTest {
	@Test
	fun `a tab counts as four columns, and a line wider than the limit is found with its number`() {
		val lines = listOf("\tshort", "x".repeat(LineWidth.LIMIT), "\t" + "x".repeat(LineWidth.LIMIT - 3))

		assertThat(LineWidth.of("\tab")).isEqualTo(6)
		assertThat(LineWidth.tooWide(lines)).containsExactly(3 to LineWidth.LIMIT + 1)
	}

	@Test
	fun `shipshape keeps the length it asks of a line, in the Kotlin and the build file of each of its modules`() {
		val plugin = File("..")
		val files = plugin.walkTopDown().onEnter { it.name != "build" }
			.filter { it.extension == "kt" && "/src/" in it.invariantSeparatorsPath || it.name == "build.gradle.kts" }
		val wide = files.toList().flatMap { file ->
			val path = file.relativeTo(plugin).invariantSeparatorsPath

			LineWidth.tooWide(file.readLines()).map { (line, width) -> "$path:$line is $width columns wide" }
		}

		assertThat(wide).isEmpty()
	}
}
