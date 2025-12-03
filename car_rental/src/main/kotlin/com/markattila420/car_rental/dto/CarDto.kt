package com.markattila420.car_rental.dto

import com.markattila420.car_rental.entity.Car
import java.math.BigDecimal

data class CarDto(
    val id: Long,
    val brand: String,
    val model: String,
    val dailyRate: BigDecimal,
    val imagePath: String?
) {
    companion object {
        fun fromEntity(car: Car): CarDto {
            return CarDto(
                id = car.id!!,
                brand = car.brand,
                model = car.model,
                dailyRate = car.dailyRate,
                imagePath = car.imagePath
            )
        }
    }
}
