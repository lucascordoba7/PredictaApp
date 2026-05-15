package com.lucas.predictaapp.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val name: String,
    val email: String,
    val monthIncome: Int,
    val monthSpent: Int,
    val previousMonthSpent: Int,
    val monthProjected: Int,
    val score: Int,
    val fixedMonthly: Int,
    val availableNow: Int,
    val daysToPayday: Int,
    val personality: Personality,
)

@Serializable
data class Personality(
    val name: String,
    val description: String,
    val weeklySpike: String,
    val spikeStat: String = "+318%",
    val emoji: String = "🎉",
    val accentColorKey: String = "amber",  // "amber" | "coral" | "green" | "blue" | "purple" | "teal" | "pink"
)

@Serializable
data class GroupGoal(
    val title: String,
    val current: Int,
    val target: Int,
    val deadline: String,
    val members: List<GoalMember>,
)

@Serializable
data class GoalMember(
    val initial: String,
    val color: String,
)
