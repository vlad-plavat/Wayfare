package com.plavatvlad.wayfare.data

data class UserProfile(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val category: String = "regular",
    val createdAt: String = ""
)