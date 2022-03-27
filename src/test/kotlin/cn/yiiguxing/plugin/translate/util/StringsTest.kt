package cn.yiiguxing.plugin.translate.util

import org.junit.Assert
import org.junit.Test

/**
 * StringsTest
 */
class StringsTest {

    @Test
    fun testSplitWord() {
        Assert.assertEquals("split Word", "splitWord".splitWords())
        Assert.assertEquals("Split Word", "SplitWord".splitWords())
        Assert.assertEquals("Split Word", "Split_Word".splitWords())
        Assert.assertEquals("split word", "split_word".splitWords())
        Assert.assertEquals("SPLIT WORD", "SPLIT_WORD".splitWords())
        Assert.assertEquals("split WORD", "splitWORD".splitWords())
        Assert.assertEquals("SPLIT Word", "SPLITWord".splitWords())
        Assert.assertEquals(" split  Word  Split Word;", " split  Word  SplitWord;".splitWords())
        Assert.assertEquals("0 word 1", "0word1".splitWords())
        Assert.assertEquals("word字", "word字".splitWords())
        Assert.assertEquals(
            "0 split Word Split Word Split WORD SPLIT Word字",
            "0splitWordSplitWordSplitWORD_SPLITWord字".splitWords()
        )
    }

    @Test
    fun testSplitSentence() {
        """If baby only wanted to, he could fly up to heaven this moment.
            |It is not for nothing that he does not leave us.
            |He loves to rest his head on mother's bosom, and cannot ever bear to lose sight of her."""
            .trimMargin()
            .splitSentence(60)
            .toTypedArray()
            .let {
                Assert.assertArrayEquals(
                    arrayOf(
                        "If baby only wanted to,",
                        "he could fly up to heaven this moment.",
                        "It is not for nothing that he does not leave us.",
                        "He loves to rest his head on mother's bosom,",
                        "and cannot ever bear to lose sight of her."
                    ), it
                )
            }

        """Baby knows all manner of wise words, though few on earth can understand their meaning.
            |It is not for nothing that he never wants to speak.
            |The one things he wants is to learn mother's words from mother's lips.
            |That is why he looks so innocent.
            |Baby had a heap of gold and pearls, yet he came like a beggar on to this earth.
            |It is not for nothing that he came in such a disguise.
            |This dear little naked mendicant pretends to be utterly helpless,
            |so that he may beg for mother's wealth of love."""
            .trimMargin()
            .splitSentence(200)
            .toTypedArray()
            .let {
                Assert.assertArrayEquals(
                    arrayOf(
                        "Baby knows all manner of wise words, though few on earth can understand their meaning. " +
                                "It is not for nothing that he never wants to speak.",
                        "The one things he wants is to learn mother's words from mother's lips. " +
                                "That is why he looks so innocent. Baby had a heap of gold and pearls, " +
                                "yet he came like a beggar on to this earth.",
                        "It is not for nothing that he came in such a disguise. " +
                                "This dear little naked mendicant pretends to be utterly helpless, " +
                                "so that he may beg for mother's wealth of love."
                    ), it
                )
            }

        "Baby knows all manner of wise words"
            .splitSentence(15)
            .toTypedArray()
            .let {
                Assert.assertArrayEquals(arrayOf("Baby knows all", "manner of wise", "words"), it)
            }

        """东风夜放花千树。更吹落、星如雨。宝马雕车香满路。凤箫声动，玉壶光转，一夜鱼龙舞。
            |蛾儿雪柳黄金缕。笑语盈盈暗香去。众里寻他千百度。蓦然回首，那人却在，灯火阑珊处。"""
            .trimMargin()
            .splitSentence(45)
            .toTypedArray()
            .let {
                Assert.assertArrayEquals(
                    arrayOf(
                        "东风夜放花千树。更吹落、星如雨。宝马雕车香满路。凤箫声动，玉壶光转，一夜鱼龙舞。",
                        "蛾儿雪柳黄金缕。笑语盈盈暗香去。众里寻他千百度。蓦然回首，那人却在，灯火阑珊处。"
                    ), it
                )
            }

        "蓦然回首那人却在灯火阑珊处"
            .splitSentence(4)
            .toTypedArray()
            .let {
                Assert.assertArrayEquals(arrayOf("蓦然回首", "那人却在", "灯火阑珊", "处"), it)
            }
    }

    @Test
    fun testEllipsis() {
        Assert.assertEquals("...", "".ellipsis(0))
        Assert.assertEquals("", "".ellipsis(1))
        Assert.assertEquals(
            "...",
            "If baby only wanted to, he could fly up to heaven this moment.".ellipsis(0)
        )
        Assert.assertEquals(
            "If baby on...",
            "If baby only wanted to, he could fly up to heaven this moment.".ellipsis(10)
        )
        Assert.assertEquals("baby", "baby".ellipsis(4))
        Assert.assertEquals("baby", "baby".ellipsis(10))
    }

    @Test
    fun testSingleLine() {
        Assert.assertEquals(" ", "\n".singleLine())
        Assert.assertEquals(" ", "\r".singleLine())
        Assert.assertEquals(" ", "\r\n".singleLine())
        Assert.assertEquals("  ", "\n\r".singleLine())
        Assert.assertEquals("  ", "\n\r\n".singleLine())
        Assert.assertEquals("  ", "\r\n\r".singleLine())
        Assert.assertEquals("  ", "\r\n\n".singleLine())
        Assert.assertEquals("   ", "\r\r\r".singleLine())
        Assert.assertEquals("   ", "\n\n\n".singleLine())
        Assert.assertEquals("   ", "\r\n\r\r".singleLine())
        Assert.assertEquals("   ", "\r\r\n\r".singleLine())
        Assert.assertEquals("   ", "\r\r\r\n".singleLine())
        Assert.assertEquals("   ", "\r\n\n\n".singleLine())
        Assert.assertEquals("   ", "\n\r\n\n".singleLine())
        Assert.assertEquals("   ", "\n\n\r\n".singleLine())
    }

    @Test
    fun testCompressWhitespace() {
        Assert.assertEquals(" ", " ".compressWhitespace())
        Assert.assertEquals(" ", "  ".compressWhitespace())
        Assert.assertEquals(" ", "    ".compressWhitespace())
        Assert.assertEquals(" a b c ", " a  b   c    ".compressWhitespace())
    }

}