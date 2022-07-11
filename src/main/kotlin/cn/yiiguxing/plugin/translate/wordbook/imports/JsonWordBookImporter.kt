package cn.yiiguxing.plugin.translate.wordbook.imports

import cn.yiiguxing.plugin.translate.util.registerDefaultTypeAdapter
import cn.yiiguxing.plugin.translate.wordbook.WordBookItem
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.InputStream
import java.io.InputStreamReader

class JsonWordBookImporter : WordBookImporter {
    override fun InputStream.readWords(): List<WordBookItem> {
        val reader = InputStreamReader(this)
        val type = object : TypeToken<List<WordBookItem>>() {}.type

        return GsonBuilder()
            .registerDefaultTypeAdapter()
            .create()
            .fromJson(reader, type)
    }
}