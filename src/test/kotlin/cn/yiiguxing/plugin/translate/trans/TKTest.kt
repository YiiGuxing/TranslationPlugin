package cn.yiiguxing.plugin.translate.trans

import cn.yiiguxing.plugin.translate.trans.google.tk
import org.junit.Assert
import org.junit.Test

/**
 * TKTest
 */
class TKTest {

    @Test
    fun testTK() {
        val a = 202905874.toLong()
        val b = 544157181.toLong()
        val c = 419689.toLong()
        val tkk = c to a + b
        val tk1 = "Translate".tk(tkk)
        val tk2 = "Google translate".tk(tkk)

        Assert.assertEquals("34939.454418", tk1)
        Assert.assertEquals("671407.809414", tk2)
    }

}