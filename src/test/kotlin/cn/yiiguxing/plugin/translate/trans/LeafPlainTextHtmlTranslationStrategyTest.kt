package cn.yiiguxing.plugin.translate.trans

import cn.yiiguxing.plugin.translate.trans.documentation.HtmlDocumentationTranslator
import cn.yiiguxing.plugin.translate.trans.documentation.LeafPlainTextHtmlTranslationStrategy
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * LeafPlainTextHtmlTranslationStrategyTest
 */
class LeafPlainTextHtmlTranslationStrategyTest {

    @Test
    fun testEachLeafIsTranslatedSeparatelyAsPlainText() {
        val body = parseBody("""<p>Hello <b>bold</b> world</p>""")
        val texts = mutableListOf<String>()
        HtmlDocumentationTranslator(LeafPlainTextHtmlTranslationStrategy) { input ->
            texts.addAll(input)
            input.map(::fakeTranslate)
        }.translateElement(body)

        assertEquals(listOf("Hello ", "bold", " world"), texts)
        assertEquals("[Hello ][bold][ world]", body.selectFirst("p")?.text())
    }

    @Test
    fun testNoHtmlTagsAreSentToTranslator() {
        val body = parseBody("""<p>See <a href="https://example.com/doc">the documentation</a> now</p>""")
        val texts = mutableListOf<String>()
        HtmlDocumentationTranslator(LeafPlainTextHtmlTranslationStrategy) { input ->
            texts.addAll(input)
            input.map(::fakeTranslate)
        }.translateElement(body)

        assertEquals(listOf("See ", "the documentation", " now"), texts)
        assertEquals("https://example.com/doc", body.selectFirst("a")?.attr("href"))
        assertEquals("[the documentation]", body.selectFirst("a")?.text())
    }

    @Test
    fun testProtectedInlineTagsAreNotTranslated() {
        val body = parseBody("""<p>The <code>body</code> element.</p>""")
        val texts = mutableListOf<String>()
        HtmlDocumentationTranslator(LeafPlainTextHtmlTranslationStrategy) { input ->
            texts.addAll(input)
            input.map(::fakeTranslate)
        }.translateElement(body)

        assertEquals(listOf("The ", " element."), texts)
        assertEquals("body", body.selectFirst("code")?.text())
        assertEquals("[The ]body[ element.]", body.selectFirst("p")?.text())
    }

    @Test
    fun testKeepOriginalText() {
        val body = parseBody("""<p>Hello</p>""")
        translate(body, keepOriginal = true)

        val paragraphs = body.select("p")
        assertEquals(2, paragraphs.size)
        assertEquals("Hello", paragraphs[0].text())
        assertEquals("br", paragraphs[0].nextElementSibling()?.tagName())
        assertEquals("[Hello]", paragraphs[1].text())
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
        HtmlDocumentationTranslator(LeafPlainTextHtmlTranslationStrategy) { texts -> texts.map(::fakeTranslate) }
            .translateElement(body, keepOriginal)
    }

    private fun fakeTranslate(text: String): String = "[$text]"
}
