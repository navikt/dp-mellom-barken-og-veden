package no.nav.dagpenger.mellom.barken.og.veden.repository

import com.zaxxer.hikari.HikariDataSource
import no.nav.dagpenger.mellom.barken.og.veden.PostgresConfiguration
import org.flywaydb.core.internal.configuration.ConfigUtils
import org.testcontainers.containers.PostgreSQLContainer
import kotlin.also
import kotlin.apply
import kotlin.run

internal object Postgres {
    val instance by lazy {
        PostgreSQLContainer<Nothing>("postgres:17.4").apply {
            withReuse(true)
            start()
        }
    }

    fun withMigratedDb(block: () -> Unit) {
        withCleanDb {
            PostgresConfiguration.runMigration()
            block()
        }
    }

    fun withMigratedDb(): HikariDataSource {
        setup()
        PostgresConfiguration.runMigration()
        return PostgresConfiguration.dataSource
    }

    fun setup() {
        System.setProperty(ConfigUtils.CLEAN_DISABLED, "false")
        System.setProperty(PostgresConfiguration.DB_URL_KEY, instance.jdbcUrl)
        System.setProperty(PostgresConfiguration.DB_USERNAME_KEY, instance.username)
        System.setProperty(PostgresConfiguration.DB_PASSWORD_KEY, instance.password)
    }

    fun tearDown() {
        System.clearProperty(PostgresConfiguration.DB_URL_KEY)
        System.clearProperty(PostgresConfiguration.DB_USERNAME_KEY)
        System.clearProperty(PostgresConfiguration.DB_PASSWORD_KEY)
        System.clearProperty(ConfigUtils.CLEAN_DISABLED)
    }

    fun withCleanDb(block: () -> Unit) {
        setup()
        PostgresConfiguration
            .clean()
            .run {
                block()
            }.also {
                tearDown()
            }
    }

    fun withCleanDb(
        target: String,
        setup: () -> Unit,
        test: () -> Unit,
    ) {
        this.setup()
        PostgresConfiguration
            .clean()
            .run {
                PostgresConfiguration.runMigrationTo(target)
                setup()
                PostgresConfiguration.runMigration()
                test()
            }.also {
                tearDown()
            }
    }
}
