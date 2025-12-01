package com.markattila420.car_rental.dto

import com.fasterxml.jackson.annotation.JsonFormat
import jakarta.validation.constraints.*
import java.time.LocalDate

data class CreateBookingRequest(
    @field:NotNull(message = "Car ID is required")
    @field:Positive(message = "Car ID must be positive")
    val carId: Long,
    
    @field:NotBlank(message = "Customer name is required")
    @field:Size(min = 2, max = 100, message = "Customer name must be between 2 and 100 characters")
    val customerName: String,
    
    @field:NotBlank(message = "Customer email is required")
    @field:Email(message = "Invalid email format")
    val customerEmail: String,
    
    @field:NotBlank(message = "Customer address is required")
    @field:Size(min = 5, max = 200, message = "Customer address must be between 5 and 200 characters")
    val customerAddress: String,
    
    @field:NotBlank(message = "Customer phone is required")
    @field:Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid phone number format")
    val customerPhone: String,
    
    @field:NotNull(message = "Start date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    val startDate: LocalDate,
    
    @field:NotNull(message = "End date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    val endDate: LocalDate
)
