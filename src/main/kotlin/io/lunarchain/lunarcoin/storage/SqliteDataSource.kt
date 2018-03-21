package io.lunarchain.lunarcoin.storage

class SqliteDataSource(override val name: String, val dbHelper: SqliteDbHelper): DataSource<ByteArray, ByteArray> {

    override fun get(key: ByteArray): ByteArray? {
        return dbHelper.getValue(name, key)
    }

    override fun put(key: ByteArray, value: ByteArray) {
        dbHelper.putValue(name, key, value)
    }

    override fun delete(key: ByteArray) {
        dbHelper.deleteValue(name, key)
    }

    override fun flush(): Boolean {
        //DO NOTHING
        return true
    }

    override fun init() {
        dbHelper.init()
    }

    override fun isAlive(): Boolean {
        return true
    }

    override fun close() {
        dbHelper.close()
    }

    override fun updateBatch(rows: Map<ByteArray, ByteArray?>) {
        for (row in rows) {
            if (row.value!=null){ put(row.key, row.value!!)}
        }
    }

    override fun keys(): Set<ByteArray> {
        return dbHelper.listKeys(name)
    }

    override fun reset() {
        //DO NOTHING
    }

    override fun start() {
        dbHelper.start()
    }

    override fun commit() {
        dbHelper.commit()
    }

    override fun rollback() {
        dbHelper.rollback()
    }
}