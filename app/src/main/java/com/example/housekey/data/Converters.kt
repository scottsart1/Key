package com.example.housekey.data

import androidx.room.TypeConverter

/** Room converters for enum columns. */
class Converters {
    @TypeConverter
    fun fromKeyType(type: KeyType): String = type.name

    @TypeConverter
    fun toKeyType(value: String): KeyType =
        runCatching { KeyType.valueOf(value) }.getOrDefault(KeyType.NDEF_TEXT)
}
