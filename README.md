# Autókölcsönző alkalmazás

Full-stack autólölcsönző webalkalmazás Spring Boot és Kotlin használatával. 

## Funkciók

### Publikus felület
- **Autókeresés**: Elérhető autók szűrése dátumtartomány megadásával.
- **Valós idejű elérhetőség**: A kiválasztott időszakra elérhető autók megjelenítése.
- **Foglalási rendszer**: Autó lefoglalása személyes adatok megadásával.
- **Automatikus árazás**: Teljes költség kiszámítása bérlési időtartam alapján.
- **Ütközésmegelőzés**: Ugyanarra a dátumra nem lehet ugyanazt az autót kétszer lefoglalni.

### Admin felület
- **Folgalás kezelés**: Összes foglalás megtekintése, szerkesztése és törlése.
- **Autó kezelés**: Autók hozzádása, szerkesztése és deaktiválása.
- **Biztonságos hozzáférés**:Adminisztrátori hitelesítés alkalmazás konfiguráción keresztül.
- **Adatmegőrzés**: Autókat nem lehet törölni, csak deaktiválni.

### REST API
- Autók lekérdezése adott dátumtartományra.
- Új foglalás létrehozása API-n keresztül.
- Részletes API dokumentáció elérhető a `car_rental/API_DOCUMENTATION.md` fájlban.

### Technical Features
- Több mint 50%-os teszt lefedettség
- Docker konzténerizáció
- PostgreSQL adatbázis
- Responzív felület Bootstrap használatával
- Spring Security a hitelesítéshez.

## Tech Stack
- **Backend**: Spring Boot 4.0, Kotlin 2.2
- **Frontend**: Thymeleaf, Bootstrap 5, jQuery
- **Adatbázis**: PostgreSQL 15
- **Security**: Spring Security
- **Tesztelés**: JUnit 5, MockK, H2 (test database)
- **Conténerizáció**: Docker, Docker Compose
- **Build eszköz**: Gradle (Kotlin DSL)

## Előfeltételek

- Docker és Docker Compose telepítve a rendszeren

## Indítási útmutató

1. **Repository klónozása**
   ```bash
   git clone https://github.com/MarkAttila420/car_rental.git
   ```

2. **Alkalmazás elindítása**
   ```powershell
   docker-compose up --build
   ```

3. **Hozzáférés az alkalmazáshoz**
   - Publikus felület: http://localhost:8080
   - Admin felület: http://localhost:8080/admin
   - API végpontok: http://localhost:8080/api

4. **Almalmazás leállítása**
   ```powershell
   docker-compose down
   ```

## Admin hitelesítő adatok

Az admin felület elérhető a `/admin` végponton a következő hitelesítő adatokkal:
- **Felhasználónév**: `admin`
- **Jelszó**: `Password1`

## Tesztelés

Tesztkörnyezet futtatása:

```powershell
cd car_rental
./gradlew test
```

## API Dokumentáció

Részletes API dokumentáció megtalálható a `car_rental/API_DOCUMENTATION.md` fájlban.
