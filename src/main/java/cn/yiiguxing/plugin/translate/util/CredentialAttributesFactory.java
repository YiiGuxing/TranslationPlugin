package cn.yiiguxing.plugin.translate.util;

import com.intellij.credentialStore.CredentialAttributes;
import org.jetbrains.annotations.NotNull;

/**
 * CredentialAttributesFactory
 * <p>
 * https://github.com/YiiGuxing/TranslationPlugin/issues/319
 */
public final class CredentialAttributesFactory {

    static CredentialAttributes create(@NotNull String serviceName, String userName) {
        return new CredentialAttributes(serviceName, userName);
    }

}
