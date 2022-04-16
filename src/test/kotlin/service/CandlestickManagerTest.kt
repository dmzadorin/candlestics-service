package service

import Candlestick
import Instrument
import dao.InstrumentRepository
import dao.QuoteRepository
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals

internal class CandlestickManagerTest {
    @Test
    fun handleInstrument() {
        val instrumentRepository = mockk<InstrumentRepository>(relaxed = true)
        val quoteRepository = mockk<QuoteRepository>(relaxed = true)
        val candlestickManager = CandlestickManagerImpl(quoteRepository, instrumentRepository)
        val existingIsin = "abc"
        val instrument = Instrument(existingIsin, "test")

        val candleSticks = listOf(
            Candlestick(
                openTimestamp = Instant.now(),
                closeTimestamp = Instant.now().plusSeconds(1),
                openPrice = 10.0,
                highPrice = 10.0,
                lowPrice = 10.0,
                closingPrice = 10.0
            )
        )
        every { instrumentRepository.getInstrumentByIsin(existingIsin) } returns instrument
        every { quoteRepository.getCandlesticks(existingIsin, any()) } returns candleSticks
        assertEquals(candleSticks, candlestickManager.getCandlesticks(existingIsin))
        verify { instrumentRepository.getInstrumentByIsin(existingIsin) }
        verify { quoteRepository.getCandlesticks(existingIsin, CandlestickManagerImpl.DEFAULT_MINUTES) }

        clearAllMocks()

        val missingIsin = "def"
        every { instrumentRepository.getInstrumentByIsin(missingIsin) } returns null
        assertEquals(emptyList(), candlestickManager.getCandlesticks(missingIsin))
        verify { instrumentRepository.getInstrumentByIsin(missingIsin) }
        verify(exactly = 0) { quoteRepository.getCandlesticks(any(), any()) }
    }
}