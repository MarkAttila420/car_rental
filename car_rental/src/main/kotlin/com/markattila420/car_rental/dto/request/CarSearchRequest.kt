package com.markattila420.car_rental.dto.request

import java.time.LocalDate

data class CarSearchRequest(
    val startDate: LocalDate?,
    val endDate: LocalDate?
)
