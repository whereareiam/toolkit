package me.whereareiam.toolkit.shipshape.api.report

import me.whereareiam.toolkit.shipshape.api.type.Enforcement
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.gradle.api.GradleException
import org.gradle.api.logging.Logging
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class BreachReportTest {
	private val logger = Logging.getLogger(BreachReportTest::class.java)

	private val breaches = listOf(
		Finding(SHIPPED_FILE, Enforcement.WARNS, Breach("lombok.config", 9, "is a text an older convention shipped")),
		Finding(LOGGER, Enforcement.FAILS, Breach("a/First.java", 3, "declares the logger LOGGER by hand")),
		Finding(SHIPPED_FILE, Enforcement.WARNS, Breach(".editorconfig", 12, "is a text an older convention shipped")),
		Finding(WILDCARD, Enforcement.FAILS, Breach("a/First.java", 2, "imports java.util.* whole")),
		Finding(PACKAGE_SIZE, Enforcement.FAILS, Breach("a/model", null, "holds 11 files directly")),
	)

	@Test
	fun `a breach fails the build or is only reported, as the enforcement of its rule says`() {
		val report = BreachReport(breaches)

		assertThat(report.failures.map(Finding::sentence)).containsExactly(
			"hand-written-logger a/First.java:3 declares the logger LOGGER by hand",
			"package-size a/model holds 11 files directly",
			"wildcard-import a/First.java:2 imports java.util.* whole",
		)
		assertThat(report.warnings).hasSize(2).allSatisfy { assertThat(it.enforcement).isEqualTo(Enforcement.WARNS) }
	}

	@Test
	fun `the warning names every place with its rule's name, and counts each rule`() {
		assertThat(BreachReport(breaches).warning("verifyConventionFiles")).isEqualTo(
			"verifyConventionFiles: the standard is not kept in 2 places. The build goes on: these rules warn, so that they stop no " +
				"delivery; repair the places.\n" +
				"""
					 - shipped-file-current .editorconfig:12 is a text an older convention shipped
					 - shipped-file-current lombok.config:9 is a text an older convention shipped
					shipped-file-current 2: 2 places
				""".trimIndent(),
		)
	}

	@Test
	fun `the report holds every breach, what fails among it`() {
		assertThat(BreachReport(breaches.take(2) + breaches[3]).text()).isEqualTo(
			"""
				hand-written-logger a/First.java:3 declares the logger LOGGER by hand
				shipped-file-current lombok.config:9 is a text an older convention shipped
				wildcard-import a/First.java:2 imports java.util.* whole
				hand-written-logger 1, shipped-file-current 1, wildcard-import 1: 3 places
			""".trimIndent() + "\n",
		)
	}

	@Test
	fun `a build that keeps the standard is told so, and nothing is printed`() {
		val report = BreachReport(emptyList())

		assertThat(report.text()).isEqualTo("The standard is kept.\n")
		assertThat(report.warning("verifySource")).isNull()
		assertThat(report.failures).isEmpty()
	}

	@Test
	fun `a task that found only what warns ends with its report written, and passes`(@TempDir directory: File) {
		val file = directory.resolve("verify-convention-files/report.txt")

		BreachReport(listOf(breaches[0], breaches[2])).conclude("verifyConventionFiles", file, logger, "A file was changed")

		assertThat(file.readLines()).containsExactly(
			"shipped-file-current .editorconfig:12 is a text an older convention shipped",
			"shipped-file-current lombok.config:9 is a text an older convention shipped",
			"shipped-file-current 2: 2 places",
		)
	}

	@Test
	fun `a task that found what fails ends with a failure that names it, after the report is written`(@TempDir directory: File) {
		val file = directory.resolve("report.txt")
		val report = BreachReport(breaches)

		assertThatThrownBy { report.conclude("verifySource", file, logger, "The rules of the source were violated") }
			.isInstanceOf(GradleException::class.java)
			.hasMessage(
				"The rules of the source were violated:\n - hand-written-logger a/First.java:3 declares the logger LOGGER by hand\n" +
					" - package-size a/model holds 11 files directly\n - wildcard-import a/First.java:2 imports java.util.* whole",
			)
		assertThat(file.readLines()).hasSize(6)
			.contains(
				"wildcard-import a/First.java:2 imports java.util.* whole",
				"shipped-file-current .editorconfig:12 is a text an older convention shipped",
			)
	}

	@Test
	fun `one breach is one place`() {
		assertThat(BreachReport(breaches.take(1)).warning("verifyConventionFiles"))
			.startsWith("verifyConventionFiles: the standard is not kept in 1 place. ")
			.endsWith("\nshipped-file-current 1: 1 place")
	}

	private companion object {
		const val SHIPPED_FILE = "shipped-file-current"
		const val LOGGER = "hand-written-logger"
		const val WILDCARD = "wildcard-import"
		const val PACKAGE_SIZE = "package-size"
	}
}
