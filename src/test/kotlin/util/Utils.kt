package util

import Candlestick
import ISIN
import Quote
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.ZonedDateTime
import java.util.Locale
import kotlin.random.Random

fun generateQuotes(
    isin: ISIN,
    startTs: ZonedDateTime,
    size: Int,
    tsDelaySeconds: Long = 10L,
    minPrice: Double = 10.0,
    maxPrice: Double = 20.0
): List<Quote> =
    (1..size).map { idx ->
        val nextTs = startTs.plusSeconds(idx * tsDelaySeconds).toInstant()
        Quote(isin, randomDoubleRounded(minPrice, maxPrice), nextTs)
    }

fun randomDoubleRounded(minPrice: Double, maxPrice: Double): Double {
    val number = Random.nextDouble(minPrice, maxPrice)
    val df = DecimalFormat("#.####", DecimalFormatSymbols(Locale.ENGLISH))
    df.roundingMode = RoundingMode.FLOOR
    return df.format(number).toDouble()
}

fun dateTimeWithZeroSeconds() = ZonedDateTime.now().withSecond(0).withNano(0)

/**
 * Builds a candlestick based on quotes.
 * This function expects, that all quotes fall into one minute candlestick,
 * it doesn't check timestamp boundaries
 */
fun buildCandleStick(startTs: ZonedDateTime, quotes: List<Quote>) =
    Candlestick(
        openTimestamp = startTs.toInstant(),
        closeTimestamp = startTs.plusMinutes(1).toInstant(),
        openPrice = quotes.first().price,
        highPrice = quotes.maxOf { it.price },
        lowPrice = quotes.minOf { it.price },
        closingPrice = quotes.last().price
    )