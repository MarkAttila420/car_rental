package com.markattila420.car_rental.controller

import com.markattila420.car_rental.dto.*
import com.markattila420.car_rental.service.BookingService
import com.markattila420.car_rental.service.CarService
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1")
@Validated
class CarRentalApiController(
    private val carService: CarService,
    private val bookingService: BookingService
) {
    
    @GetMapping("/cars/available")
    fun getAvailableCars(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?
    ): ResponseEntity<ApiResponse<List<CarDto>>> {
        val carDtos = carService.findAvailableCars(startDate, endDate)
        
        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                data = carDtos,
                message = "Available cars retrieved successfully"
            )
        )
    }
    
    @PostMapping("/bookings")
    fun createBooking(
        @Valid @RequestBody request: CreateBookingRequest
    ): ResponseEntity<ApiResponse<BookingResponse>> {
        // Validate date range
        if (request.endDate.isBefore(request.startDate)) {
            throw IllegalArgumentException("End date must be after or equal to start date")
        }
        
        if (request.startDate.isBefore(LocalDate.now())) {
            throw IllegalArgumentException("Start date cannot be in the past")
        }
        
        val booking = bookingService.createBooking(
            carId = request.carId,
            customerName = request.customerName,
            customerEmail = request.customerEmail,
            customerAddress = request.customerAddress,
            customerPhone = request.customerPhone,
            startDate = request.startDate,
            endDate = request.endDate
        )
        
        val bookingResponse = BookingResponse.fromEntity(booking)
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                ApiResponse(
                    success = true,
                    data = bookingResponse,
                    message = "Booking created successfully"
                )
            )
    }
}
