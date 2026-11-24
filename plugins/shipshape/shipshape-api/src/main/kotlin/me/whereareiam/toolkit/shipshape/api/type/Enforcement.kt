package me.whereareiam.toolkit.shipshape.api.type

/** What a breach of a rule costs the build that finds it. */
enum class Enforcement {
	/**
	 * The breach is printed and counted, and the build goes on. A rule is held so only where a breach must stop no
	 * delivery, and while its implementation is tuned.
	 */
	WARNS,

	/** The breach fails the build. */
	FAILS,
}
