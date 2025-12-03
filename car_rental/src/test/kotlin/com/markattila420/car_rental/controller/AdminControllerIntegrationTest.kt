package com.markattila420.car_rental.controller

import com.markattila420.car_rental.entity.Booking
import com.markattila420.car_rental.entity.BookingStatus
import com.markattila420.car_rental.entity.Car
import com.markattila420.car_rental.repository.BookingRepository
import com.markattila420.car_rental.repository.CarRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.web.context.WebApplicationContext
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.view
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.model
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class AdminControllerIntegrationTest {

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var carRepository: CarRepository

    @Autowired
    private lateinit var bookingRepository: BookingRepository

    @Value("\${admin.username}")
    private lateinit var adminUsername: String

    @Value("\${admin.password}")
    private lateinit var adminPassword: String

    private lateinit var testCar: Car
    private lateinit var testBooking: Booking

    @BeforeEach
    fun setup() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .build()
        bookingRepository.deleteAll()
        carRepository.deleteAll()

        testCar = carRepository.save(
            Car(
                brand = "Toyota",
                model = "Camry",
                dailyRate = BigDecimal("500"),
                imagePath = "/images/test-car.jpg",
                active = true,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )

        testBooking = bookingRepository.save(
            Booking(
                car = testCar,
                customerName = "John Doe",
                customerEmail = "john@example.com",
                customerAddress = "123 Test St",
                customerPhone = "+1234567890",
                startDate = LocalDate.now().plusDays(1),
                endDate = LocalDate.now().plusDays(3),
                numberOfDays = 2,
                totalAmount = BigDecimal("1000"),
                bookingStatus = BookingStatus.PENDING,
                createdAt = LocalDateTime.now()
            )
        )
    }

    @AfterEach
    fun cleanup() {
        bookingRepository.deleteAll()
        carRepository.deleteAll()
    }

    @Test
    fun `should allow access to admin page with correct credentials`() {
        mockMvc.perform(get("/admin")
            .with(httpBasic(adminUsername, adminPassword)))
            .andExpect(status().isOk)
            .andExpect(view().name("admin"))
    }

    @Test
    fun `should deny access to admin page with incorrect credentials`() {
        mockMvc.perform(get("/admin")
            .with(httpBasic("wronguser", "wrongpassword")))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should load admin page with bookings and cars`() {
        mockMvc.perform(get("/admin")
            .with(httpBasic(adminUsername, adminPassword)))
            .andExpect(status().isOk)
            .andExpect(view().name("admin"))
            .andExpect(model().attributeExists("bookings"))
            .andExpect(model().attributeExists("cars"))
            .andExpect(model().attributeExists("bookingStatuses"))
    }

    @Test
    fun `should update booking status successfully`() {
        mockMvc.perform(
            post("/admin/booking/{id}/status", testBooking.id)
                .param("status", BookingStatus.CONFIRMED.name)
                .with(httpBasic(adminUsername, adminPassword))
        )
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/admin"))
            .andExpect(flash().attributeExists("message"))

        val updatedBooking = bookingRepository.findById(testBooking.id!!).orElseThrow()
        assert(updatedBooking.bookingStatus == BookingStatus.CONFIRMED)
    }

    @Test
    fun `should return error when updating non-existent booking status`() {
        mockMvc.perform(
            post("/admin/booking/{id}/status", 99999L)
                .param("status", BookingStatus.CONFIRMED.name)
                .with(httpBasic(adminUsername, adminPassword))
        )
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/admin"))
            .andExpect(flash().attributeExists("error"))
    }

    @Test
    fun `should delete booking successfully`() {
        mockMvc.perform(post("/admin/booking/{id}/delete", testBooking.id)
            .with(httpBasic(adminUsername, adminPassword)))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/admin"))
            .andExpect(flash().attributeExists("message"))

        assert(!bookingRepository.existsById(testBooking.id!!))
    }

    @Test
    fun `should update car details successfully`() {
        mockMvc.perform(
            multipart("/admin/car/{id}/update", testCar.id)
                .param("brand", "Honda")
                .param("model", "Accord")
                .param("dailyRate", "1000")
                .param("active", "true")
                .with(httpBasic(adminUsername, adminPassword))
                .with { request ->
                    request.method = "POST"
                    request
                }
        )
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/admin"))
            .andExpect(flash().attributeExists("message"))

        val updatedCar = carRepository.findById(testCar.id!!).orElseThrow()
        assert(updatedCar.brand == "Honda")
        assert(updatedCar.model == "Accord")
        assert(updatedCar.dailyRate == BigDecimal("1000"))
    }

    @Test
    fun `should cancel active bookings when car is deactivated`() {
        mockMvc.perform(
            multipart("/admin/car/{id}/update", testCar.id)
                .param("brand", testCar.brand)
                .param("model", testCar.model)
                .param("dailyRate", testCar.dailyRate.toString())
                .param("active", "false")
                .with(httpBasic(adminUsername, adminPassword))
                .with { request ->
                    request.method = "POST"
                    request
                }
        )
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/admin"))
            .andExpect(flash().attributeExists("message"))

        val updatedBooking = bookingRepository.findById(testBooking.id!!).orElseThrow()
        assert(updatedBooking.bookingStatus == BookingStatus.CANCELLED)

        val updatedCar = carRepository.findById(testCar.id!!).orElseThrow()
        assert(!updatedCar.active)
    }

    @Test
    fun `should add new car successfully with image`() {
        val imageFile = MockMultipartFile(
            "image",
            "test-image.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "test image content".toByteArray()
        )

        mockMvc.perform(
            multipart("/admin/car/add")
                .file(imageFile)
                .param("brand", "Ford")
                .param("model", "Mustang")
                .param("dailyRate", "800")
                .param("active", "true")
                .with(httpBasic(adminUsername, adminPassword))
        )
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/admin"))
            .andExpect(flash().attributeExists("message"))

        val cars = carRepository.findAll()
        assert(cars.any { it.brand == "Ford" && it.model == "Mustang" })
    }

    @Test
    fun `should return error when adding car without image`() {
        val emptyFile = MockMultipartFile(
            "image",
            "".toByteArray()
        )

        mockMvc.perform(
            multipart("/admin/car/add")
                .file(emptyFile)
                .param("brand", "Ford")
                .param("model", "Mustang")
                .param("dailyRate", "800")
                .param("active", "true")
                .with(httpBasic(adminUsername, adminPassword))
        )
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/admin"))
            .andExpect(flash().attributeExists("error"))
    }

    @Test
    fun `should update car with new image`() {
        val imageFile = MockMultipartFile(
            "image",
            "new-image.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "new test image content".toByteArray()
        )

        mockMvc.perform(
            multipart("/admin/car/{id}/update", testCar.id)
                .file(imageFile)
                .param("brand", testCar.brand)
                .param("model", testCar.model)
                .param("dailyRate", testCar.dailyRate.toString())
                .param("active", "true")
                .with(httpBasic(adminUsername, adminPassword))
                .with { request ->
                    request.method = "POST"
                    request
                }
        )
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/admin"))
            .andExpect(flash().attributeExists("message"))
    }
}
