package com.markattila420.car_rental.controller

import com.markattila420.car_rental.repository.BookingRepository
import com.markattila420.car_rental.repository.CarRepository
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api")
class ApiController(
    private val carRepository: CarRepository,
    private val bookingRepository: BookingRepository
) {
    
    @GetMapping("/cars/{carId}/availability")
    fun checkCarAvailability(
        @PathVariable carId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): Map<String, Boolean> {
        val car = carRepository.findById(carId).orElse(null)
        
        if (car == null || !car.active) {
            return mapOf("available" to false)
        }
        
        val overlappingBookings = bookingRepository.findOverlappingBookings(carId, startDate, endDate)
        
        return mapOf("available" to overlappingBookings.isEmpty())
    }
    
    @GetMapping("/cars/{carId}/unavailable-dates")
    fun getUnavailableDates(
        @PathVariable carId: Long
    ): Map<String, List<String>> {
        val car = carRepository.findById(carId).orElse(null)
        
        if (car == null || !car.active) {
            return mapOf("unavailableDates" to emptyList())
        }
        
        val today = LocalDate.now()
        val futureDate = today.plusYears(1)
        
        val bookings = bookingRepository.findOverlappingBookings(carId, today, futureDate)
        
        val unavailableDates = mutableSetOf<LocalDate>()
        bookings.forEach { booking ->
            var date = booking.startDate
            while (!date.isAfter(booking.endDate)) {
                unavailableDates.add(date)
                date = date.plusDays(1)
            }
        }
        
        return mapOf("unavailableDates" to unavailableDates.map { it.toString() }.sorted())
    }
}
