package service

import InstrumentEvent
import dao.InstrumentRepository

class InstrumentService(private val instrumentRepository: InstrumentRepository) {
    fun handleInstrument(instrument: InstrumentEvent) {
        val data = instrument.data
        when (instrument.type) {
            InstrumentEvent.Type.ADD -> {
                println("Adding instrument ${data.isin}")
                instrumentRepository.saveInstrument(data)
            }
            InstrumentEvent.Type.DELETE -> {
                println("Deleting instrument ${data.isin}")
                instrumentRepository.deleteInstrument(data)
            }
        }
    }
}