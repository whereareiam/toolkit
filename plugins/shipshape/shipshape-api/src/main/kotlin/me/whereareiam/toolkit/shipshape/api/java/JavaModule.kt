package me.whereareiam.toolkit.shipshape.api.java

import java.io.Serializable

/** One project of the build whose Java a rule reads, the root project among them. */
data class JavaModule(
	/** The Gradle path, `:session-adapter:adapter-grpc`, or `:` for the root project. */
	val path: String,
	/** The folder from the root project's folder, with `/` between its parts; empty for the root project. */
	val directory: String,
	/** The project's name, which names the root project in a breach. */
	val name: String,
) : Serializable {
	/** How a breach names the module: by its path, the root project by its name. */
	val label: String
		get() = if (path == ROOT) name else path

	private companion object {
		private const val serialVersionUID = 1L

		const val ROOT = ":"
	}
}
