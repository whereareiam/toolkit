package me.whereareiam.toolkit.shipshape.api.type

/**
 * The folders of a module that carry the same name in every build and say what kind of type lies below them: a
 * predictable place is worth more than a saved folder.
 */
enum class RoleFolder(
	/** The folder's name, which is the last word of its package. */
	val folder: String,
	/** Whether the folder is made for a single file as well. */
	val heldByOne: Boolean,
) {
	/** The options of a module. */
	CONFIG("config", heldByOne = true),

	/** The exceptions. */
	EXCEPTION("exception", heldByOne = true),

	/** The models, and below them the areas they are grouped by. */
	MODEL("model", heldByOne = true),

	/** The enums and the other closed types. */
	TYPE("type", heldByOne = true),

	/** The stateless operations that have no type of their own. */
	UTIL("util", heldByOne = false),
}
