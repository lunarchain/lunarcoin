package io.lunarchain.lunarcoin.storage

import io.lunarchain.lunarcoin.config.BlockChainConfig
import org.slf4j.LoggerFactory
import java.sql.*


class SqliteDbHelper(val config: BlockChainConfig) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private fun getCreateTableScript(tableName: String): String {
        return "CREATE TABLE IF NOT EXISTS $tableName (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$FIELD_DATA_KEY TEXT NOT NULL UNIQUE, " +
                "$FIELD_DATA_VAL BLOB " +
                ")"
    }

    var connection: Connection? = null

    var inited = false

    companion object {
        private const val DATABASE_NAME = "lunarcoin.db"
        private const val DATABASE_VERSION = 1

        const val FIELD_DATA_KEY = "data_key"
        const val FIELD_DATA_VAL = "data_val"
    }

    fun init() {
        if (!inited) {
            var statement: Statement? = null
            try {
                // create a database connection
                val jdbcUrl =
                    "jdbc:sqlite:" + config.getDatabaseDir() + "/" + DATABASE_NAME
                connection = DriverManager.getConnection(jdbcUrl)
                statement = connection!!.createStatement()
                statement.setQueryTimeout(30)  // set timeout to 30 sec.

                statement.executeUpdate(getCreateTableScript(BUCKET_NAME_ACCOUNT_STATE))
                statement.executeUpdate(getCreateTableScript(BUCKET_NAME_ACCOUNT))
                statement.executeUpdate(getCreateTableScript(BUCKET_NAME_BLOCK))
                statement.executeUpdate(getCreateTableScript(BUCKET_NAME_BLOCK_INDEX))
                statement.executeUpdate(getCreateTableScript(BUCKET_NAME_BLOCK_TRANSACTION))
                statement.executeUpdate(getCreateTableScript(BUCKET_NAME_BEST_BLOCK))

                inited = true
            } catch (e: SQLException) {
                logger.error(e.message)
            } finally {
                statement?.close()
            }

        }
    }

    fun close() {
        if (inited) {
            connection?.close()
            inited = false
        }
    }

    fun getValue(tableName: String, key: ByteArray): ByteArray? {
        if (connection != null) {
            var rs: ResultSet? = null
            try {
                val statement =
                    connection!!.prepareStatement("SELECT " + FIELD_DATA_VAL + " FROM " + tableName + " WHERE " + FIELD_DATA_KEY + "=?")
                statement.setBytes(1, key)
                rs = statement.executeQuery()

                if (rs.next()) {
                    return rs.getBytes(FIELD_DATA_VAL)
                }
            } catch (e: Exception) {
                logger.error(e.message)
            } finally {
                rs?.close()
            }
        }

        return null
    }

    fun putValue(tableName: String, key: ByteArray, value: ByteArray) {
        if (connection != null) {
            val statement =
                connection!!.prepareStatement("INSERT OR REPLACE INTO " + tableName + " (" + FIELD_DATA_KEY + ","+FIELD_DATA_VAL+") VALUES (?,?)")
            statement.setBytes(1, key)
            statement.setBytes(2, value)
            statement.executeUpdate()
        }
    }

    fun deleteValue(tableName: String, key: ByteArray) {
        if (connection != null) {
            val statement =
                connection!!.prepareStatement("DELETE FROM " + tableName + " WHERE " + FIELD_DATA_KEY + "=?")
            statement.setBytes(1, key)
            statement.executeUpdate()
        }
    }

    fun listKeys(tableName: String): Set<ByteArray> {
        val result: MutableSet<ByteArray> = hashSetOf()

        if (connection != null) {
            var rs: ResultSet? = null
            try {
                val statement =
                    connection!!.prepareStatement("SELECT " + FIELD_DATA_VAL + " FROM " + tableName)
                rs = statement.executeQuery()

                while (rs.next()) {
                    result.add(rs.getBytes(FIELD_DATA_VAL))
                }
            } catch (e: Exception) {
                logger.error(e.message)
            } finally {
                rs?.close()
            }
        }
        return result
    }

    fun start() {
        connection?.autoCommit = false
    }

    fun commit() {
        connection?.commit()
    }

    fun rollback() {
        connection?.rollback()
    }
}