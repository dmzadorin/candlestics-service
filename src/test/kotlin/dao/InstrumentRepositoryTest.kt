package dao

import Instrument
import Instruments
import Quotes
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils
import java.math.BigDecimal
import java.time.Instant

internal class InstrumentRepositoryTest : TestContainersBase() {

    private lateinit var description: String
    private lateinit var isin: String
    private lateinit var instrumentRepository: InstrumentRepository

    @BeforeEach
    fun setup() {
        instrumentRepository = InstrumentRepository()
        isin = RandomStringUtils.randomAlphabetic(10)
        description = RandomStringUtils.randomAlphabetic(10)
    }

    @Test
    fun getInstrumentByIsin() {
        transaction {
            Instruments.insert {
                it[isin] = this@InstrumentRepositoryTest.isin
                it[description] = this@InstrumentRepositoryTest.description
            }
        }
        assertEquals(Instrument(isin, description), instrumentRepository.getInstrumentByIsin(isin))
    }

    @Test
    fun saveInstrument() {
        instrumentRepository.saveInstrument(Instrument(isin, description))
        val savedInstrument = getInstrument(isin)
        assertEquals(savedInstrument[Instruments.isin], isin)
        assertEquals(savedInstrument[Instruments.description], description)
        val newDescription = "test"
        instrumentRepository.saveInstrument(Instrument(isin, newDescription))
        val updatedInstrument = getInstrument(isin)
        assertEquals(updatedInstrument[Instruments.isin], isin)
        assertEquals(updatedInstrument[Instruments.description], newDescription)
    }

    @Test
    fun deleteInstrument() {
        val instrument = Instrument(isin, description)
        instrumentRepository.saveInstrument(instrument)
        transaction {
            Quotes.insert {
                it[isin] = this@InstrumentRepositoryTest.isin
                it[price] = 10.0.toBigDecimal()
                it[ts] = Instant.now()
            }
        }
        instrumentRepository.deleteInstrument(instrument)
        val instruments = transaction {
            Instruments.selectAll().toList()
        }
        assertTrue(instruments.isEmpty())
        val quotes = transaction {
            Quotes.selectAll().toList()
        }
        assertTrue(quotes.isEmpty())
    }

    private fun getInstrument(isin: String): ResultRow {
        return transaction {
            Instruments.select {
                Instruments.isin eq isin
            }.firstOrNull() ?: fail("Instrument $isin not found")
        }
    }
}