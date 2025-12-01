package com.markattila420.car_rental.service

import com.markattila420.car_rental.dto.CarDto
import com.markattila420.car_rental.entity.Car
import com.markattila420.car_rental.repository.BookingRepository
import com.markattila420.car_rental.repository.CarRepository
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

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
    
    fun saveCar(brand: String, model: String, dailyRate: BigDecimal, active: Boolean, imageFile: MultipartFile): Car {
        val imagePath = saveImage(imageFile)
        
        val car = Car(
            brand = brand,
            model = model,
            dailyRate = dailyRate,
            active = active,
            imagePath = imagePath,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        return carRepository.save(car)
    }
    
    fun updateCarImage(carId: Long, imageFile: MultipartFile): String {
        if (imageFile.isEmpty) {
            throw IllegalArgumentException("File is empty")
        }
        
        // Save the new image
        return saveImage(imageFile)
    }
    
    private fun saveImage(file: MultipartFile): String {
        if (file.isEmpty) {
            throw IllegalArgumentException("File is empty")
        }
        
        // Generate unique filename
        val originalFilename = file.originalFilename ?: "image"
        val extension = originalFilename.substringAfterLast(".", "jpg")
        val uniqueFilename = "${UUID.randomUUID()}.${extension}"
        
        // Save to both locations to ensure it works in development and production
        // 1. Save to source directory (for development, in case running from IDE)
        val sourceDir = Paths.get("src/main/resources/static/images")
        if (!Files.exists(sourceDir)) {
            Files.createDirectories(sourceDir)
        }
        Files.copy(file.inputStream, sourceDir.resolve(uniqueFilename), StandardCopyOption.REPLACE_EXISTING)
        
        // 2. Save to build directory (for running application)
        val buildDir = Paths.get("build/resources/main/static/images")
        if (!Files.exists(buildDir)) {
            Files.createDirectories(buildDir)
        }
        // Reset the input stream before copying again
        Files.copy(file.bytes.inputStream(), buildDir.resolve(uniqueFilename), StandardCopyOption.REPLACE_EXISTING)
        
        // Return the path that will be used in the HTML (relative to static folder)
        return "/images/$uniqueFilename"
    }
}
