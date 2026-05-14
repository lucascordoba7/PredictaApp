package com.lucas.predictaapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "notifications")
data class Notification(
    @PrimaryKey
    val id: String,
    val type: NotificationType,
    val icon: String,
    val typeLabel: String,
    val title: String,
    val body: String,
    val time: String,
    val dateGroup: DateGroup,
    val unread: Boolean = false,
    val actions: List<NotificationAction> = emptyList(),
)

@Serializable
enum class NotificationType {
    WARNING, ANOMALY, PATTERN, NEW_SUB, ANT, SOCIAL, OPPORTUNITY, CLOSE, FORGOT, ZOMBIE
}

@Serializable
enum class DateGroup {
    TODAY, YESTERDAY, OLDER
}

@Serializable
data class NotificationAction(
    val label: String,
    val primary: Boolean = false,
    val danger: Boolean = false,
)
