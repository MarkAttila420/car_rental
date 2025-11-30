package com.markattila420.car_rental.repository

import com.markattila420.car_rental.entity.Car
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CarRepository : JpaRepository<Car, Long> {
    fun findByActiveTrue(): List<Car>
}
