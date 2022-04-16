package dao

import Candlestick
import service.CandlestickManagerImpl.Companion.DEFAULT_MINUTES
import Quote
import Quotes
import util.buildCandleStick
import util.dateTimeWithZeroSeconds
import util.generateQuotes
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils
import java.math.BigDecimal
import java.time.Instant
import java.time.ZonedDateTime
import kotlin.random.Random

internal class QuoteRepositoryTest : TestContainersBase() {

    private lateinit var isin: String
    private lateinit var quoteRepository: QuoteRepository

    @BeforeEach
    fun setup() {
        quoteRepository = QuoteRepository()
        isin = RandomStringUtils.randomAlphabetic(10)
    }

    @Test
    fun saveQuote() {
        val price = 10.0
        val ts = Instant.now()
        quoteRepository.saveQuote(Quote(isin, price, ts))
        val savedQuote = transaction {
            Quotes.select {
                Quotes.isin eq isin
            }.firstOrNull() ?: fail("No quotes for isin $isin")
        }

        assertEquals(savedQuote[Quotes.isin], isin)
        assertEquals(savedQuote[Quotes.price].toDouble(), price)
        assertEquals(savedQuote[Quotes.ts], ts)
    }

    @Test
    fun testCandlesticksHappyPath() {
        val currentTs = dateTimeWithZeroSeconds()
        //every 10 sec, last (6th) quote will fall down into next candlestick
        val quotes = generateQuotes(
            isin = isin, startTs = currentTs, size = 6
        )
        insertQuotes(quotes)

        val firstCandleTs = currentTs
        val secondCandleTs = firstCandleTs.plusMinutes(1)

        val firstCandleQuotes = quotes.filter { it.ts < secondCandleTs.toInstant() }
        assertEquals(
            listOf(
                buildCandleStick(firstCandleTs, firstCandleQuotes),
                //this candlestick has only one quote, thus all prices are set to the value of that quote
                Candlestick(
                    secondCandleTs.toInstant(),
                    secondCandleTs.plusMinutes(1).toInstant(),
                    quotes.last().price,
                    quotes.last().price,
                    quotes.last().price,
                    quotes.last().price
                )
            ),
            quoteRepository.getCandlesticks(isin, DEFAULT_MINUTES)
        )
    }

    @Test
    fun testRealTimeAggregation() {
        val currentTs = dateTimeWithZeroSeconds()
        val minPrice = 10.0
        val maxPrice = 30.0
        val quotes = generateQuotes(
            isin = isin, startTs = currentTs, size = 5, tsDelaySeconds = 5L, maxPrice = maxPrice, minPrice = minPrice
        )
        insertQuotes(quotes)

        assertEquals(
            listOf(buildCandleStick(currentTs, quotes)),
            quoteRepository.getCandlesticks(isin, DEFAULT_MINUTES)
        )
        val highPrice = maxPrice + 1
        val lowPrice = minPrice - 1
        //insert two quotes with not intersecting timestamps
        val highPriceQuote = Quote(isin, highPrice, currentTs.plusSeconds(17).toInstant())
        val lowPriceQuote = Quote(isin, lowPrice, currentTs.plusSeconds(22).toInstant())
        insertQuotes(listOf(highPriceQuote, lowPriceQuote))

        //Now after inserting new quotes, when we query for candle once more,
        //our high & low price would be changed
        //That's achieved with the help of real time aggregates in timescale
        assertEquals(
            listOf(
                Candlestick(
                    openTimestamp = currentTs.toInstant(),
                    closeTimestamp = currentTs.plusMinutes(1).toInstant(),
                    openPrice = quotes.first().price,
                    highPrice = highPrice,
                    lowPrice = lowPrice,
                    closingPrice = quotes.last().price
                )
            ),
            quoteRepository.getCandlesticks(isin, DEFAULT_MINUTES)
        )
    }

    @Test
    fun oldCandleSticksAreSkipped() {
        val current = dateTimeWithZeroSeconds()
        val thirtyOne = (DEFAULT_MINUTES + 1).toLong()
        val minus31Min = dateTimeWithZeroSeconds().minusMinutes(thirtyOne)
        //prepare two candles - first one has open ts 31min ago (in order to test fetching last 30min candles)
        //second one with current ts
        val firstCandle = generateQuotes(isin = isin, startTs = minus31Min, size = 3)
        val secondCandle = generateQuotes(isin = isin, startTs = current, size = 3)
        insertQuotes(firstCandle + secondCandle)

        //first test, that passing 60-min shift covers both candlesticks
        assertEquals(
            listOf(
                buildCandleStick(minus31Min, firstCandle),
                buildCandleStick(current, secondCandle)
            ),
            quoteRepository.getCandlesticks(isin, 60)
        )
        //and then test, that first candlestick does not fall into 30 min shift
        assertEquals(
            listOf(buildCandleStick(current, secondCandle)),
            quoteRepository.getCandlesticks(isin, DEFAULT_MINUTES)
        )
    }


    private fun insertQuotes(quotes: List<Quote>) {
        transaction {
            Quotes.batchInsert(quotes) { quote ->
                this[Quotes.isin] = quote.isin
                this[Quotes.price] = BigDecimal.valueOf(quote.price)
                this[Quotes.ts] = quote.ts
            }
        }
    }
}