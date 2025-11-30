package com.markattila420.car_rental.service

import com.markattila420.car_rental.dto.CarDto
import com.markattila420.car_rental.repository.BookingRepository
import com.markattila420.car_rental.repository.CarRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class CarService(
    private val carRepository: CarRepository,
    private val bookingRepository: BookingRepository
) {
    
    fun findAllActiveCars(): List<CarDto> {
        return carRepository.findByActiveTrue()
            .map { CarDto.fromEntity(it) }
    }
    
    fun findAvailableCars(startDate: LocalDate?, endDate: LocalDate?): List<CarDto> {
        if (startDate == null || endDate == null) {
            return findAllActiveCars()
        }
        
        val allActiveCars = carRepository.findByActiveTrue()
        
        return allActiveCars.filter { car ->
            val overlappingBookings = bookingRepository.findOverlappingBookings(
                car.id!!,
                startDate,
                endDate
            )
            overlappingBookings.isEmpty()
        }.map { CarDto.fromEntity(it) }
    }
}
