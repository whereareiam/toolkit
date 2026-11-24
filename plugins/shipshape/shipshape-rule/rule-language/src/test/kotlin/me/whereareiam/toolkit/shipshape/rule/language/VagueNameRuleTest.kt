package me.whereareiam.toolkit.shipshape.rule.language

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class VagueNameRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = VagueNameRule()

	@Test
	fun `a name that says what the type is passes, also one that only holds such a word, and a manager is a reader's to judge`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-common", "main", "com.example.session.DefaultSessionService")
			.java("session-common", "main", "com.example.session.util.Hashes")
			.java("session-common", "main", "com.example.session.UtilityBill")
			.java("session-common", "main", "com.example.session.ImplicitGrant")
			.java("session-common", "main", "com.example.session.PinnedTrustManager")
			.java("session-common", "test", "com.example.session.SessionTestHelper")

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `a type of main code that is named a util, an impl or a helper is refused`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-common", "main", "com.example.session.SessionServiceImpl")
			.java("session-common", "main", "com.example.session.util.HashUtils")
			.java("session-common", "main", "com.example.session.util.TextUtil")
			.java("", "main", "com.example.session.WiringHelper")

		assertThat(fixture.breaches(rule)).containsExactly(
			"vague-name src/main/java/com/example/session/WiringHelper.java is named …Helper, $ASKED",
			"vague-name $COMMON/SessionServiceImpl.java is named …Impl, $ASKED",
			"vague-name $COMMON/util/HashUtils.java is named …Utils, $ASKED",
			"vague-name $COMMON/util/TextUtil.java is named …Util, $ASKED",
		)
	}

	private companion object {
		const val COMMON = "session-common/src/main/java/com/example/session"
		const val ASKED = "which says nothing of what the type is; name it after what it does or what it is made with"
	}
}
