package me.whereareiam.toolkit.shipshape.api.buildfile

/** One plugin the block `plugins` of a build file applies. */
class PluginDeclaration(
	/** The line it stands on, counted from one. */
	val line: Int,
	/** The plugin's name as it is written: `java-test-fixtures`, `org.openapi.generator`, `com.example.convention.api`. */
	val name: String,
	val kind: Kind,
	/** The version the line gives the plugin, or `null` when it gives none. */
	val version: String?,
) {
	/** What a plugin is to the build that applies it, in the order the kinds are written in. */
	enum class Kind {
		/** A plugin Gradle brings, written by its accessor: `base`, `` `java-test-fixtures` ``. */
		BUILT_IN,

		/** A plugin of somebody else, written by its id or by an alias of the catalog: `id("org.openapi.generator")`. */
		EXTERNAL,

		/** A plugin of the build's own conventions: an id of one word, or one in a namespace the build names its conventions by. */
		CONVENTION,
	}

	companion object {
		private val ACCESSOR = Regex("""`([^`]+)`|([A-Za-z][A-Za-z0-9]*)""")
		private val ID = Regex("""id\(\s*"([^"]+)"\s*\)""")
		private val ALIAS = Regex("""alias\(\s*${CatalogAccessor.CATALOG}\.plugins\.([A-Za-z0-9.]+)\s*\)""")
		private val KOTLIN = Regex("""kotlin\(\s*"([^"]+)"\s*\)""")

		/** What stands between a plugin and the version a line gives it, and what begins `apply false`. */
		private val VERSION = Regex("""\s+version\s+""")
		private const val NOT_APPLIED = " apply "

		/** The ids Gradle's own plugins begin with. */
		private const val GRADLE_IDS = "org.gradle."

		/**
		 * Reads a declaration from one line of a block `plugins`, without its comments.
		 *
		 * @param number the line's number, counted from one
		 * @param conventionPlugins the id namespaces of the build's own convention plugins, `com.example.convention`
		 * @return the declaration, or `null` when the line applies no plugin in a form that is known
		 */
		fun of(number: Int, text: String, conventionPlugins: Set<String> = emptySet()): PluginDeclaration? {
			val written = text.trim().split(VERSION, limit = 2).map { it.substringBefore(NOT_APPLIED).trim() }
			val applied = written.first()
			val version = written.getOrNull(1)?.trim('"')

			ID.matchEntire(applied)?.groupValues?.get(1)?.let { id ->
				return PluginDeclaration(number, id, kindOf(id, conventionPlugins), version)
			}
			ALIAS.matchEntire(applied)?.groupValues?.get(1)?.let { return PluginDeclaration(number, it, Kind.EXTERNAL, version) }
			KOTLIN.matchEntire(applied)?.groupValues?.get(1)?.let { return PluginDeclaration(number, "kotlin-$it", Kind.EXTERNAL, version) }

			return ACCESSOR.matchEntire(applied)?.let { accessor ->
				PluginDeclaration(number, accessor.groupValues[1].ifEmpty { accessor.groupValues[2] }, Kind.BUILT_IN, version)
			}
		}

		private fun kindOf(id: String, conventionPlugins: Set<String>): Kind = when {
			id.startsWith(GRADLE_IDS) -> Kind.BUILT_IN
			'.' !in id || conventionPlugins.any { id == it || id.startsWith("$it.") } -> Kind.CONVENTION
			else -> Kind.EXTERNAL
		}
	}
}
