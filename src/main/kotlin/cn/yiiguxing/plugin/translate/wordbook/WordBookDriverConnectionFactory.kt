package cn.yiiguxing.plugin.translate.wordbook

import org.apache.commons.dbcp2.DriverConnectionFactory
import org.sqlite.BusyHandler
import java.sql.Connection
import java.sql.Driver
import java.util.*

@Suppress("unused") // unused in wordbook service
internal class WordBookDriverConnectionFactory(
    driver: Driver?,
    connectString: String?,
    properties: Properties?
) : DriverConnectionFactory(driver, connectString, properties) {

    private val busyHandler = object : BusyHandler() {
        override fun callback(nbPrevInvok: Int): Int = 1
    }

    override fun createConnection(): Connection {
        return super.createConnection().also {
            BusyHandler.setHandler(it, busyHandler)
        }
    }
}