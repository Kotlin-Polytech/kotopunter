package org.jetbrains.research.kotopunter.util.database

import com.zaxxer.hikari.HikariDataSource
import io.vertx.core.Vertx
import org.jetbrains.research.kotopunter.config.Config
import org.jetbrains.research.kotopunter.util.getSharedLocal
import javax.sql.DataSource

data class KotoedDataSource(val ds: DataSource, val url: String) : DataSource by ds

fun Vertx.getSharedDataSource(name: String = Config.Debug.Database.DataSourceId,
                              url: String = Config.Debug.Database.Url,
                              username: String = Config.Debug.Database.User,
                              password: String = Config.Debug.Database.Password): KotoedDataSource =
        getSharedLocal(name) {
            KotoedDataSource(
                    ds = HikariDataSource().apply {
                        this.username = username
                        this.password = password
                        this.jdbcUrl = url
                    },
                    url = url
            )
        }
