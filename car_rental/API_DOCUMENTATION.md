# REST API Dokumentáció

## Base URL
```
http://localhost:8080/api/v1
```

## Végpontok

### 1. Elérhető autók listája

**Végpont:** `GET /api/v1/cars/available`

**Leírás:** Visszaad egy listát amiben az elérhető autók szerepelnek, opcionálisan szűrve kezdő és vég dátumokkal.

**Lekérdezés Paraméterei:**
- `startDate` (opcionális): Kezdő dátum ISO formátumban (YYYY-MM-DD)
- `endDate` (opcionális): Vég dátum ISO formátumban (YYYY-MM-DD)

**Lekérdezés minták:**
```bash
curl "http://localhost:8080/api/v1/cars/available"

curl "http://localhost:8080/api/v1/cars/available?startDate=2025-12-10&endDate=2025-12-15"
```

**Sikeres válasz (200 OK):**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "brand": "Toyota",
      "model": "Camry",
      "dailyRate": 50.00,
      "imagePath": "/images/abc123.jpg"
    },
    {
      "id": 2,
      "brand": "Honda",
      "model": "Civic",
      "dailyRate": 45.00,
      "imagePath": "/images/def456.jpg"
    }
  ],
  "message": "Available cars retrieved successfully"
}
```

---

### 2. Foglalás létrehozása

**Végpont:** `POST /api/v1/bookings`

**Leírás:** Létrehoz egy új foglalást.

**Kérés header:**
```
Content-Type: application/json
```

**Kérés Body:**
```json
{
  "carId": 1,
  "customerName": "John Doe",
  "customerEmail": "john.doe@example.com",
  "customerAddress": "123 Main Street, City",
  "customerPhone": "+36201234567",
  "startDate": "2025-12-10",
  "endDate": "2025-12-15"
}
```

**Validációs szabályok:**
- `carId`: (kötelező), pozitív szám.
- `customerName`: (kötelező), 2-100 karakter.
- `customerEmail`: (kötelező), érvényes email formátum.
- `customerAddress`: (kötelező), 5-200 karakter.
- `customerPhone`: (kötelező), érvényes telefonszám formátum (10-15 digits, opcionális + prefix)
- `startDate`: (kötelező), ISO dátum formátum (YYYY-MM-DD), nem lehet a múltban.
- `endDate`: (kötelező), ISO dátum formátum.(YYYY-MM-DD), Későbbi vagy ugyanaz kell legyen, mint a kezdő dátum.

**Lekérdezés minta:**
```bash
curl -X POST http://localhost:8080/api/v1/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "carId": 1,
    "customerName": "John Doe",
    "customerEmail": "john.doe@example.com",
    "customerAddress": "123 Main Street, Budapest",
    "customerPhone": "+36201234567",
    "startDate": "2025-12-10",
    "endDate": "2025-12-15"
  }'
```

**Sikeres válasz (201 Created):**
```json
{
  "success": true,
  "data": {
    "id": 42,
    "car": {
      "id": 1,
      "brand": "Toyota",
      "model": "Camry",
      "dailyRate": 50.00,
      "imagePath": "/images/abc123.jpg"
    },
    "customerName": "John Doe",
    "customerEmail": "john.doe@example.com",
    "customerAddress": "123 Main Street, Budapest",
    "customerPhone": "+36201234567",
    "startDate": "2025-12-10",
    "endDate": "2025-12-15",
    "numberOfDays": 6,
    "totalAmount": 300.00,
    "bookingStatus": "PENDING",
    "createdAt": "2025-12-01T15:30:00"
  },
  "message": "Booking created successfully"
}
```

---

## Hiba válaszok

Mindegyik a következő formátumot követi:

```json
{
  "timestamp": "2025-12-01T15:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Detailed error message",
  "path": "/api/v1/bookings"
}
```

### Gyakori HTTP státusz kódok

- **200 OK**: Sikeres kérés
- **201 Created**: Erőforrás sikeresen létrehozva
- **400 Bad Request**: Érvénytelen kérés paraméterek vagy validációs hiba
- **404 Not Found**: Erőforrás nem található
- **409 Conflict**: Erőforrás konfliktus (például az autó már le van foglalva a megadott időtartamra)
- **500 Internal Server Error**: Szerver hiba

### Hiba példák

**Validációs hiba (400):**
```json
{
  "timestamp": "2025-12-01T15:30:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "customerEmail: Invalid email format, customerPhone: Invalid phone number format",
  "path": "/api/v1/bookings"
}
```

**Autó nem elérhető (409):**
```json
{
  "timestamp": "2025-12-01T15:30:00",
  "status": 409,
  "error": "Conflict",
  "message": "Car is already booked for the selected dates",
  "path": "/api/v1/bookings"
}
```

**Autó nem található (400):**
```json
{
  "timestamp": "2025-12-01T15:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Car not found with id: 999",
  "path": "/api/v1/bookings"
}
```

**Nem megfelelő dátum intervallum (400):**
```json
{
  "timestamp": "2025-12-01T15:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "End date must be after or equal to start date",
  "path": "/api/v1/bookings"
}
```