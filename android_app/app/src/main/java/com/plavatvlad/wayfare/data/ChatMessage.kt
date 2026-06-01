package com.plavatvlad.wayfare.data

data class ChatMessage(
    val role: String = "",   // "user" | "assistant"
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)