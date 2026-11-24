package me.whereareiam.toolkit.architecture

import me.whereareiam.toolkit.architecture.model.ArchitectureExtension
import me.whereareiam.toolkit.architecture.type.ArchitectureKind
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ArchitectureKindsTest {
	@TempDir
	lateinit var directory: File

	private val root: Project by lazy { ProjectBuilder.builder().withProjectDir(directory).withName("session-service").build() }

	@Test
	fun `a project's kind is inferred from its name, its plugins and its sources where the plugin is not applied`() {
		val api = child("session-api")
		val plugin = child("session-plugin").also { it.pluginManager.apply("java-gradle-plugin") }
		val logic = child("session-common").also { it.file("src/main/java").mkdirs() }

		assertEquals(ArchitectureKind.API, ArchitectureKinds.of(api))
		assertEquals(ArchitectureKind.ASSEMBLY, ArchitectureKinds.of(plugin))
		assertEquals(ArchitectureKind.IMPLEMENTATION, ArchitectureKinds.of(logic))
	}

	@Test
	fun `the kind an architecture block states is the project's kind`() {
		val model = child("session-model").also { it.pluginManager.apply(ToolkitArchitecturePlugin.ID) }
		model.extensions.getByType(ArchitectureExtension::class.java).kind = ArchitectureKind.API

		assertEquals(ArchitectureKind.API, ArchitectureKinds.of(model))
	}

	private fun child(name: String): Project =
		ProjectBuilder.builder().withParent(root).withName(name).withProjectDir(directory.resolve(name)).build()
}
