package cn.yiiguxing.plugin.translate.util;

import com.intellij.credentialStore.CredentialAttributes;
import org.jetbrains.annotations.NotNull;

/**
 * Factory class for creating instances of {@link CredentialAttributes}.
 * <p>
 * Fix <a href="https://github.com/YiiGuxing/TranslationPlugin/issues/319">#319</a>
 */
public final class CredentialAttributesFactory {

    static CredentialAttributes create(@NotNull String serviceName, String userName) {
        return new CredentialAttributes(serviceName, userName);
    }

}
