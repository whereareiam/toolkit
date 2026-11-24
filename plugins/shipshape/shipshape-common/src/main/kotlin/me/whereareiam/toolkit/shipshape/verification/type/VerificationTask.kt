package me.whereareiam.toolkit.shipshape.verification.type

/** The tasks shipshape registers, each with the rules of one kind of reading. */
enum class VerificationTask(
	/** The task's name. */
	val taskName: String,
	/** The folder below the root project's build folder its report is written to. */
	val reportFolder: String,
	/** What the task says before the breaches that fail it. */
	val broken: String,
	val description: String,
) {
	/** Rules that read the parsed shape of the Java classes. */
	SHAPE(
		"verifyShape", "verify-shape", "The rules of the shape of a class were violated",
		"Verifies the rules that are read from the shape of the Java classes.",
	),

	/** Rules that read the Java source as text. */
	SOURCE(
		"verifySource", "verify-source", "The rules of the source were violated",
		"Verifies the rules that are read from the Java source as text.",
	),

	/** Rules that read the build files. */
	BUILD(
		"verifyBuild", "verify-build", "The rules of build files were violated",
		"Verifies the rules that are read from the build files.",
	),
}
