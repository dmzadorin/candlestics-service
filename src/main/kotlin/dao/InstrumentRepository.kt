package dao

import ISIN
import Instrument
import Instruments
import Quotes
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class InstrumentRepository {
    fun getInstrumentByIsin(isin: ISIN): Instrument? =
        transaction {
            Instruments
                .select { Instruments.isin eq isin }
                .map { Instruments.toInstrument(it) }
                .firstOrNull()
        }

    fun saveInstrument(instrument: Instrument) {
        transaction {
            Instruments.insertOrUpdate(Instruments.isin) {
                it[isin] = instrument.isin
                it[description] = instrument.description
            }
        }
    }

    fun deleteInstrument(instrument: Instrument) {
        transaction {
            Instruments.deleteWhere {
                Instruments.isin eq instrument.isin
            }
            Quotes.deleteWhere {
                Quotes.isin eq instrument.isin
            }
        }
    }
}