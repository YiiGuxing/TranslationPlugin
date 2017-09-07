package cn.yiiguxing.plugin.translate;

import org.jetbrains.annotations.NotNull;

/**
 * CacheKey
 * <p>
 * Created by Yii.Guxing on 2017-06-11.
 */
public final class CacheKey {

    public final Lang langFrom;
    public final Lang langTo;
    public final String queryText;

    public CacheKey(@NotNull Lang langFrom, @NotNull Lang langTo, @NotNull String queryText) {
        this.langFrom = langFrom;
        this.langTo = langTo;
        this.queryText = queryText;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CacheKey cacheKey = (CacheKey) o;

        return langFrom == cacheKey.langFrom
                && langTo == cacheKey.langTo
                && queryText.equals(cacheKey.queryText);
    }

    @Override
    public int hashCode() {
        int result = langFrom.hashCode();
        result = 31 * result + langTo.hashCode();
        result = 31 * result + queryText.hashCode();
        return result;
    }
}
