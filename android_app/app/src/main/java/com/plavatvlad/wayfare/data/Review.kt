package com.plavatvlad.wayfare.data

data class Review(
    val userId: String = "",
    val userName: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val createdAt: String = ""
)