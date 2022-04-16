package util

import ISIN
import InstrumentEvent
import Price
import org.http4k.format.Jackson.auto
import org.http4k.routing.bind
import org.http4k.routing.websockets
import org.http4k.server.Netty
import org.http4k.server.asServer
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsMessage
import java.util.concurrent.CopyOnWriteArrayList

//helper classes where timestamp is not present.
//They are handy, since original Quote has timestamp, but we cannot initialize it on partners service side
data class QuoteEventWithoutTs(val data: QuoteWithoutTs)
data class QuoteWithoutTs(val isin: ISIN, val price: Price)

class PartnerServiceStub(
    partnerServicePort: Int,
    quoteConnectionCallback: () -> Unit,
    instrumentConnectionCallback: () -> Unit,
) {
    companion object {
        val quoteLens = WsMessage.auto<QuoteEventWithoutTs>().toLens()
        val instrumentLens = WsMessage.auto<InstrumentEvent>().toLens()
    }

    private val quoteWebsockets = CopyOnWriteArrayList<Websocket>()
    private val instrumentWebsockets = CopyOnWriteArrayList<Websocket>()

    private val wsServer = websockets(
        "/quotes" bind { ws: Websocket ->
            println("New quotes ws connect")
            quoteConnectionCallback.invoke()
            quoteWebsockets.add(ws)
        },
        "/instruments" bind { ws: Websocket ->
            println("New instruments ws connect")
            instrumentConnectionCallback.invoke()
            instrumentWebsockets.add(ws)
        }
    ).asServer(Netty(partnerServicePort)).start()

    fun sendInstrument(instrumentEvent: InstrumentEvent) {
        instrumentWebsockets.forEach { ws ->
            ws.send(instrumentLens.create(instrumentEvent))
        }
    }

    fun sendQuote(quote: QuoteWithoutTs) {
        quoteWebsockets.forEach { ws ->
            ws.send(quoteLens.create(QuoteEventWithoutTs(quote)))
        }
    }

    fun stop() {
        wsServer.stop()
    }
}