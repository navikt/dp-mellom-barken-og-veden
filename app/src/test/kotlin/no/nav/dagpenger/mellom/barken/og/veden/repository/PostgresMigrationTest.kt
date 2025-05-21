package no.nav.dagpenger.mellom.barken.og.veden.repository

import io.kotest.matchers.ints.shouldBeExactly
import no.nav.dagpenger.mellom.barken.og.veden.PostgresConfiguration.runMigration
import no.nav.dagpenger.mellom.barken.og.veden.repository.Postgres.withCleanDb
import org.junit.jupiter.api.Test

class PostgresMigrationTest {
    @Test
    fun `Migration scripts are applied successfully`() {
        withCleanDb {
            val migrations = runMigration()
            migrations shouldBeExactly 1
        }
    }
}
