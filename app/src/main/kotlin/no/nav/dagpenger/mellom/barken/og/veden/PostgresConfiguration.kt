package no.nav.dagpenger.mellom.barken.og.veden

import ch.qos.logback.core.util.OptionHelper
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration

// Understands how to create a data source from environment variables
internal object PostgresConfiguration {
    const val DB_USERNAME_KEY = "DB_USERNAME"
    const val DB_PASSWORD_KEY = "DB_PASSWORD"
    const val DB_URL_KEY = "DB_JDBC_URL"

    private fun getOrThrow(key: String): String = OptionHelper.getEnv(key) ?: OptionHelper.getSystemProperty(key)

    val dataSource by lazy {
        HikariDataSource().apply {
            jdbcUrl = getOrThrow(DB_URL_KEY).ensurePrefix("jdbc:postgresql://").stripCredentials()
            username = getOrThrow(DB_USERNAME_KEY)
            password = getOrThrow(DB_PASSWORD_KEY)
            maximumPoolSize = 10
            minimumIdle = 1
            idleTimeout = 10001
            connectionTimeout = 1000
            maxLifetime = 30001
            initializationFailTimeout = 5000
        }
    }

    private fun flyWayBuilder() = Flyway.configure().connectRetries(10)

    private val flyWayBuilder: FluentConfiguration = Flyway.configure().connectRetries(10)

    fun clean() =
        flyWayBuilder
            .cleanDisabled(
                // temp
                false,
            ).dataSource(dataSource)
            .load()
            .clean()

    internal fun runMigration(initSql: String? = null): Int =
        flyWayBuilder
            .dataSource(dataSource)
            .initSql(initSql)
            .load()
            .migrate()
            .migrations
            .size

    internal fun runMigrationTo(target: String): Int =
        flyWayBuilder()
            .dataSource(dataSource)
            .target(target)
            .load()
            .migrate()
            .migrations
            .size
}

private fun String.stripCredentials() = this.replace(Regex("://.*:.*@"), "://")

private fun String.ensurePrefix(prefix: String) =
    if (this.startsWith(prefix)) {
        this
    } else {
        prefix + this.substringAfter("//")
    }
