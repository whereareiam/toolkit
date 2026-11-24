package me.whereareiam.toolkit.shipshape.api.java

import java.io.File

/**
 * The Java sources of a build as the rules read them: every file below `src/<source set>/java` of each module, each
 * read once.
 */
class JavaSources(
	/** The root project's folder, which every path is taken from. */
	val directory: File,
	/** The modules, the root project among them, in the order of their paths. */
	val modules: List<JavaModule>,
) {
	private val read = HashMap<JavaModule, List<JavaSource>>()

	private val packages: Map<String, Set<String>> by lazy {
		all.groupBy(JavaSource::packageName).mapValues { (_, files) -> files.map(JavaSource::typeName).toSet() }
	}

	/** Every Java file of the build, module by module. */
	val all: List<JavaSource> by lazy { modules.flatMap(::of) }

	/** The folder of [module]. */
	fun directoryOf(module: JavaModule): File = if (module.directory.isEmpty()) directory else directory.resolve(module.directory)

	/** Every Java file below `src/<source set>/java` of [module]; a file is read once. */
	fun of(module: JavaModule): List<JavaSource> = read.getOrPut(module) {
		sourceRoots(module).flatMap { root ->
			root.walkTopDown().filter { it.isFile && it.extension == JAVA }.sortedBy(File::getPath)
				.map { JavaSource(it, root, directory, module) }
				.toList()
		}
	}

	/** The names of the types the package [name] holds, over every module and source set of the build. */
	fun typesOf(name: String): Set<String> = packages[name].orEmpty()

	/** The Java source roots of [module]: `src/main/java`, `src/test/java`, and so on. */
	fun sourceRoots(module: JavaModule): List<File> = directoryOf(module).resolve("src").listFiles().orEmpty()
		.map { it.resolve(JAVA) }
		.filter(File::isDirectory)
		.sortedBy(File::getPath)

	companion object {
		private const val JAVA = "java"

		/** The pattern, from a module's folder, of the Java files [of] reads. */
		const val PATTERN = "src/*/java/**/*.java"
	}
}
