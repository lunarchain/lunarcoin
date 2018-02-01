package lunar.storage

import lunar.serialization.Serializer

/**
 * 对象存储类，可以接入不同的DbSource(Memory, LevelDb)和Serializer(AccountState, Transaction, Block)实现。
 */
class ObjectStore<V>(val db: DataSource<ByteArray, ByteArray>, val serializer: Serializer<V, ByteArray>) :
    DataSource<ByteArray, V> {

  override val name = db.name

  override fun delete(key: ByteArray) {
    db.delete(key)
  }

  override fun flush(): Boolean {
    return db.flush()
  }

  override fun init() {
    db.init()
  }

  override fun isAlive(): Boolean {
    return db.isAlive()
  }

  override fun close() {
    db.close()
  }

  override fun updateBatch(rows: Map<ByteArray, V?>) {
    val transformed = rows.mapValues { it.value?.let { it1 -> serializer.serialize(it1) } }
    db.updateBatch(transformed)
  }

  override fun keys(): Set<ByteArray> {
    return db.keys()
  }

  override fun reset() {
    db.reset()
  }

  override fun put(key: ByteArray, value: V) {
    db.put(key, serializer.serialize(value))
  }


  override fun get(key: ByteArray): V? {
    val bytes = db.get(key)
    if (bytes == null) {
      return null
    } else {
      return serializer.deserialize(bytes)
    }
  }

}
