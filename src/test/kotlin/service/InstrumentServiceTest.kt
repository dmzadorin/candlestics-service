package service

import Instrument
import InstrumentEvent
import dao.InstrumentRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class InstrumentServiceTest {

    @Test
    fun handleInstrument() {
        val instrumentRepository = mockk<InstrumentRepository>(relaxed = true)
        val service = InstrumentService(instrumentRepository)
        val instrument = Instrument("abc", "test")

        service.handleInstrument(InstrumentEvent(InstrumentEvent.Type.ADD, instrument))
        service.handleInstrument(InstrumentEvent(InstrumentEvent.Type.DELETE, instrument))

        verify { instrumentRepository.saveInstrument(instrument) }
        verify { instrumentRepository.deleteInstrument(instrument) }
    }
}