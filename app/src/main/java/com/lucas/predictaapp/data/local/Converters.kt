package com.lucas.predictaapp.data.local

import androidx.room.TypeConverter
import com.lucas.predictaapp.data.model.DateGroup
import com.lucas.predictaapp.data.model.ExpenseSource
import com.lucas.predictaapp.data.model.NotificationAction
import com.lucas.predictaapp.data.model.NotificationType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

class Converters {
    @TypeConverter fun expenseSourceToString(v: ExpenseSource): String = v.name
    @TypeConverter fun stringToExpenseSource(v: String): ExpenseSource = ExpenseSource.valueOf(v)

    @TypeConverter fun notifTypeToString(v: NotificationType): String = v.name
    @TypeConverter fun stringToNotifType(v: String): NotificationType = NotificationType.valueOf(v)

    @TypeConverter fun dateGroupToString(v: DateGroup): String = v.name
    @TypeConverter fun stringToDateGroup(v: String): DateGroup = DateGroup.valueOf(v)

    @TypeConverter fun actionsToString(v: List<NotificationAction>): String = json.encodeToString(v)
    @TypeConverter fun stringToActions(v: String): List<NotificationAction> = json.decodeFromString(v)
}
