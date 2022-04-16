import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

data class InstrumentEvent(val type: Type, val data: Instrument) {
    enum class Type {
        ADD,
        DELETE
    }
}

data class QuoteEvent(val data: Quote)

data class Instrument(val isin: ISIN, val description: String)
typealias ISIN = String

data class Quote(val isin: ISIN, val price: Price, val ts: Instant = Instant.now())
typealias Price = Double


interface CandlestickManager {
    fun getCandlesticks(isin: String): List<Candlestick>
}

data class Candlestick(
    val openTimestamp: Instant,
    var closeTimestamp: Instant,
    val openPrice: Price,
    var highPrice: Price,
    var lowPrice: Price,
    var closingPrice: Price
)

object Instruments : Table("instruments") {
    val isin = varchar("isin", 30).uniqueIndex()
    val description = varchar("description", 100)

    fun toInstrument(rs: ResultRow): Instrument {
        return Instrument(rs[isin], rs[description])
    }
}

object Quotes : IntIdTable("quotes") {
    val isin = varchar("isin", 30)
    val price = decimal("price", 10, 4)
    val ts = timestamp("ts")

    fun toQuote(row: ResultRow): Quote =
        Quote(
            isin = row[isin],
            price = row[price].toDouble(),
        )
}