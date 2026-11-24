package me.whereareiam.toolkit.shipshape.api.buildfile

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DependencyDeclarationTest {
	@Test
	fun `a project is read by its path, also as its test fixtures`() {
		assertThat(declared("""api(project(":session-api"))""").project).isEqualTo(":session-api")
		assertThat(declared("""testImplementation(testFixtures(project(":depot-common")))""").project).isEqualTo(":depot-common")
		assertThat(declared("""implementation(project(path = ":session-common"))""").project).isEqualTo(":session-common")
	}

	@Test
	fun `what the catalog names is read as it is written, also as a platform and before a closure`() {
		assertThat(declared("implementation(libs.spring.boot.starter.jdbc)").accessor).isEqualTo("libs.spring.boot.starter.jdbc")
		assertThat(declared("api(platform(libs.spring.boot.dependencies))").accessor).isEqualTo("libs.spring.boot.dependencies")
		assertThat(declared("api(platform(libs.spring.boot.dependencies))").platform).isTrue()
		assertThat(declared("implementation(libs.awssdk.s3) {").accessor).isEqualTo("libs.awssdk.s3")
		assertThat(declared("implementation(libs.awssdk.s3) {").coordinate).isNull()
	}

	@Test
	fun `a library that is written out is read with its version, as a string and by its parts`() {
		val plain = declared("""testImplementation("org.yaml:snakeyaml")""")
		val pinned = declared("""implementation(platform("software.amazon.awssdk:bom:2.55.11"))""")
		val parts = declared("""implementation(group = "org.yaml", name = "snakeyaml", version = "2.6")""")

		assertThat(plain.coordinate).isEqualTo("org.yaml:snakeyaml")
		assertThat(plain.module).isEqualTo("org.yaml:snakeyaml")
		assertThat(plain.version).isNull()
		assertThat(pinned.module).isEqualTo("software.amazon.awssdk:bom")
		assertThat(pinned.version).isEqualTo("2.55.11")
		assertThat(pinned.platform).isTrue()
		assertThat(parts.coordinate).isEqualTo("org.yaml:snakeyaml:2.6")
	}

	@Test
	fun `a library whose version is taken from a value is written out as well`() {
		val valued = declared("implementation(\"org.yaml:snakeyaml:\$yaml\")")

		assertThat(valued.module).isEqualTo("org.yaml:snakeyaml")
		assertThat(valued.version).isEqualTo("\$yaml")
	}

	@Test
	fun `a string put together from parts, a file and Gradle's own are no library written out`() {
		assertThat(declared("published(\"\$group:\${it.name}:\$version\")").coordinate).isNull()
		assertThat(declared("published(\"com.example.protocol:\${it.name}:\$version\")").coordinate).isNull()
		assertThat(declared("""implementation(files("libs/local.jar"))""").coordinate).isNull()
		assertThat(declared("testImplementation(gradleTestKit())").coordinate).isNull()
		assertThat(declared("testImplementation(gradleTestKit())").accessor).isNull()
	}

	@Test
	fun `a configuration is read by its name, also where a build file made it and writes it as a string`() {
		assertThat(declared("contract(libs.nimbus.api.contract)").configuration).isEqualTo("contract")
		assertThat(declared(""""slowTestImplementation"(libs.testcontainers)""").configuration).isEqualTo("slowTestImplementation")
	}

	@Test
	fun `what adds nothing to a configuration by its name is no declaration`() {
		assertThat(DependencyDeclaration.of(3, "constraints {", separated = false)).isNull()
		assertThat(DependencyDeclaration.of(3, """add("implementation", libs.snakeyaml)""", separated = false)).isNull()
		assertThat(DependencyDeclaration.of(3, """subprojects.forEach { published(it.name) }""", separated = false)).isNull()
		assertThat(DependencyDeclaration.of(3, "listOf(", separated = false)).isNull()
	}

	private fun declared(line: String): DependencyDeclaration = DependencyDeclaration.of(1, "\t$line", separated = false)!!
}
