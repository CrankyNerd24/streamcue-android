package com.streamcueapp.streamcue.data

import androidx.room.TypeConverter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Room has no native list support — store these as JSON, same spirit as
 * the iOS app's SwiftData arrays (`subscriptionOn`, `genreIDs`, …). */
class Converters {
    @TypeConverter
    fun stringListToJson(value: List<String>): String = Json.encodeToString(value)

    @TypeConverter
    fun jsonToStringList(value: String): List<String> = Json.decodeFromString(value)

    @TypeConverter
    fun intListToJson(value: List<Int>): String = Json.encodeToString(value)

    @TypeConverter
    fun jsonToIntList(value: String): List<Int> = Json.decodeFromString(value)
}
