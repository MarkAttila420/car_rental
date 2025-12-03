package com.markattila420.car_rental.service

import com.markattila420.car_rental.entity.Booking
import com.markattila420.car_rental.entity.BookingStatus
import com.markattila420.car_rental.repository.BookingRepository
import com.markattila420.car_rental.repository.CarRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
class BookingService(
    private val bookingRepository: BookingRepository,
    private val carRepository: CarRepository
) {
    
    @Transactional
    fun createBooking(
        carId: Long,
        customerName: String,
        customerEmail: String,
        customerAddress: String,
        customerPhone: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Booking {
        val car = carRepository.findById(carId)
            .orElseThrow { IllegalArgumentException("Car not found with id: $carId") }
        
        if (!car.active) {
            throw IllegalStateException("Car is not available for booking")
        }
        
        val overlappingBookings = bookingRepository.findOverlappingBookings(carId, startDate, endDate)
        if (overlappingBookings.isNotEmpty()) {
            throw IllegalStateException("Car is already booked for the selected dates")
        }
        
        val numberOfDays = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
        val totalAmount = car.dailyRate.multiply(BigDecimal(numberOfDays))
        
        val booking = Booking(
            car = car,
            customerName = customerName,
            customerEmail = customerEmail,
            customerAddress = customerAddress,
            customerPhone = customerPhone,
            startDate = startDate,
            endDate = endDate,
            numberOfDays = numberOfDays,
            totalAmount = totalAmount,
            bookingStatus = BookingStatus.PENDING,
            createdAt = LocalDateTime.now()
        )
        
        return bookingRepository.save(booking)
    }
}
