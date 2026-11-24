package me.whereareiam.toolkit.shipshape.verification

import me.whereareiam.toolkit.shipshape.api.java.JavaModule
import me.whereareiam.toolkit.shipshape.api.java.JavaSources
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.work.DisableCachingByDefault

/** A task whose rules read the Java of the build's modules. */
@DisableCachingByDefault(because = "Reading the sources is as fast as fetching a result")
abstract class JavaVerification : StandardVerification() {
	/** What the rules read: the Java sources of every source set of every module, and what a rule reads beside them. */
	@get:InputFiles
	@get:PathSensitive(PathSensitivity.RELATIVE)
	@get:IgnoreEmptyDirectories
	abstract val sources: ConfigurableFileCollection

	/** The Java sources of the build, read as [sources] tracks them. */
	protected fun javaSources(): JavaSources =
		JavaSources(directory.get().asFile, modules.get().keys.sortedBy(JavaModule::path))
}
