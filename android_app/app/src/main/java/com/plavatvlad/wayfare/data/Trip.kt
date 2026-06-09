package com.plavatvlad.wayfare.data

data class Trip(
    var id: String = "",
    var name: String = "",
    var placeIds: List<String> = emptyList(),
    var createdBy: String = "",
    var createdAt: Long = 0
)