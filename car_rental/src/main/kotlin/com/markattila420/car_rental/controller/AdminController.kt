package com.markattila420.car_rental.controller

import com.markattila420.car_rental.entity.BookingStatus
import com.markattila420.car_rental.repository.BookingRepository
import com.markattila420.car_rental.repository.CarRepository
import com.markattila420.car_rental.service.CarService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import java.math.BigDecimal
import java.time.LocalDateTime

@Controller
@RequestMapping("/admin")
class AdminController(
    private val bookingRepository: BookingRepository,
    private val carRepository: CarRepository,
    private val carService: CarService
) {
    
    @GetMapping
    fun adminPage(model: Model): String {
        val bookings = bookingRepository.findAll().sortedByDescending { it.createdAt }
        val cars = carRepository.findAll().sortedBy { it.id }
        
        model.addAttribute("bookings", bookings)
        model.addAttribute("bookingStatuses", BookingStatus.values())
        model.addAttribute("cars", cars)
        return "admin"
    }
    
    @PostMapping("/booking/{id}/status")
    fun updateBookingStatus(
        @PathVariable id: Long,
        @RequestParam status: BookingStatus,
        redirectAttributes: RedirectAttributes
    ): String {
        val booking = bookingRepository.findById(id).orElse(null)
        if (booking != null) {
            val updatedBooking = booking.copy(bookingStatus = status)
            bookingRepository.save(updatedBooking)
            redirectAttributes.addFlashAttribute("message", "Booking status updated successfully")
        } else {
            redirectAttributes.addFlashAttribute("error", "Booking not found")
        }
        return "redirect:/admin"
    }
    
    @PostMapping("/booking/{id}/delete")
    fun deleteBooking(
        @PathVariable id: Long,
        redirectAttributes: RedirectAttributes
    ): String {
        try {
            bookingRepository.deleteById(id)
            redirectAttributes.addFlashAttribute("message", "Booking deleted successfully")
        } catch (e: Exception) {
            redirectAttributes.addFlashAttribute("error", "Error deleting booking: ${e.message}")
        }
        return "redirect:/admin"
    }
    
    @PostMapping("/car/{id}/update")
    fun updateCar(
        @PathVariable id: Long,
        @RequestParam brand: String,
        @RequestParam model: String,
        @RequestParam dailyRate: BigDecimal,
        @RequestParam active: Boolean,
        @RequestParam(required = false) image: MultipartFile?,
        redirectAttributes: RedirectAttributes
    ): String {
        val car = carRepository.findById(id).orElse(null)
        if (car != null) {
            // Check if car is being deactivated
            val wasActive = car.active
            val isBeingDeactivated = wasActive && !active
            
            // Update image if provided
            var newImagePath = car.imagePath
            if (image != null && !image.isEmpty) {
                try {
                    newImagePath = carService.updateCarImage(id, image)
                } catch (e: Exception) {
                    redirectAttributes.addFlashAttribute("error", "Error updating image: ${e.message}")
                    return "redirect:/admin"
                }
            }
            
            // Update the car
            val updatedCar = car.copy(
                brand = brand,
                model = model,
                dailyRate = dailyRate,
                active = active,
                imagePath = newImagePath,
                updatedAt = LocalDateTime.now()
            )
            carRepository.save(updatedCar)
            
            // If car is being deactivated, cancel all active bookings
            if (isBeingDeactivated) {
                val activeBookings = bookingRepository.findActiveBookingsByCarId(id)
                val cancelledCount = activeBookings.size
                
                activeBookings.forEach { booking ->
                    val cancelledBooking = booking.copy(bookingStatus = BookingStatus.CANCELLED)
                    bookingRepository.save(cancelledBooking)
                }
                
                if (cancelledCount > 0) {
                    redirectAttributes.addFlashAttribute("message", 
                        "Car updated successfully. $cancelledCount active booking(s) have been cancelled.")
                } else {
                    redirectAttributes.addFlashAttribute("message", "Car updated successfully")
                }
            } else {
                redirectAttributes.addFlashAttribute("message", "Car updated successfully")
            }
        } else {
            redirectAttributes.addFlashAttribute("error", "Car not found")
        }
        return "redirect:/admin"
    }
    
    @PostMapping("/car/add")
    fun addCar(
        @RequestParam brand: String,
        @RequestParam model: String,
        @RequestParam dailyRate: BigDecimal,
        @RequestParam(required = false, defaultValue = "false") active: Boolean,
        @RequestParam image: MultipartFile,
        redirectAttributes: RedirectAttributes
    ): String {
        try {
            if (image.isEmpty) {
                redirectAttributes.addFlashAttribute("error", "Image file is required")
                return "redirect:/admin"
            }
            
            carService.saveCar(brand, model, dailyRate, active, image)
            redirectAttributes.addFlashAttribute("message", "Car added successfully")
        } catch (e: Exception) {
            redirectAttributes.addFlashAttribute("error", "Error adding car: ${e.message}")
        }
        return "redirect:/admin"
    }
}
