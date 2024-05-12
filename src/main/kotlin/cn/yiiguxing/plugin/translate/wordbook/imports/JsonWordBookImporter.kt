package cn.yiiguxing.plugin.translate.wordbook.imports

import cn.yiiguxing.plugin.translate.util.registerDefaultTypeAdapter
import cn.yiiguxing.plugin.translate.util.type
import cn.yiiguxing.plugin.translate.wordbook.WordBookItem
import com.google.gson.GsonBuilder
import java.io.InputStream
import java.io.InputStreamReader

class JsonWordBookImporter : WordBookImporter {
    override fun InputStream.readWords(): List<WordBookItem> {
        val reader = InputStreamReader(this, Charsets.UTF_8)

        return GsonBuilder()
            .registerDefaultTypeAdapter()
            .create()
            .fromJson(reader, type<List<WordBookItem>>())
    }
}