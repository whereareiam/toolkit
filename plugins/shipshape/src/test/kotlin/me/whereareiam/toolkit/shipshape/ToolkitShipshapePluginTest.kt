package me.whereareiam.toolkit.shipshape

import me.whereareiam.toolkit.shipshape.ShipshapeBuild.Companion.API
import me.whereareiam.toolkit.shipshape.ShipshapeBuild.Companion.COMMON
import me.whereareiam.toolkit.shipshape.ShipshapeBuild.Companion.ROOT
import me.whereareiam.toolkit.shipshape.api.ShipshapeExtension
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ToolkitShipshapePluginTest {
	@TempDir
	lateinit var directory: Path

	@Test
	fun `check holds a task of each category, a build that keeps every rule passes them, and an unchanged build is not read twice`() {
		val build = ShipshapeBuild.service(directory)

		val result = build.build("check")

		assertThat(TASKS.map { result.task(it)?.outcome }).containsOnly(TaskOutcome.SUCCESS)
		assertThat(REPORTS.map(build::read)).containsOnly("The standard is kept.\n")
		assertThat(TASKS.map { build.build("check").task(it)?.outcome }).containsOnly(TaskOutcome.UP_TO_DATE)
	}

	@Test
	fun `a breach of each category fails check, named with its rule, its place and what is asked, and counted in its report`() {
		val build = ShipshapeBuild.service(directory)
			.write("$API/SessionKind.java", KIND)
			.write("$COMMON/Sessions.java", SESSIONS.replace("import java.util.List;", "import java.util.*;"))
			.write("session-common/build.gradle.kts", "plugins {\n\t`java-library`\n\tbase\n}\n")

		val result = build.fail("check", "--continue")

		assertThat(result.output).contains(
			"The rules of packages were violated:\n" +
				" - role-folder $API/SessionKind.java:6 declares SessionKind outside of type/; an enum of an api lies below type/",
			"The rules of imports were violated:\n - wildcard-import $COMMON/Sessions.java:3 imports java.util.* whole; import each " +
				"name it uses",
			"The rules of buildfile were violated:\n - plugin-order session-common/build.gradle.kts:3 applies base behind java-library;",
		)
		assertThat(build.read("build/verify-imports/report.txt")).endsWith("wildcard-import 1: 1 place\n")
	}

	@Test
	fun `the settings script is read by the build rules that read it, and a change to it is read anew`() {
		val build = ShipshapeBuild.service(directory)

		assertThat(build.build("verifyBuildfile").task(":verifyBuildfile")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

		build.write("settings.gradle.kts", build.read("settings.gradle.kts") + "\nval yaml = \"org.yaml:snakeyaml:2.2\"\n")

		assertThat(build.fail("verifyBuildfile").output).contains(
			"The rules of buildfile were violated:\n - catalog-coordinate settings.gradle.kts:5 writes out org.yaml:snakeyaml:2.2 " +
				"with its version; name the library from the version catalog",
		)
	}

	@Test
	fun `a module's role is the kind the architecture plugin gives it, and the inferred one where the plugin is not applied`() {
		val build = ShipshapeBuild.service(directory).write("$COMMON/SessionKind.java", KIND)

		assertThat(build.build("verifyPackages").task(":verifyPackages")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

		build.write("session-common/build.gradle.kts", "plugins {\n\tid(\"$ARCHITECTURE\")\n}\n\narchitecture {\n\tkind = api\n}\n")

		assertThat(build.fail("verifyPackages").output)
			.contains(" - role-folder $COMMON/SessionKind.java:6 declares SessionKind outside of type/; an enum of an api lies below type/")
	}

	@Test
	fun `a declared place is excused, and a declaration that excuses nothing fails the build`() {
		val catching = SESSIONS.replace("return List.of();", "try {\n\t\t\treturn List.of();\n\t\t} catch (RuntimeException failure) {\n" +
			"\t\t\treturn List.of();\n\t\t}")
		val excused = "import me.whereareiam.toolkit.shipshape.rule.language.LanguageRules\n\n$ROOT\nshipshape {\n" +
			"\texempt(LanguageRules.CATCHES_EVERYTHING, \"com.example.session.Sessions\", \"A listing must answer\")\n}\n"
		val build = ShipshapeBuild.service(directory, excused).write("$COMMON/Sessions.java", catching)

		assertThat(build.build("verifyLanguage").task(":verifyLanguage")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

		build.write("$COMMON/Sessions.java", SESSIONS)

		assertThat(build.fail("verifyLanguage").output).contains(
			" - unused-exemption build.gradle.kts declares catchesEverything(\"com.example.session.Sessions\") in shipshape, and nothing " +
				"there needs it; remove the declaration",
		)
	}

	@Test
	fun `the configuration cache keeps the rules, and a build it reuses holds them alike`() {
		val build = ShipshapeBuild.service(directory)

		build.build("check", "--configuration-cache")
		val reused = build.build("check", "--configuration-cache")

		assertThat(reused.output).contains("Reusing configuration cache.")
		assertThat(TASKS.map { reused.task(it)?.outcome }).containsOnly(TaskOutcome.UP_TO_DATE)
	}

	@Test
	fun `a build that names no package root of its own is told to`() {
		val build = ShipshapeBuild.service(directory, "plugins {\n\tid(\"me.whereareiam.toolkit.shipshape\")\n}\n")

		assertThat(build.fail("verifyBuildfile").output)
			.contains("verifyBuildfile needs the packages the build's own types lie in: set shipshape { packageRoots } or the group")
	}

	@Test
	fun `shipshape holds a whole build, and so is applied to its root project alone`() {
		val build = ShipshapeBuild.service(directory, "")
			.write("session-api/build.gradle.kts", "plugins {\n\tid(\"me.whereareiam.toolkit.shipshape\")\n}\n")

		assertThat(build.fail("help").output)
			.contains("me.whereareiam.toolkit.shipshape is applied to the root project, which holds every module; :session-api is none")
	}

	@Test
	fun `another plugin registers its rules and the exemption kinds they read, and their breaches are excused and counted alike`() {
		val root = "import com.example.rules.ManagerRulesPlugin\n\nplugins {\n\tid(\"manager-rules\")\n}\n\ngroup = \"com.example\"\n\n" +
			"shipshape {\n\texempt(ManagerRulesPlugin.MANAGES_TRUST, \"com.example.session.TrustManager\", \"The JDK names it so\")\n}\n"
		val build = ShipshapeBuild.service(directory, root)
			.pluginInBuildSrc(mapOf("manager-rules" to "com.example.rules.ManagerRulesPlugin"), MANAGER_RULES)
			.write("$COMMON/TrustManager.java", manager("TrustManager"))
			.write("$COMMON/SessionManager.java", manager("SessionManager"))

		val refused = build.failWithBuildSrc("verifyNames")

		assertThat(refused.output).contains(
			" - manager-name $COMMON/SessionManager.java is named …Manager; name it after what it does",
		).doesNotContain("TrustManager.java")
		assertThat(build.read("build/verify-names/report.txt")).endsWith("manager-name 1: 1 place\n")

		build.remove("$COMMON/SessionManager.java").remove("$COMMON/TrustManager.java")

		assertThat(build.failWithBuildSrc("verifyNames").output).contains(
			" - unused-exemption build.gradle.kts declares managesTrust(\"com.example.session.TrustManager\") in shipshape, and nothing " +
				"there needs it; remove the declaration",
		)
	}

	@Test
	fun `a plugin that registers a rule under a name shipshape has is refused`() {
		val root = "plugins {\n\tid(\"duplicate-rules\")\n}\n\ngroup = \"com.example\"\n"
		val build = ShipshapeBuild.service(directory, root)
			.pluginInBuildSrc(mapOf("duplicate-rules" to "com.example.rules.DuplicateRulesPlugin"), DUPLICATE_RULES)

		assertThat(build.failWithBuildSrc("help").output)
			.contains("shipshape already has a rule wildcard-import: " +
				"me.whereareiam.toolkit.shipshape.rule.imports.WildcardImportRule")
	}

	private fun manager(type: String): String = "package com.example.session;\n\n/**\n * Manages.\n */\npublic interface $type {\n}\n"

	private companion object {
		val CATEGORIES = listOf(
			"buildfile", "documentation", "flow", "format", "imports", "language", "lombok", "models", "packages", "size", "tests", "types",
		)
		val TASKS = CATEGORIES.map { ":verify" + it.replaceFirstChar(Char::uppercaseChar) }
		const val ARCHITECTURE = "me.whereareiam.toolkit.architecture"
		const val KIND = "package com.example.session;\n\n/**\n * Where a session is held.\n */\npublic enum SessionKind {\n\tWEB\n}\n"

		val REPORTS = CATEGORIES.map { "build/verify-$it/report.txt" }

		val SESSIONS = """
			package com.example.session;

			import java.util.List;

			/**
			 * Lists the sessions of a user.
			 */
			public class Sessions {
				List<Integer> list() {
					return List.of();
				}
			}

		""".trimIndent()

		val MANAGER_RULES = mapOf(
			"com.example.rules.ManagerRulesPlugin" to """
				package com.example.rules;

				import me.whereareiam.toolkit.shipshape.api.RuleCategory;
				import me.whereareiam.toolkit.shipshape.api.ShipshapeExtension;
				import me.whereareiam.toolkit.shipshape.api.exemption.ExemptionKind;
				import org.gradle.api.Plugin;
				import org.gradle.api.Project;

				public class ManagerRulesPlugin implements Plugin<Project> {
					public static final ExemptionKind MANAGES_TRUST = new ExemptionKind("managesTrust");
					public static final RuleCategory NAMES = new RuleCategory("names", "Verifies what types are named.");

					@Override
					public void apply(Project project) {
						project.getPluginManager().apply("me.whereareiam.toolkit.shipshape");
						ShipshapeExtension shipshape = project.getExtensions().getByType(ShipshapeExtension.class);
						shipshape.exemptionKind(MANAGES_TRUST);
						shipshape.source(new ManagerNameRule());
					}
				}
			""".trimIndent(),
			"com.example.rules.ManagerNameRule" to """
				package com.example.rules;

				import java.util.ArrayList;
				import java.util.List;
				import java.util.Set;
				import me.whereareiam.toolkit.shipshape.api.java.JavaSource;
				import me.whereareiam.toolkit.shipshape.api.java.JavaSources;
				import me.whereareiam.toolkit.shipshape.api.RuleCategory;
				import me.whereareiam.toolkit.shipshape.api.SourceRule;
				import me.whereareiam.toolkit.shipshape.api.RuleContext;
				import me.whereareiam.toolkit.shipshape.api.report.Breach;
				import me.whereareiam.toolkit.shipshape.api.exemption.ExemptionKind;
				import me.whereareiam.toolkit.shipshape.api.exemption.ExemptionPlace;
				import me.whereareiam.toolkit.shipshape.api.type.Enforcement;

				public class ManagerNameRule implements SourceRule {
					@Override
					public String getId() {
						return "manager-name";
					}

					@Override
					public RuleCategory getCategory() {
						return ManagerRulesPlugin.NAMES;
					}

					@Override
					public Enforcement getEnforcement() {
						return Enforcement.FAILS;
					}

					@Override
					public Set<ExemptionKind> getExemptions() {
						return Set.of(ManagerRulesPlugin.MANAGES_TRUST);
					}

					@Override
					public Set<String> getReads() {
						return Set.of();
					}

					@Override
					public List<Breach> check(JavaSources sources, RuleContext context) {
						List<Breach> found = new ArrayList<>();
						for (JavaSource source : sources.getAll()) {
							if (!source.getMain() || !source.getTypeName().endsWith("Manager")) continue;
							ExemptionPlace place = new ExemptionPlace(ManagerRulesPlugin.MANAGES_TRUST, source.getQualifiedName());
							found.add(new Breach(source.getPath(), null, "is named …Manager; name it after what it does", place));
						}
						return found;
					}
				}
			""".trimIndent(),
		)

		val DUPLICATE_RULES = mapOf(
			"com.example.rules.DuplicateRulesPlugin" to """
				package com.example.rules;

				import java.util.List;
				import java.util.Set;
				import me.whereareiam.toolkit.shipshape.api.RuleCategory;
				import me.whereareiam.toolkit.shipshape.api.ShipshapeExtension;
				import me.whereareiam.toolkit.shipshape.api.java.JavaSources;
				import me.whereareiam.toolkit.shipshape.api.SourceRule;
				import me.whereareiam.toolkit.shipshape.rule.imports.ImportRules;
				import me.whereareiam.toolkit.shipshape.api.RuleContext;
				import me.whereareiam.toolkit.shipshape.api.report.Breach;
				import me.whereareiam.toolkit.shipshape.api.exemption.ExemptionKind;
				import me.whereareiam.toolkit.shipshape.api.type.Enforcement;
				import org.gradle.api.Plugin;
				import org.gradle.api.Project;

				public class DuplicateRulesPlugin implements Plugin<Project>, SourceRule {
					@Override
					public void apply(Project project) {
						project.getPluginManager().apply("me.whereareiam.toolkit.shipshape");
						project.getExtensions().getByType(ShipshapeExtension.class).source(this);
					}

					@Override
					public String getId() {
						return "wildcard-import";
					}

					@Override
					public RuleCategory getCategory() {
						return ImportRules.CATEGORY;
					}

					@Override
					public Enforcement getEnforcement() {
						return Enforcement.FAILS;
					}

					@Override
					public Set<ExemptionKind> getExemptions() {
						return Set.of();
					}

					@Override
					public Set<String> getReads() {
						return Set.of();
					}

					@Override
					public List<Breach> check(JavaSources sources, RuleContext context) {
						return List.of();
					}
				}
			""".trimIndent(),
		)
	}
}
