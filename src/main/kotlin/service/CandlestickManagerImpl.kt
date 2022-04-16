package service

import Candlestick
import CandlestickManager
import dao.InstrumentRepository
import dao.QuoteRepository

class CandlestickManagerImpl(
    private val quoteRepository: QuoteRepository,
    private val instrumentRepository: InstrumentRepository
) : CandlestickManager {
    companion object {
        const val DEFAULT_MINUTES = 30
    }

    override fun getCandlesticks(isin: String): List<Candlestick> {
        val instrument = instrumentRepository.getInstrumentByIsin(isin)
        return instrument?.run { return quoteRepository.getCandlesticks(isin, DEFAULT_MINUTES) } ?: emptyList()
    }
}