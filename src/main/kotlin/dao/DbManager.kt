package dao

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction

class DbManager(
    private val dbUrl: String,
    private val user: String,
    private val password: String
) {
    val initConnect by lazy {
        Database.connect(
            dbUrl,
            driver = "org.postgresql.Driver",
            user = user,
            password = password
        )
        transaction {
            addLogger(StdOutSqlLogger)
        }
    }
}