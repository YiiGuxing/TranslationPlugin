package cn.yiiguxing.plugin.translate.trans

import cn.yiiguxing.plugin.translate.trans.documentation.HtmlDocumentationTranslator
import cn.yiiguxing.plugin.translate.trans.documentation.RawHtmlTranslationStrategy
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * RawHtmlTranslationStrategyTest
 */
class RawHtmlTranslationStrategyTest {

    @Test
    fun testSegmentsAreSerializedAsRawHtml() {
        val body = parseBody("""<p>Hello <b>bold</b> world</p>""")
        val texts = mutableListOf<String>()
        HtmlDocumentationTranslator(RawHtmlTranslationStrategy) { input ->
            texts.addAll(input)
            input.map(::fakeTranslate)
        }.translateElement(body)

        assertEquals(listOf("Hello <b>bold</b> world"), texts)
        assertEquals("[Hello ][bold][ world]", body.selectFirst("p")?.text())
    }

    @Test
    fun testAttributesArePassedToTranslatorAndPreserved() {
        val body = parseBody("""<p>See <a href="https://example.com/doc">the documentation</a> now</p>""")
        val texts = mutableListOf<String>()
        HtmlDocumentationTranslator(RawHtmlTranslationStrategy) { input ->
            texts.addAll(input)
            input.map(::fakeTranslate)
        }.translateElement(body)

        assertEquals(listOf("""See <a href="https://example.com/doc">the documentation</a> now"""), texts)
        assertEquals("https://example.com/doc", body.selectFirst("a")?.attr("href"))
        assertEquals("[the documentation]", body.selectFirst("a")?.text())
    }

    @Test
    fun testProtectedInlineTagsKeepOriginalContent() {
        val body = parseBody("""<p>The <code>body</code> element.</p>""")
        translate(body)

        assertEquals("body", body.selectFirst("code")?.text())
        assertEquals("[The ]body[ element.]", body.selectFirst("p")?.text())
    }

    @Test
    fun testKeepOriginalText() {
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
        HtmlDocumentationTranslator(RawHtmlTranslationStrategy) { texts -> texts.map(::fakeTranslate) }
            .translateElement(body, keepOriginal)
    }

    /**
     * Simulates the translation service: keeps HTML tags untouched and wraps
     * text segments with brackets.
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
            result.append(match.value)
            lastEnd = match.range.last + 1
        }

        val tail = text.substring(lastEnd)
        if (tail.isNotEmpty()) {
            result.append('[').append(tail).append(']')
        }
        return result.toString()
    }
}
