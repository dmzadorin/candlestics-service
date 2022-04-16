package dao

import Candlestick
import ISIN
import Quote
import Quotes
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.sql.ResultSet

class QuoteRepository {
    fun saveQuote(quote: Quote) {
        transaction {
            Quotes.insert {
                it[isin] = quote.isin
                it[price] = BigDecimal.valueOf(quote.price)
                it[ts] = quote.ts
            }
        }
    }

    fun getCandlesticks(isin: ISIN, minutes: Int): List<Candlestick> {
        return transaction {
            val query = """
                SELECT * FROM one_min_candle
                WHERE isin = '$isin' AND bucket >= NOW() - INTERVAL '$minutes min'
                ORDER BY bucket
            """.trimIndent()
            TransactionManager.current().exec(query) { rs ->
                val candlesticks = mutableListOf<Candlestick>()
                while (rs.next()) {
                    candlesticks += buildCandlestick(rs)
                }
                candlesticks
            } ?: emptyList()
        }
    }

    private fun buildCandlestick(rs: ResultSet): Candlestick {
        val openPrice = rs.getBigDecimal("open")
        val closePrice = rs.getBigDecimal("close")
        val highPrice = rs.getBigDecimal("high")
        val lowPrice = rs.getBigDecimal("low")
        val openTime = rs.getTimestamp("bucket").toInstant()
        val closeTime = openTime.plusSeconds(60)

        return Candlestick(
            openTimestamp = openTime,
            closeTimestamp = closeTime,
            openPrice = openPrice.toDouble(),
            closingPrice = closePrice.toDouble(),
            highPrice = highPrice.toDouble(),
            lowPrice = lowPrice.toDouble()
        )
    }
}
