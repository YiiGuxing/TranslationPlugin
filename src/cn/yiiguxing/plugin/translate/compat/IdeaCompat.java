package cn.yiiguxing.plugin.translate.compat;

import com.intellij.openapi.application.ApplicationInfo;

@SuppressWarnings({"SpellCheckingInspection", "WeakerAccess", "unused"})
public final class IdeaCompat {

    public static final int BUILD_NUMBER = ApplicationInfo.getInstance().getBuild().getBaselineVersion();

    public static final class Version {
        public static final int IDEA15 = 143;
        public static final int IDEA2016_1 = 145;
        public static final int IDEA2016_2 = 162;

        private Version() {
        }
    }

    private IdeaCompat() {
    }

}
