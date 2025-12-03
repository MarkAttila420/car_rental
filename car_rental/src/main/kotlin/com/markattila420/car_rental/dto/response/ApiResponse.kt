package com.markattila420.car_rental.dto.response

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null
)
