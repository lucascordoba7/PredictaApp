package com.lucas.predictaapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey
    val id: String,
    val service: String,
    val initial: String,
    val usagePct: Int,
    val monthly: Int,
    val zombie: Boolean = false,
)
