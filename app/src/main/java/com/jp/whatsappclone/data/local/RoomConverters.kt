package com.jp.whatsappclone.data.local

import androidx.room.TypeConverter

class RoomConverters {
    @TypeConverter
    fun memberIdsToStorage(value: List<String>): String = value.joinToString(MEMBER_SEPARATOR)

    @TypeConverter
    fun memberIdsFromStorage(value: String): List<String> =
        if (value.isBlank()) emptyList() else value.split(MEMBER_SEPARATOR)

    @TypeConverter
    fun outboxOperationToStorage(value: OutboxOperation): String = value.name

    @TypeConverter
    fun outboxOperationFromStorage(value: String): OutboxOperation = OutboxOperation.valueOf(value)

    @TypeConverter
    fun outboxStateToStorage(value: OutboxState): String = value.name

    @TypeConverter
    fun outboxStateFromStorage(value: String): OutboxState = OutboxState.valueOf(value)

    private companion object {
        const val MEMBER_SEPARATOR = "\u001F"
    }
}
