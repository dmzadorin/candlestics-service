package dao

import Instruments
import Quote
import Quotes
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.testcontainers.containers.TimescaleDBContainerProvider
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
open class TestContainersBase {
    companion object {
        @Container
        val timescaleContainer = TimescaleDBContainerProvider()
            .newInstance("latest-pg14")
            .withInitScript("db/init.sql")
    }

    @BeforeEach
    fun setupDb() {
        DbManager(timescaleContainer.jdbcUrl, timescaleContainer.username, timescaleContainer.password).initConnect
        //need to clean up previous inserted quotes/instruments
        transaction {
            Quotes.deleteAll()
            Instruments.deleteAll()
        }
    }
}

