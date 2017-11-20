package org.jetbrains.research.kotopunter.config

import org.jooq.tools.jdbc.JDBCUtils

class GlobalConfig : Configuration() {
    class DebugConfig : Configuration() {
        class DBConfig : Configuration() {
            val Url by "jdbc:postgresql://localhost/kotopunter"
            val User by "kotopunter"
            val Password by "kotopunter"

            val Dialect get() = JDBCUtils.dialect(Url)

            val DataSourceId by "debug.db"
            val PoolSize: Int by { Runtime.getRuntime().availableProcessors() * 2 }
        }

        val Database by DBConfig()

        class MetricsConfig : Configuration() {
            val Enabled by true
        }

        val Metrics by MetricsConfig()
    }

    val Debug by DebugConfig()

    class GameConfig : Configuration() {
        val MapDirectory: String by "maps"
        val Timeout by 3.0
    }

    val Game by GameConfig()

    class MailConfig : Configuration() {
        val KotoedAddress: String by "kotopunter@jetbrains.com"
        val KotoedSignature: String by "Kotoed, the one and only"
        val ServerHost: String by "kspt.icc.spbstu.ru"

        val UseSSL: Boolean by false
        val UseTLS: Boolean by false

        val ServerPort: Int by { if (UseTLS) 587 else if (UseSSL) 465 else 25 }

        val User: String? by Null
        val Password: String? by Null
    }

    class NotificationsConfig : Configuration() {
        val PoolSize: Int by { Runtime.getRuntime().availableProcessors() }

        val Mail by MailConfig()
    }

    val Notifications by NotificationsConfig()

    class DispatcherConfig : Configuration() {
        class DefaultPortRange : Configuration() {
            val From by 9002
            val To by 9012
        }

        class DefaultInfTimeoutPortRange : Configuration() {
            val From by 10000
            val To by 10000
        }

        val PortRange by DefaultPortRange()

        val InfTimeoutPortRange by DefaultInfTimeoutPortRange()

        val MinPlayers by 2
        val MaxPlayers by 4
    }

    val Dispatcher by DispatcherConfig()

    class RootConfig : Configuration() {
        val Host: String by "http://localhost"
        val Port: Int by 9001
    }

    val Root by RootConfig()
}

val Config: GlobalConfig = loadConfiguration(GlobalConfig(),
        fromResource(System.getProperty("kotopunter.settingsFile", "defaultSettings.json")))
