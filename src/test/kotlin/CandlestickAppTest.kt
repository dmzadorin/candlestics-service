import com.fasterxml.jackson.module.kotlin.readValue
import util.PartnerServiceStub
import util.QuoteWithoutTs
import util.randomDoubleRounded
import dao.TestContainersBase
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.awaitility.Awaitility
import org.http4k.client.JavaHttpClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils
import java.net.ServerSocket
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

internal class CandlestickAppTest : TestContainersBase() {

    companion object {
        val appPort = findFreePort()
        //setting up a countdown latch in order to wait till app is connected to partner service
        private val appConnected = CountDownLatch(2)
        private val partnerServicePort = findFreePort()
        private val partnerService = PartnerServiceStub(
            partnerServicePort = partnerServicePort,
            quoteConnectionCallback = { appConnected.countDown() },
            instrumentConnectionCallback = { appConnected.countDown() }
        )
        private lateinit var app: CandlestickApp

        @BeforeAll
        @JvmStatic
        fun setup() {
            println("Using $appPort for app requests")
            println("Using $partnerServicePort for partner service")
            app = CandlestickApp(
                serverPort = appPort,
                partnerServiceUrl = "ws://localhost:$partnerServicePort",
                dbUrl = timescaleContainer.jdbcUrl,
                dbUser = timescaleContainer.username,
                dbPassword = timescaleContainer.password,
            )
            val connected = appConnected.await(1, TimeUnit.SECONDS)
            connected shouldBe true
        }

        @AfterAll
        @JvmStatic
        fun cleanup() {
            app.stop()
            partnerService.stop()
        }

        private fun findFreePort() = ServerSocket(0).use { it.localPort }
    }

    @Test
    fun testE2eInteraction() {
        val isin = RandomStringUtils.randomAlphabetic(5)
        val description = RandomStringUtils.randomAlphabetic(10)
        val instrumentEvent = InstrumentEvent(InstrumentEvent.Type.ADD, Instrument(isin, description))

        val quotes = (1..5).map { _ ->
            QuoteWithoutTs(isin = isin, price = randomDoubleRounded(10.0, 20.0))
        }

        runBlocking {
            partnerService.sendInstrument(instrumentEvent)
            quotes.forEach {
                partnerService.sendQuote(it)
                delay(1.seconds)
            }
        }
        //Waiting for instruments/quotes to appear in DB
        Awaitility.await()
            .atMost(Duration.ofSeconds(3))
            .untilAsserted {
                val hasInstrument = hasInstrument(isin)
                val actualQuotes = fetchQuotesByIsin(isin)
                assertTrue(hasInstrument)
                assertEquals(actualQuotes.size, quotes.size)
            }

        val request = Request(Method.GET, "http://localhost:$appPort/candlesticks").query("isin", isin)
        val response = JavaHttpClient()(request)
        val actualCandlesticks: List<Candlestick> = jackson.readValue(response.bodyString())

        val expectedQuotes = fetchQuotesByIsin(isin)
        val expectedCandleSticks = expectedQuotes.groupBy {
            //truncate each quote's timestamp to minute and group by minute buckets
            it.ts.truncatedTo(ChronoUnit.MINUTES)
        }.map { entry ->
            val groupedQuotes = entry.value
            Candlestick(
                openTimestamp = entry.key,
                closeTimestamp = entry.key.plusSeconds(60),
                openPrice = groupedQuotes.first().price,
                highPrice = groupedQuotes.maxOf { it.price },
                lowPrice = groupedQuotes.minOf { it.price },
                closingPrice = groupedQuotes.last().price
            )
        }
        assertEquals(expectedCandleSticks, actualCandlesticks)
    }

    private fun fetchQuotesByIsin(isin: ISIN): List<Quote> {
        return transaction {
            Quotes.select {
                Quotes.isin eq isin
            }.toList().map {
                Quote(it[Quotes.isin], it[Quotes.price].toDouble(), it[Quotes.ts])
            }
        }
    }

    private fun hasInstrument(isin: ISIN): Boolean {
        return transaction {
            Instruments.select {
                Instruments.isin eq isin
            }.toList().isNotEmpty()
        }
    }
}