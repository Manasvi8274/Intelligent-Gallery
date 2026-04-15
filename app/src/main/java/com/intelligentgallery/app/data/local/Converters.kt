package com.intelligentgallery.app.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromLongList(value: List<Long>): String = value.joinToString(",")

    @TypeConverter
    fun toLongList(value: String): List<Long> =
        value.takeIf { it.isNotBlank() }
            ?.split(",")
            ?.mapNotNull { it.toLongOrNull() }
            ?: emptyList()

    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString("|")

    @TypeConverter
    fun toStringList(value: String): List<String> =
        value.takeIf { it.isNotBlank() }?.split("|") ?: emptyList()

    @TypeConverter
    fun fromFloatList(value: List<Float>): String = value.joinToString(",")

    @TypeConverter
    fun toFloatList(value: String): List<Float> =
        value.takeIf { it.isNotBlank() }
            ?.split(",")
            ?.mapNotNull { it.toFloatOrNull() }
            ?: emptyList()
}
