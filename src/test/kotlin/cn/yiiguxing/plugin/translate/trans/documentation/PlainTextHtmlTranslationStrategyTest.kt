package cn.yiiguxing.plugin.translate.trans.documentation

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * PlainTextHtmlTranslationStrategyTest
 */
class PlainTextHtmlTranslationStrategyTest {

    @Test
    fun testSegmentsAreSerializedAsPlainTextWithoutTags() {
        val body = parseBody("""<p class="p-doc">See <a href="https://example.com/doc">the documentation</a> now</p>""")
        val texts = mutableListOf<String>()
        HtmlDocumentationTranslator(PlainTextHtmlTranslationStrategy) { input ->
            texts.addAll(input)
            input.map(::fakeTranslate)
        }.translateElement(body)

        assertEquals(listOf("See the documentation now"), texts)
        assertNull(body.selectFirst("a"))
        assertEquals("p-doc", body.selectFirst("p")?.attr("class"))
        assertEquals("[See the documentation now]", body.selectFirst("p")?.text())
    }

    @Test
    fun testAdjacentNodesAreMergedAndTagsAreStripped() {
        val body = parseBody("""<p>Hello <b>bold</b> world</p>""")
        val texts = mutableListOf<String>()
        HtmlDocumentationTranslator(PlainTextHtmlTranslationStrategy) { input ->
            texts.addAll(input)
            input.map(::fakeTranslate)
        }.translateElement(body)

        assertEquals(listOf("Hello bold world"), texts)
        assertNull(body.selectFirst("b"))
        assertEquals("[Hello bold world]", body.selectFirst("p")?.text())
    }

    @Test
    fun testProtectedInlineTagsAreNotRestored() {
        val body = parseBody("""<p>The <code>body</code> element.</p>""")
        val texts = mutableListOf<String>()
        HtmlDocumentationTranslator(PlainTextHtmlTranslationStrategy) { input ->
            texts.addAll(input)
            input.map(::fakeTranslate)
        }.translateElement(body)

        assertEquals(listOf("The body element."), texts)
        assertNull(body.selectFirst("code"))
        assertEquals("[The body element.]", body.selectFirst("p")?.text())
    }

    @Test
    fun testKeepOriginalText() {
        val body = parseBody("""<p>Hello</p>""")
        translate(body, keepOriginal = true)

        val p = body.selectFirst("p")!!
        assertEquals("Hello", (p.childNode(0) as TextNode).text())
        assertEquals("br", (p.childNode(1) as Element).tagName())
        assertEquals("[Hello]", (p.childNode(2) as TextNode).text())
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

    private fun parseBody(html: String): Element {
        return Jsoup.parse("<html><body>$html</body></html>").body()
    }

    private fun translate(body: Element, keepOriginal: Boolean = false) {
        HtmlDocumentationTranslator(PlainTextHtmlTranslationStrategy) { texts -> texts.map(::fakeTranslate) }
            .translateElement(body, keepOriginal)
    }

    private fun fakeTranslate(text: String): String = "[$text]"
}
