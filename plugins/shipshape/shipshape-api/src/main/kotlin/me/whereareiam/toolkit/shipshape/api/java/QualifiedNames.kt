package me.whereareiam.toolkit.shipshape.api.java

/**
 * The names the files of a build write in full, each with whether an import would do. An import does where nothing
 * else in the file answers to the same simple name: no other import, no type the file declares, no type of the file's
 * own package, and no second name written in full, of which nothing says which an import would take.
 */
class QualifiedNames(private val sources: JavaSources) {
	/** Every name [source] writes in full in its code, in the order of the file; an import and the package line are none. */
	fun of(source: JavaSource): List<Written> {
		val statements = source.code.lines()
		val found = source.find(NAME).filterNot { HEADER.containsMatchIn(statements[it.line - 1]) }
			.filterNot { it.group(1).substringBefore('.') in RECEIVERS }
		val inFull = found.map { it.group(1) }.groupBy { it.substringAfterLast('.') }.mapValues { (_, names) -> names.toSet() }
		val imported = source.imports.associateBy { it.substringAfterLast('.') }
		val declared = DECLARED.findAll(source.code).map { it.groupValues[1] }.toSet()
		val neighbours = sources.typesOf(source.packageName)

		return found.map { occurrence ->
			val name = occurrence.group(1)
			val simple = name.substringAfterLast('.')
			val ofThePackage = simple in declared || (simple in neighbours && name.substringBeforeLast('.') != source.packageName)
			val beside = buildSet {
				imported[simple]?.takeIf { it != name }?.let(::add)
				addAll(inFull.getValue(simple) - name)
				if (ofThePackage) add("${source.packageName}.$simple")
			}
			val taken = imported[simple]?.let { it != name } == true || ofThePackage

			Written(name, occurrence.line, importable = !taken && inFull.getValue(simple).size == 1, beside)
		}
	}

	/** One name written in full. */
	class Written(
		val name: String,
		/** The line it is written on, counted from 1. */
		val line: Int,
		/** Whether an import would do: nothing else in the file answers to the same simple name. */
		val importable: Boolean,
		/** The other types the file knows by the same simple name, each by its full name. */
		val beside: Set<String>,
	) {
		/** The name without its package. */
		val simpleName: String
			get() = name.substringAfterLast('.')
	}

	private companion object {
		/** Two packages or more, then a type: `java.net.SocketTimeoutException`. What follows the type is a member of it. */
		val NAME = Regex("""(?<![\w.$])((?:[a-z][a-z0-9_]*\.){2,}[A-Z][A-Za-z0-9_]*)""")

		/** A chain that begins with one of these is the path to a field, not a package. */
		val RECEIVERS = setOf("this", "super")

		val HEADER = Regex("""^\s*(?:import|package)\s""")
		val DECLARED = Regex("""\b(?:class|interface|enum|record)\s+([A-Z][A-Za-z0-9_]*)""")
	}
}
