import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dao.DbManager
import dao.InstrumentRepository
import dao.QuoteRepository
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import service.CandlestickManagerImpl
import service.InstrumentService

fun main(args: Array<String>) {
    println("starting up")

    val parser = ArgParser("candlesticks")
    val port by parser.option(ArgType.Int, shortName = "p", description = "port").default(9000)
    val partnerServiceUrl by parser.option(
        ArgType.String,
        shortName = "partnerUrl",
        description = "partner service url"
    )
        .default("ws://localhost:8032")
    val dbUrl by parser.option(ArgType.String, shortName = "dbUrl", description = "database connection string")
        .default("jdbc:postgresql://localhost:5432/app")
    val dbUser by parser.option(ArgType.String, shortName = "dbUser", description = "database username")
        .default("postgres")
    val dbPassword by parser.option(ArgType.String, shortName = "dbPass", description = "database password")
        .default("password")

    parser.parse(args)

    val maskedPass = "*".repeat(dbPassword.length)
    println("Using following params: port=$port, partnerUrl=$partnerServiceUrl, dbUrl=$dbUrl, user=$dbUser, pass=$maskedPass")

    CandlestickApp(port, partnerServiceUrl, dbUrl, dbUser, dbPassword)
}

class CandlestickApp(
    serverPort: Int,
    partnerServiceUrl: String,
    dbUrl: String,
    dbUser: String,
    dbPassword: String
) {
    private val server: Server

    init {
        DbManager(dbUrl, dbUser, dbPassword).initConnect

        val instrumentRepository = InstrumentRepository()
        val instrumentService = InstrumentService(instrumentRepository)
        val quoteRepository = QuoteRepository()
        val server = Server(CandlestickManagerImpl(quoteRepository, instrumentRepository), serverPort)

        val instrumentsStreamUrl = "$partnerServiceUrl/instruments"
        val instrumentStream = InstrumentStream(instrumentsStreamUrl)
        instrumentStream.connect { event ->
            println(event)
            instrumentService.handleInstrument(event)
        }

        val quotesStreamUrl = "$partnerServiceUrl/quotes"
        val quoteStream = QuoteStream(quotesStreamUrl)
        quoteStream.connect { event ->
            println(event)
            quoteRepository.saveQuote(event.data)
        }

        server.start()
        this.server = server
    }

    fun stop() {
        server.stop()
    }
}

val jackson: ObjectMapper =
    jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
