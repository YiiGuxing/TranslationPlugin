package cn.yiiguxing.plugin.translate.trans

import cn.yiiguxing.plugin.translate.trans.documentation.HtmlDocumentationTranslator
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * HtmlDocumentationTranslatorTest
 */
class HtmlDocumentationTranslatorTest {

    @Test
    fun testTableStructureIsPreserved() {
        val body = parseBody("""<table class="sections"><tr><td>Hello world</td></tr></table>""")
        translate(body)

        assertEquals(1, body.select("table.sections").size)
        assertEquals("tr", body.selectFirst("table > tbody > tr")?.tagName())
        assertEquals("[Hello world]", body.selectFirst("td")?.text())
    }

    @Test
    fun testLinkHrefIsPreserved() {
        val body = parseBody("""<p><a href="https://example.com/doc">See the documentation</a></p>""")
        translate(body)

        assertEquals("https://example.com/doc", body.selectFirst("a")?.attr("href"))
        assertEquals("[See the documentation]", body.selectFirst("a")?.text())
    }

    @Test
    fun testProtectedInlineTagsDoNotBreakSentence() {
        val body = parseBody(
            """<p>The text content of the given <code>body</code> element.</p>""" +
                    """<p>Press <kbd>Ctrl</kbd>+<kbd>C</kbd> to copy.</p>""" +
                    """<p>Use the <samp>config</samp> file and <var>${'$'}value</var> variable.</p>"""
        )
        val texts = mutableListOf<String>()
        HtmlDocumentationTranslator { input ->
            texts.addAll(input)
            input.map(::fakeTranslate)
        }.translateElement(body)

        assertEquals(
            listOf(
                "The text content of the given <b10>body</b10> element.",
                "Press <b10>Ctrl</b10>+<b11>C</b11> to copy.",
                "Use the <b10>config</b10> file and <b11>${'$'}value</b11> variable."
            ),
            texts
        )

        assertEquals("body", body.selectFirst("code")?.text())
        assertEquals("Ctrl", body.select("kbd")[0].text())
        assertEquals("C", body.select("kbd")[1].text())
        assertEquals("config", body.selectFirst("samp")?.text())
        assertEquals("${'$'}value", body.selectFirst("var")?.text())
        assertEquals("[The text content of the given ]body[ element.]", body.select("p")[0].text())
        assertEquals("[Press ]Ctrl[+]C[ to copy.]", body.select("p")[1].text())
        assertEquals("[Use the ]config[ file and ]${'$'}value[ variable.]", body.select("p")[2].text())
    }

    @Test
    fun testSegmentWithOnlyProtectedInlineTagsIsNotTranslated() {
        val body = parseBody("""<p>Before</p><p><code>onlyCode</code></p><p>After</p>""")
        val texts = mutableListOf<String>()
        HtmlDocumentationTranslator { input ->
            texts.addAll(input)
            input.map(::fakeTranslate)
        }.translateElement(body)

        assertEquals(listOf("Before", "After"), texts)
        assertEquals("onlyCode", body.selectFirst("code")?.text())
    }

    @Test
    fun testKeepOriginalTextWithProtectedInlineTags() {
        val body = parseBody("""<p>The <code>body</code> element.</p>""")
        translate(body, keepOriginal = true)

        val p = body.selectFirst("p")!!
        assertEquals("The ", (p.childNode(0) as TextNode).text())
        assertEquals("body", (p.childNode(1) as Element).text())
        assertEquals(" element.", (p.childNode(2) as TextNode).text())
        assertEquals("br", (p.childNode(3) as Element).tagName())
        assertEquals("[The ]", (p.childNode(4) as TextNode).text())
        assertEquals("body", (p.childNode(5) as Element).text())
        assertEquals("[ element.]", (p.childNode(6) as TextNode).text())
    }

    @Test
    fun testNestedInlineElementsAreTranslatedAsAWhole() {
        val body = parseBody("""<p>Hello <b>bold</b> world</p>""")
        translate(body)

        assertEquals("[Hello ][bold][ world]", body.selectFirst("p")?.text())
        assertEquals("[bold]", body.selectFirst("p > b")?.text())
    }

    @Test
    fun testKeepOriginalText() {
        val body = parseBody("""<p>Hello</p>""")
        translate(body, keepOriginal = true)

        val p = body.selectFirst("p")!!
        assertEquals("Hello", p.childNode(0).toString())
        assertEquals("br", (p.childNode(1) as Element).tagName())
        assertEquals("[Hello]", (p.childNode(2) as TextNode).text())
    }

    @Test
    fun testKeepOriginalTextWithNestedInlineElements() {
        val body = parseBody("""<p>Hello <b>bold</b> world</p>""")
        translate(body, keepOriginal = true)

        val p = body.selectFirst("p")!!
        assertEquals("Hello ", (p.childNode(0) as TextNode).text())
        assertEquals("bold", (p.childNode(1) as Element).text())
        assertEquals(" world", (p.childNode(2) as TextNode).text())
        assertEquals("br", (p.childNode(3) as Element).tagName())
        assertEquals("[Hello ]", (p.childNode(4) as TextNode).text())
        assertEquals("[bold]", (p.childNode(5) as Element).text())
        assertEquals("[ world]", (p.childNode(6) as TextNode).text())
    }

    @Test
    fun testTextEntityRoundTrip() {
        val body = parseBody("""<p>a &lt; b &amp; c &gt; d</p>""")
        translate(body)

        assertEquals("[a < b & c > d]", body.selectFirst("p")?.text())
    }

    @Test
    fun testSkippedTagsAreNotTranslated() {
        val body = parseBody(
            """<p>Before</p><pre id="0">code block</pre><script>var x = 1;</script><p>After</p>"""
        )
        translate(body)

        assertEquals("code block", body.selectFirst("pre")?.text())
        assertEquals("var x = 1;", body.selectFirst("script")?.data())
        assertEquals("[Before]", body.select("p")[0].text())
        assertEquals("[After]", body.select("p")[1].text())
    }

    @Test
    fun testNotranslateClassIsSkipped() {
        val body = parseBody("""<p>Normal</p><p class="notranslate">Do not translate</p>""")
        translate(body)

        assertEquals("[Normal]", body.select("p")[0].text())
        assertEquals("Do not translate", body.select("p")[1].text())
    }

    @Test
    fun testSerializedTextsAreNotWrappedWithTopLevelPlaceholder() {
        val body = parseBody("""<p>text 1</p><p>text 1, <b>text 2</b></p>""")
        val texts = mutableListOf<List<String>>()
        HtmlDocumentationTranslator { input ->
            texts.add(input)
            input
        }.translateElement(body)

        assertEquals(1, texts.size)
        assertEquals(listOf("text 1", "text 1, <b10>text 2</b10>"), texts[0])
    }

    @Test
    fun testKeepOriginalTextWithMultipleSegmentsInSameParent() {
        val body = parseBody(
            """<table class="sections"><tr><td valign="top"><code>out</code> – The output stream""" +
                    """<br>""" +
                    """<code>autoFlush</code> – Whether the buffer will be flushed""" +
                    """<br>""" +
                    """<code>encoding</code> – The name of a supported """ +
                    """<a href="psi_element://java.lang###charenc"> character encoding</a></td></tr></table>"""
        )
        translate(body, keepOriginal = true)

        val td = body.selectFirst("td")!!
        assertEquals(19, td.childNodes().size)

        // Unit 1: original segment, then <br> + translated segment.
        assertEquals("out", (td.childNode(0) as Element).text())
        assertEquals(" – The output stream", (td.childNode(1) as TextNode).text())
        assertEquals("br", (td.childNode(2) as Element).tagName())
        assertEquals("out", (td.childNode(3) as Element).text())
        assertEquals("[ – The output stream]", (td.childNode(4) as TextNode).text())

        // The original <br> between unit 1 and unit 2.
        assertEquals("br", (td.childNode(5) as Element).tagName())

        // Unit 2.
        assertEquals("autoFlush", (td.childNode(6) as Element).text())
        assertEquals(" – Whether the buffer will be flushed", (td.childNode(7) as TextNode).text())
        assertEquals("br", (td.childNode(8) as Element).tagName())
        assertEquals("autoFlush", (td.childNode(9) as Element).text())
        assertEquals("[ – Whether the buffer will be flushed]", (td.childNode(10) as TextNode).text())

        // The original <br> between unit 2 and unit 3.
        assertEquals("br", (td.childNode(11) as Element).tagName())

        // Unit 3.
        assertEquals("encoding", (td.childNode(12) as Element).text())
        assertEquals(" – The name of a supported ", (td.childNode(13) as TextNode).text())
        assertEquals("a", (td.childNode(14) as Element).tagName())
        assertEquals("character encoding", (td.childNode(14) as Element).text())
        assertEquals("psi_element://java.lang###charenc", (td.childNode(14) as Element).attr("href"))
        assertEquals("br", (td.childNode(15) as Element).tagName())
        assertEquals("encoding", (td.childNode(16) as Element).text())
        assertEquals("[ – The name of a supported ]", (td.childNode(17) as TextNode).text())
        assertEquals("a", (td.childNode(18) as Element).tagName())
        assertEquals("[ character encoding]", (td.childNode(18) as Element).text())
        assertEquals("psi_element://java.lang###charenc", (td.childNode(18) as Element).attr("href"))
    }

    @Test
    fun testInvertedPunctuationIsTranslatable() {
        val body = parseBody("""<p>¿</p><p>¡</p>""")
        val texts = mutableListOf<String>()
        HtmlDocumentationTranslator { input ->
            texts.addAll(input)
            input.map(::fakeTranslate)
        }.translateElement(body)

        assertEquals(listOf("¿", "¡"), texts)
    }

    private fun parseBody(html: String): Element {
        return Jsoup.parse("<html><body>$html</body></html>").body()
    }

    private fun translate(body: Element, keepOriginal: Boolean = false) {
        HtmlDocumentationTranslator { texts -> texts.map(::fakeTranslate) }
            .translateElement(body, keepOriginal)
    }

    /**
     * Simulates the translation service: keeps HTML tags untouched, wraps text
     * segments with brackets and breaks placeholder tags by inserting spaces.
     */
    private fun fakeTranslate(text: String): String {
        val tagRegex = Regex("""<[^>]*>""")
        val result = StringBuilder()
        var lastEnd = 0
        for (match in tagRegex.findAll(text)) {
            val plain = text.substring(lastEnd, match.range.first)
            if (plain.isNotEmpty()) {
                result.append('[').append(plain).append(']')
            }
            result.append(breakPlaceholderTag(match.value))
            lastEnd = match.range.last + 1
        }

        val tail = text.substring(lastEnd)
        if (tail.isNotEmpty()) {
            result.append('[').append(tail).append(']')
        }
        return result.toString()
    }

    private fun breakPlaceholderTag(tag: String): String {
        return tag.replace(Regex("""<(/?b)(\d+)(\d+)>"""), "<$1$2 $3>")
    }
}
