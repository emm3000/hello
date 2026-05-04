package com.emm.data.catalog

data class CommunicativeIntent(
    val id: String,
    val label: String,
    val description: String,
)

val communicativeIntents: List<CommunicativeIntent> = listOf(
    CommunicativeIntent(
        id = "introduce_yourself",
        label = "Presentarte",
        description = "Hablar de ti, decir tu nombre, origen u ocupacion."
    ),
    CommunicativeIntent(
        id = "talk_about_plans",
        label = "Hablar de planes",
        description = "Expresar planes, intenciones o actividades futuras."
    ),
    CommunicativeIntent(
        id = "describe_past_events",
        label = "Contar experiencias pasadas",
        description = "Narrar algo que paso ayer, la semana pasada o en otro momento."
    ),
    CommunicativeIntent(
        id = "make_polite_requests",
        label = "Hacer pedidos educados",
        description = "Pedir ayuda, favores, objetos o acciones de forma natural."
    ),
    CommunicativeIntent(
        id = "order_food",
        label = "Pedir comida",
        description = "Ordenar en restaurantes, cafes o delivery."
    ),
    CommunicativeIntent(
        id = "ask_for_directions",
        label = "Pedir direcciones",
        description = "Preguntar ubicaciones, rutas o como llegar a un lugar."
    ),
    CommunicativeIntent(
        id = "solve_daily_problems",
        label = "Resolver problemas cotidianos",
        description = "Expresar fallas, dificultades o necesidades practicas."
    ),
    CommunicativeIntent(
        id = "express_preferences",
        label = "Expresar gustos",
        description = "Decir lo que te gusta, prefieres o no soportas."
    ),
    CommunicativeIntent(
        id = "express_emotions",
        label = "Expresar emociones",
        description = "Hablar de alegria, frustracion, sorpresa, cansancio o alivio."
    ),
    CommunicativeIntent(
        id = "social_small_talk",
        label = "Conversacion social",
        description = "Mantener una charla casual con amigos, colegas o desconocidos."
    ),
    CommunicativeIntent(
        id = "handle_complaints",
        label = "Hacer o responder quejas",
        description = "Expresar molestias y responder con naturalidad."
    ),
    CommunicativeIntent(
        id = "phone_and_messages",
        label = "Telefono y mensajes",
        description = "Hablar por llamada, dejar mensajes o escribir textos comunes."
    ),
)
