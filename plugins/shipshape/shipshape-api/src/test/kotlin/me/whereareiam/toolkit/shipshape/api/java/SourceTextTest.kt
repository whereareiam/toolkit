package me.whereareiam.toolkit.shipshape.api.java

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SourceTextTest {
	@Test
	fun `comments are removed and lines are kept`() {
		val text = "a(); // record Point(int x)\n/* @Bean\n */ b();\n"

		assertThat(SourceText.strip(text, literals = true)).isEqualTo("a(); \n \n b();\n")
	}

	@Test
	fun `a literal keeps its text only when asked`() {
		val text = """tag("integration"); c = '"'; s = "a \" // b";"""

		assertThat(SourceText.strip(text, literals = true)).isEqualTo(text)
		assertThat(SourceText.strip(text, literals = false)).isEqualTo("""tag(""); c = ''; s = "";""")
	}

	@Test
	fun `a text block is one literal`() {
		val text = "s = \"\"\"\n\trecord Point(int x) {}\n\t\"\"\";\nnext();"

		assertThat(SourceText.strip(text, literals = false)).isEqualTo("s = \"\n\n\";\nnext();")
	}

	@Test
	fun `only what the literals say is kept, each on its line and apart from the next`() {
		val text = "a(\"one\", \"two\"); // \"three\"\n/* \"four\"\n */ b = \"\"\"\n\tfive\n\t\"\"\" + 'c';\n"

		assertThat(SourceText.literals(text)).isEqualTo(" one  two \n \n \n\tfive\n\t  c \n")
	}

	@Test
	fun `what stands between braces is not top level`() {
		val code = "@Tags({A, B}) public class Outer { record Inner(int x) {} } interface Second {}"

		assertThat(SourceText.topLevel(code)).isEqualTo("@Tags( ) public class Outer    interface Second  ")
	}
}
