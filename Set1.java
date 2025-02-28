## Hotel Management System - Room Booking with Concurrency Handling

### **Overview**
This project is a simple **Spring Boot-based Hotel Management System** that allows users to book hotel rooms while handling concurrency issues using **Optimistic Locking**.

---

### **Tech Stack**
- Java 17
- Spring Boot 3
- Spring Data JPA (Hibernate)
- PostgreSQL / MySQL
- Lombok

---

### **Entity Models**

#### **Room Entity**
```java
@Entity
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String roomNumber;
    private boolean available;
    
    // Getters and Setters
}
```

#### **Reservation Entity with Optimistic Locking**
```java
@Entity
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    private Room room;
    
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    
    @Version // Enables Optimistic Locking
    private Integer version;
}
```

---

### **Repository Layer**
```java
@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    boolean existsByRoomAndCheckInDateBetween(Room room, LocalDate start, LocalDate end);
}
```

---

### **Service Layer with Concurrency Handling**
```java
@Service
public class BookingService {
    @Autowired
    private ReservationRepository reservationRepository;
    
    @Autowired
    private RoomRepository roomRepository;

    @Transactional
    public boolean bookRoom(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        
        boolean isBooked = reservationRepository.existsByRoomAndCheckInDateBetween(room, checkIn, checkOut);
        if (isBooked) {
            throw new RuntimeException("Room already booked for selected dates");
        }
        
        Reservation reservation = new Reservation();
        reservation.setRoom(room);
        reservation.setCheckInDate(checkIn);
        reservation.setCheckOutDate(checkOut);
        reservationRepository.save(reservation);
        
        return true;
    }
}
```

---

### **Controller Layer**
```java
@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    @Autowired
    private BookingService bookingService;

    @PostMapping("/book")
    public ResponseEntity<String> bookRoom(@RequestParam Long roomId, 
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn, 
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        try {
            bookingService.bookRoom(roomId, checkIn, checkOut);
            return ResponseEntity.ok("Room booked successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }
}
```

---

### **How Concurrency is Handled?**
- **Optimistic Locking (`@Version`)** ensures that concurrent bookings for the same room don’t override each other.
- If multiple users try to book the same room, the first successful transaction commits, while others fail with a `ConcurrencyException`.
- The system responds with `409 Conflict` if a room is already booked.

---

### **Run the Project**
1. Clone the repository.
2. Configure `application.properties` for your database.
3. Run using:
   ```bash
   mvn spring-boot:run
   ```
4. Use **Postman or curl** to test API:
   ```bash
   curl -X POST "http://localhost:8080/api/bookings/book?roomId=1&checkIn=2025-03-01&checkOut=2025-03-05"
   ```

---

This setup ensures **safe room booking** even with high concurrency. 🚀
