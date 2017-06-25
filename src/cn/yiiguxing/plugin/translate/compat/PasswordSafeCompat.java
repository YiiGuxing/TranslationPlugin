package cn.yiiguxing.plugin.translate.compat;

import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.ide.passwordSafe.PasswordSafeException;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PasswordSafeCompat
 * <p>
 * Created by Yii.Guxing on 2017-06-12.
 */
public final class PasswordSafeCompat {

    private static final Logger LOG = Logger.getInstance("#" + PasswordSafeCompat.class.getCanonicalName());

    @SuppressWarnings({"deprecation", "SpellCheckingInspection"})
    public static void setPassword(@NotNull Class<?> requestor, @NotNull String accountName, @Nullable String value) {
        if (IdeaCompat.BUILD_NUMBER > IdeaCompat.Version.IDEA2016_2) {
            PasswordSafe.getInstance().setPassword(requestor, accountName, value);
        } else {
            PasswordSafe.getInstance().storePassword(null, requestor, accountName, value);
        }
    }

    @SuppressWarnings({"deprecation", "SpellCheckingInspection"})
    @Nullable
    public static String getPassword(@NotNull Class<?> requestor, @NotNull String accountName) {
        if (IdeaCompat.BUILD_NUMBER > IdeaCompat.Version.IDEA2016_2) {
            return PasswordSafe.getInstance().getPassword(requestor, accountName);
        } else {
            String password;
            try {
                password = PasswordSafe.getInstance().getPassword(null, requestor, accountName);
            } catch (PasswordSafeException e) {
                LOG.info("Couldn't get password for key [" + accountName + "]", e);
                password = "";
            }

            return password;
        }
    }

}
