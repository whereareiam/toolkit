package me.whereareiam.toolkit.shipshape.api.buildfile

/** How a build file names something of the version catalog `libs`: `libs.jackson.databind`. */
object CatalogAccessor {
	/** What a build file names the catalog by. */
	const val CATALOG = "libs"

	private val WRITTEN = Regex("""(?<![\w.])$CATALOG(?:\.[A-Za-z][A-Za-z0-9]*)+""")

	/** Everything in [text] that is written as something of the catalog: `libs.` and the words behind it, in their order. */
	fun written(text: String): Sequence<String> = WRITTEN.findAll(text).map(MatchResult::value)
}
