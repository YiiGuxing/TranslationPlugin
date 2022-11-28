package cn.yiiguxing.plugin.translate.update

import org.junit.Assert
import org.junit.Test

class VersionTest {

    @Test
    fun `test version parsing`() {
        Version("1.2.3-beta.0+b1-2.3").let {
            Assert.assertEquals("Major", it.major, 1)
            Assert.assertEquals("Minor", it.minor, 2)
            Assert.assertEquals("Patch", it.patch, 3)
            Assert.assertEquals("Prerelease", it.prerelease, "beta.0")
            Assert.assertEquals("Build metadata", it.buildMetadata, "b1-2.3")
        }
    }

    @Test
    fun `test version forms`() {
        Version("1.2.3-beta.0+b1-2.3").let {
            Assert.assertEquals("Feature update version", it.getFeatureUpdateVersion(), "1.2")
            Assert.assertEquals("Stable version", it.getStableVersion(), "1.2.3")
            Assert.assertEquals("Version without build metadata", it.getVersionWithoutBuildMetadata(), "1.2.3-beta.0")
        }
    }

    @Test
    fun `test same version`() {
        (Version("1.2.3") to Version("1.2.3+b0")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} is same version as v${v2.version}.", v1.isSameVersion(v2))
        }
        (Version("1.2.3+b1") to Version("1.2.3+b2")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} is same version as v${v2.version}.", v1.isSameVersion(v2))
        }
        (Version("1.2.3+b1") to Version("1.2.3-beta+b2")).let { (v1, v2) ->
            Assert.assertFalse("v${v1.version} is not same version as v${v2.version}.", v1.isSameVersion(v2))
        }
    }

    @Test
    fun `test feature update version`() {
        (Version("1.0.0") to Version("0.1.0")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} is a feature update version of v${v2.version}.", v1.isFeatureUpdateOf(v2))
        }
        (Version("1.2.0") to Version("1.1.0")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} is a feature update version of v${v2.version}.", v1.isFeatureUpdateOf(v2))
        }
        (Version("1.2.0") to Version("1.1.1+b0")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} is a feature update version of v${v2.version}.", v1.isFeatureUpdateOf(v2))
        }
        (Version("1.2.0") to Version("1.2.0-alpha.1")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} is a feature update version of v${v2.version}.", v1.isFeatureUpdateOf(v2))
        }
        (Version("1.2.0") to Version("1.2.0-alpha.1+b0")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} is a feature update version of v${v2.version}.", v1.isFeatureUpdateOf(v2))
        }
        (Version("1.2.3") to Version("1.2.0-alpha.1+b0")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} is a feature update version of v${v2.version}.", v1.isFeatureUpdateOf(v2))
        }
        (Version("1.2.3") to Version("1.1.1")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} is a feature update version of v${v2.version}.", v1.isFeatureUpdateOf(v2))
        }
        (Version("1.2.3") to Version("1.1.1-alpha.1")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} is a feature update version of v${v2.version}.", v1.isFeatureUpdateOf(v2))
        }
        (Version("1.2.0-beta.1") to Version("1.0.0")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} is a feature update version of v${v2.version}.", v1.isFeatureUpdateOf(v2))
        }
        (Version("1.2.0-beta.1") to Version("1.0.0-alpha.1")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} is a feature update version of v${v2.version}.", v1.isFeatureUpdateOf(v2))
        }
        (Version("1.2.1") to Version("1.2.0")).let { (v1, v2) ->
            Assert.assertFalse(
                "v${v1.version} is not a feature update version of v${v2.version}.",
                v1.isFeatureUpdateOf(v2)
            )
        }
        (Version("1.2.0") to Version("1.2.1-alpha.1")).let { (v1, v2) ->
            Assert.assertFalse(
                "v${v1.version} is not a feature update version of v${v2.version}.",
                v1.isFeatureUpdateOf(v2)
            )
        }
        (Version("1.2.0-beta.1") to Version("1.2.0-alpha.1")).let { (v1, v2) ->
            Assert.assertFalse(
                "v${v1.version} is not a feature update version of v${v2.version}.",
                v1.isFeatureUpdateOf(v2)
            )
        }
        (Version("1.2.0+b0") to Version("1.2.0")).let { (v1, v2) ->
            Assert.assertFalse(
                "v${v1.version} is not a feature update version of v${v2.version}.",
                v1.isFeatureUpdateOf(v2)
            )
        }
        (Version("1.2.0") to Version("1.2.0+b0")).let { (v1, v2) ->
            Assert.assertFalse(
                "v${v1.version} is not a feature update version of v${v2.version}.",
                v1.isFeatureUpdateOf(v2)
            )
        }
    }

    @Test
    fun `test version comparison`() {
        (Version("1.0.0") to Version("1.0.0")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} == v${v2.version}", v1 == v2)
        }
        (Version("1.0.0") to Version("1.0.0+b0")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} == v${v2.version}", v1 == v2)
        }
        (Version("1.0.0+b0") to Version("1.0.0")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} == v${v2.version}", v1 == v2)
        }
        (Version("1.0.0+b0") to Version("1.0.0+b1")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} == v${v2.version}", v1 == v2)
        }
        (Version("1.0.0") to Version("1.0.0-alpha1")).let { (v1, v2) ->
            Assert.assertFalse("v${v1.version} != v${v2.version}", v1 == v2)
        }
        (Version("1.0.0") to Version("1.0.0+b0")).let { (v1, v2) ->
            Assert.assertFalse("v${v1.version} is not exactly equal to v${v2.version}", v1.isEqual(v2))
        }

        (Version("0.0.1") to Version("0.0.0")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} > v${v2.version}", v1 > v2)
        }
        (Version("0.1.0") to Version("0.0.0")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} > v${v2.version}", v1 > v2)
        }
        (Version("0.1.0") to Version("0.0.1")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} > v${v2.version}", v1 > v2)
        }
        (Version("0.1.0") to Version("0.0.1")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} > v${v2.version}", v1 > v2)
        }
        (Version("1.0.0") to Version("0.0.0")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} > v${v2.version}", v1 > v2)
        }
        (Version("1.0.0") to Version("0.0.1")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} > v${v2.version}", v1 > v2)
        }
        (Version("1.0.0") to Version("0.1.0")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} > v${v2.version}", v1 > v2)
        }
        (Version("1.0.0") to Version("0.1.1")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} > v${v2.version}", v1 > v2)
        }
        (Version("1.0.0-alpha") to Version("0.1.0")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} > v${v2.version}", v1 > v2)
        }
        (Version("1.0.0+b1") to Version("0.1.0")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} > v${v2.version}", v1 > v2)
        }
        (Version("1.0.0") to Version("1.0.0-alpha")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} > v${v2.version}", v1 > v2)
        }
        (Version("1.0.0") to Version("1.0.0-alpha+b0")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} > v${v2.version}", v1 > v2)
        }
        (Version("1.0.0-beta") to Version("1.0.0-alpha")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} > v${v2.version}", v1 > v2)
        }
        (Version("1.0.0-beta") to Version("1.0.0-alpha.1")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} > v${v2.version}", v1 > v2)
        }
        (Version("1.0.0-beta.1") to Version("1.0.0-beta")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} > v${v2.version}", v1 > v2)
        }
        (Version("1.0.0-beta.2") to Version("1.0.0-beta.1")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} > v${v2.version}", v1 > v2)
        }
        (Version("1.0.0-beta-b") to Version("1.0.0-beta-a")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} > v${v2.version}", v1 > v2)
        }

        (Version("1.0.0") to Version("0.1.0")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} >= v${v2.version}", v1 >= v2)
        }
        (Version("1.0.0") to Version("1.0.0")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} >= v${v2.version}", v1 >= v2)
        }
        (Version("1.0.0") to Version("1.0.0+b0")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} >= v${v2.version}", v1 >= v2)
        }
        (Version("1.0.0+b0") to Version("1.0.0")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} >= v${v2.version}", v1 >= v2)
        }
        (Version("1.0.0+b0") to Version("1.0.0+b1")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} >= v${v2.version}", v1 >= v2)
        }

        (Version("1.0.0") to Version("2.0.0")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} < v${v2.version}", v1 < v2)
        }
        (Version("0.0.1") to Version("0.0.2")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} < v${v2.version}", v1 < v2)
        }
        (Version("0.1.0") to Version("0.1.1")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} < v${v2.version}", v1 < v2)
        }
        (Version("0.1.0") to Version("0.2.0")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} < v${v2.version}", v1 < v2)
        }
        (Version("1.0.0-alpha") to Version("1.0.0")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} < v${v2.version}", v1 < v2)
        }
        (Version("1.0.0-alpha") to Version("1.0.0-alpha.1")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} < v${v2.version}", v1 < v2)
        }
        (Version("1.0.0-alpha.1") to Version("1.0.0-alpha.2")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} < v${v2.version}", v1 < v2)
        }
        (Version("1.0.0-alpha") to Version("1.0.0-beta")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} < v${v2.version}", v1 < v2)
        }
        (Version("1.0.0") to Version("1.0.0")).let { (v1, v2) ->
            Assert.assertTrue("v${v1.version} <= v${v2.version}", v1 <= v2)
        }
    }

}