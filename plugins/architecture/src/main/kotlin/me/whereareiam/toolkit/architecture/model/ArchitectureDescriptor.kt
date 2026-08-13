package me.whereareiam.toolkit.architecture.model

import me.whereareiam.toolkit.architecture.type.ArchitectureKind

internal data class ArchitectureDescriptor(
	val kind: ArchitectureKind,
	val rootApi: Boolean,
)
