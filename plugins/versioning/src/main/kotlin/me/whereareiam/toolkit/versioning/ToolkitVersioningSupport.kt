package me.whereareiam.toolkit.versioning

object ToolkitVersioningSupport {

    const val DEFAULT_RELEASE_PATTERN =
        "^v?(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-[0-9A-Za-z.-]+)?(?:\\+[0-9A-Za-z.-]+)?$"

    fun classifyChannel(version: String, releasePattern: String): String =
        if (isReleaseVersion(version, releasePattern)) "release" else "development"

    fun isReleaseVersion(version: String, releasePattern: String): Boolean =
        Regex(releasePattern).matches(version)
}

