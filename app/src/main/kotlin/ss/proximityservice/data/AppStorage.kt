package ss.proximityservice.data

interface AppStorage {
    fun getInt(key: String, defValue: Int): Int
    fun getBoolean(key: String, defValue: Boolean): Boolean
    fun <T> put(key: String, value: T)
}
