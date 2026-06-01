package com.plavatvlad.wayfare.data

data class Place(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val createdBy: String = "",
    val isPublic: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)