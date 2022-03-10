package cn.yiiguxing.plugin.translate.util

import cn.yiiguxing.plugin.translate.trans.Lang
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.util.*

private object LanguageTypeAdapter : TypeAdapter<Lang>() {
    override fun write(out: JsonWriter, value: Lang?) {
        if (value == null) {
            out.nullValue()
            return
        }

        out.value(value.code)
    }

    override fun read(jsonReader: JsonReader): Lang? {
        if (jsonReader.peek() == JsonToken.NULL) {
            jsonReader.nextNull()
            return null
        }

        return Lang[jsonReader.nextString()]
    }

}

private object DateTypeAdapter : TypeAdapter<Date>() {
    override fun write(out: JsonWriter, value: Date?) {
        if (value == null) {
            out.nullValue()
            return
        }

        out.value(value.time)
    }

    override fun read(jsonReader: JsonReader): Date? {
        if (jsonReader.peek() == JsonToken.NULL) {
            jsonReader.nextNull()
            return null
        }

        return Date(jsonReader.nextLong())
    }
}

fun GsonBuilder.registerDefaultTypeAdapter(): GsonBuilder = apply {
    registerTypeAdapter(Lang::class.java, LanguageTypeAdapter)
    registerTypeAdapter(Date::class.java, DateTypeAdapter)
    registerTypeAdapter(java.sql.Date::class.java, DateTypeAdapter)
}