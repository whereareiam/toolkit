package me.whereareiam.toolkit.shipshape.api.buildfile

/** One dependency a block `dependencies` declares: the configuration it is added to, and what is added. */
class DependencyDeclaration(
	/** The line it begins on, counted from one. */
	val line: Int,
	/** The configuration it is added to, `testImplementation`. */
	val configuration: String,
	/** What is added, as it is written between the configuration's parentheses. */
	private val argument: String,
	/** Whether an empty line stands between it and the declaration before it. A line of comment is not empty. */
	val separated: Boolean,
) {
	/** The project it names, `:session-api`, also where it names that project's test fixtures; `null` when it names none. */
	val project: String? = PROJECT.find(argument)?.groupValues?.get(1)

	/**
	 * What it names of the catalog as it is written, `libs.jackson.databind`, also inside `platform(…)`;
	 * `null` when it names nothing of the catalog.
	 */
	val accessor: String? = CatalogAccessor.written(argument).firstOrNull().takeIf { project == null }

	/**
	 * The library it writes out, `org.yaml:snakeyaml`, with the version where one is written, also one taken
	 * from a value (`org.yaml:snakeyaml:$yaml`): as a string, or as `group`, `name` and `version`. `null` when
	 * it writes out none. A string whose group or name is put together, as `"$group:${it.name}:$version"`
	 * names a module of the build itself, is no library written out.
	 */
	val coordinate: String? = (COORDINATE.find(argument)?.groupValues?.get(1) ?: named()).takeIf { project == null && accessor == null }

	/** Whether what it adds is a platform: a set of versions. */
	val platform: Boolean = PLATFORM.containsMatchIn(argument)

	/** The library of [coordinate] without its version, `group:name`. */
	val module: String?
		get() = coordinate?.split(':')?.take(2)?.joinToString(":")

	/** The version [coordinate] carries, or `null` when it carries none. */
	val version: String?
		get() = coordinate?.split(':', limit = 3)?.getOrNull(2)?.takeIf(String::isNotEmpty)

	private fun named(): String? {
		val parts = NAMED.findAll(argument).associate { it.groupValues[1] to it.groupValues[2] }
		val module = listOfNotNull(parts["group"], parts["name"]).takeIf { it.size == 2 } ?: return null

		return (module + listOfNotNull(parts["version"])).joinToString(":")
	}

	companion object {
		/** A configuration, what it is given between its parentheses, and behind them at most a closure. */
		private val DECLARATION = Regex("""(?:([A-Za-z][A-Za-z0-9]*)|"([A-Za-z][A-Za-z0-9]*)")\s*\((.*)\)(?:\s*\{.*)?""")
		private val PROJECT = Regex("""(?<![\w.])project\(\s*(?:path\s*=\s*)?"([^"]+)"""")
		/**
		 * Coordinates as a string holds them: a group and a name that are written out, and behind them at most
		 * a version, which may be taken from a value.
		 */
		const val COORDINATES = """[A-Za-z0-9_.-]+:[A-Za-z0-9_.-]+(?::[^"]*)?"""

		private val COORDINATE = Regex(""""($COORDINATES)"""")
		private val NAMED = Regex("""(?<![\w.])(group|name|version)\s*=\s*"([^"$]*)"""")
		private val PLATFORM = Regex("""(?<![\w.])(?:platform|enforcedPlatform)\(""")

		/** What a block `dependencies` calls that adds nothing to a configuration by its own name. */
		private val NO_CONFIGURATION = setOf("add", "constraints", "components", "modules", "registerTransform")

		/**
		 * Reads a declaration from one line of a block `dependencies`, without its comments.
		 *
		 * @param number the line's number, counted from one
		 * @param separated whether an empty line stands between it and the declaration before it
		 * @param text the declaration as one line, also where it is written over several
		 * @return the declaration, or `null` when the line adds nothing to a configuration by its name, or does
		 * not close the parenthesis it opens
		 */
		fun of(number: Int, text: String, separated: Boolean): DependencyDeclaration? {
			val match = DECLARATION.matchEntire(text.trim()) ?: return null
			val configuration = match.groupValues[1].ifEmpty { match.groupValues[2] }
			if (configuration in NO_CONFIGURATION) return null

			return DependencyDeclaration(number, configuration, match.groupValues[3], separated)
		}
	}
}
