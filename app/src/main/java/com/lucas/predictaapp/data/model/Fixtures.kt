package com.lucas.predictaapp.data.model

object Fixtures {
    val user = UserProfile(
        name = "Vale",
        monthIncome = 900000,
        monthSpent = 180000,
        previousMonthSpent = 196000,
        monthProjected = 460000,
        score = 74,
        fixedMonthly = 100000,
        availableNow = 720000,
        daysToPayday = 2,
        personality = Personality(
            name = "La Anfitriona de Viernes",
            description = "Tus gastos cuentan una historia: amás juntar gente y los viernes son imparables.",
            weeklySpike = "viernes",
        ),
    )

    val expenses = listOf(
        Expense(1, "Don Julio", "Salidas", 38500, "ayer 22:48", ExpenseSource.WHATSAPP_IMAGE),
        Expense(2, "Uber", "Transporte", 8400, "ayer 23:46", ExpenseSource.WHATSAPP_IMAGE),
        Expense(3, "Farmacity", "Salud", 4870, "hace 2d", ExpenseSource.WHATSAPP_TEXT),
        Expense(4, "Rappi", "Delivery", 6200, "hace 2d", ExpenseSource.WHATSAPP_TEXT),
        Expense(5, "Coto", "Comida en casa", 12450, "hace 3d", ExpenseSource.WHATSAPP_IMAGE),
    )

    val subscriptions = listOf(
        Subscription("spotify", "Spotify Familiar", "S", 92, 3200, zombie = false),
        Subscription("netflix", "Netflix", "N", 68, 4500, zombie = false),
        Subscription("hbo", "HBO Max", "H", 4, 2800, zombie = true),
        Subscription("icloud", "iCloud 200GB", "i", 100, 1200, zombie = false),
        Subscription("adobe", "Adobe CC fotografía", "A", 0, 1800, zombie = true),
        Subscription("patreon", "Patreon (podcast)", "P", 12, 4300, zombie = true),
    )

    val notifications = listOf(
        Notification(
            id = "n1", type = NotificationType.WARNING, icon = "⚡", typeLabel = "Aviso anticipado",
            title = "Vas al límite de Comida", body = "Llevás $64.000 de $80.000 y faltan 12 días.",
            time = "14:02", dateGroup = DateGroup.TODAY, unread = true,
            actions = listOf(NotificationAction("Ajustar plan"), NotificationAction("Ver detalle")),
        ),
        Notification(
            id = "n2", type = NotificationType.PATTERN, icon = "🔄", typeLabel = "Patrón recurrente",
            title = "Hoy es viernes — usualmente gastás $6.200", body = "Los viernes a la noche 11 de cada 12 hay salida o delivery.",
            time = "11:30", dateGroup = DateGroup.TODAY, unread = true,
            actions = listOf(NotificationAction("Reservar $6.200", primary = true), NotificationAction("Hoy no")),
        ),
        Notification(
            id = "n3", type = NotificationType.ANOMALY, icon = "👁", typeLabel = "Anomalía detectada",
            title = "$18.400 en Mercado Pago — sin descripción", body = "Es 3,2× tu promedio en pagos QR.",
            time = "09:48", dateGroup = DateGroup.TODAY, unread = true,
            actions = listOf(NotificationAction("Fui yo"), NotificationAction("No fui yo", danger = true)),
        ),
        Notification(
            id = "n4", type = NotificationType.FORGOT, icon = "📋", typeLabel = "Te olvidaste de cargar",
            title = "¿Cargo el café de la mañana?", body = "Detecté que pasaste por Starbucks Palermo a las 08:15.",
            time = "22:14", dateGroup = DateGroup.YESTERDAY, unread = false,
            actions = listOf(NotificationAction("Sí, cargar", primary = true), NotificationAction("Ignorar")),
        ),
        Notification(
            id = "n5", type = NotificationType.NEW_SUB, icon = "💳", typeLabel = "Nueva suscripción",
            title = "Primer cargo de Apple One: $2.990/mes", body = "Se debitó hoy. Sumás $2.990/mes a tus fijos.",
            time = "18:00", dateGroup = DateGroup.YESTERDAY, unread = false,
            actions = listOf(NotificationAction("Agregar", primary = true), NotificationAction("Ignorar")),
        ),
        Notification(
            id = "n6", type = NotificationType.ANT, icon = "🐜", typeLabel = "Gasto hormiga",
            title = "Llevás $14.800 en cafés este mes", body = "9 compras de menos de $2.000.",
            time = "10:00", dateGroup = DateGroup.OLDER, unread = false,
            actions = listOf(NotificationAction("Ver detalle")),
        ),
        Notification(
            id = "n7", type = NotificationType.SOCIAL, icon = "🤝", typeLabel = "Deuda pendiente",
            title = "Lucas te debe $4.500 hace 9 días", body = "La última vez que le prestaste fue el 29 de abril.",
            time = "09:00", dateGroup = DateGroup.OLDER, unread = false,
            actions = listOf(NotificationAction("Recordar a Lucas", primary = true), NotificationAction("Ya me pagó")),
        ),
        Notification(
            id = "n8", type = NotificationType.OPPORTUNITY, icon = "🎯", typeLabel = "Oportunidad",
            title = "Bajó 25% el curso UX que mirabas", body = "Estaba en $60.000, ahora $45.000.",
            time = "08:30", dateGroup = DateGroup.OLDER, unread = false,
            actions = listOf(NotificationAction("Ver curso", primary = true), NotificationAction("Después")),
        ),
    )

    val groupGoal = GroupGoal(
        title = "Viaje a Chile",
        current = 117500,
        target = 250000,
        deadline = "2026-12-15",
        members = listOf(GoalMember("V", "amber"), GoalMember("L", "green")),
    )
}
