# Ticketing Microservices with Kafka

A distributed ticketing system built with Spring Boot microservices architecture, using Apache Kafka for event-driven communication and MySQL for data persistence.

## Architecture Overview

This system consists of 4 microservices:

- **API Gateway** (Port 8090) - Central entry point with routing, security, and circuit breaker
- **Inventory Service** (Port 8080) - Manages events, venues, and ticket inventory
- **Booking Service** (Port 8081) - Handles booking requests and publishes events to Kafka
- **Order Service** (Port 8082) - Consumes booking events from Kafka and creates orders

## Technology Stack

- **Java**: Java 21 (API Gateway, Booking Service, Order Service), Java 17 (Inventory Service)
- **Spring Boot**: 3.5.14 (API Gateway), 3.5.7 (Booking Service, Inventory Service), 3.4.12 (Order Service)
- **Spring Cloud**: 2025.0.2 (API Gateway)
- **Apache Kafka**: Event-driven messaging between services
- **MySQL**: Database persistence (ticketing_db)
- **Flyway**: Database migrations (Inventory Service)
- **Spring Cloud Gateway**: API routing and load balancing
- **Resilience4J**: Circuit breaker pattern implementation
- **Keycloak**: OAuth2 authentication and authorization
- **SpringDoc OpenAPI**: API documentation with Swagger UI
- **Lombok**: Reduce boilerplate code

---

## Microservices Details

### 1. API Gateway

**Port**: 8090  
**Java Version**: 21  
**Spring Boot Version**: 3.5.14  
**Spring Cloud Version**: 2025.0.2

#### Description
The API Gateway serves as the single entry point for all client requests. It provides routing to downstream services, security through OAuth2/Keycloak integration, circuit breaker patterns for fault tolerance, and aggregates API documentation from all services.

#### Key Features
- **Routing**: Routes requests to Booking Service and Inventory Service
- **Security**: OAuth2 Resource Server with Keycloak integration
- **Circuit Breaker**: Resilience4J implementation with configurable thresholds
- **API Documentation**: Aggregates Swagger UI from all services
- **Monitoring**: Spring Boot Actuator with all endpoints exposed
- **Timeout & Retry**: Configurable time limiter and retry mechanisms

#### Dependencies
- Spring Cloud Gateway Server WebMVC
- Spring Boot Starter OAuth2 Resource Server
- Resilience4J Circuit Breaker
- SpringDoc OpenAPI (WebMVC UI & API)
- Spring Boot Actuator

#### Configuration
```properties
server.port=8090
spring.application.name=apigateway

# Keycloak OAuth2
keycloak.auth.jwk-set-uri=http://localhost:8091/realms/ticketing-security-realm/protocol/openid-connect/certs
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8091/realms/ticketing-security-realm

# Circuit Breaker Configuration
resilience4j.circuitbreaker.configs.default.slidingWindowSize=8
resilience4j.circuitbreaker.configs.default.failureRateThreshold=50
resilience4j.circuitbreaker.configs.default.waitDurationInOpenState=5s
resilience4j.timelimiter.configs.default.timeout-duration=3s
resilience4j.retry.configs.default.max-attempts=3
```

#### Routes
- **POST** `/api/v1/booking` → Booking Service (8081)
- **GET** `/api/v1/inventory/venue/{venueId}` → Inventory Service (8080)
- **GET** `/api/v1/inventory/event/{eventId}` → Inventory Service (8080)
- **GET** `/docs/bookingservice/v3/api-docs` → Booking Service API Docs
- **GET** `/docs/inventoryservice/v3/api-docs` → Inventory Service API Docs
- **GET** `/swagger-ui.html` → Aggregated Swagger UI

#### Security
Excludes authentication for:
- `/swagger-ui.html`
- `/swagger-ui/**`
- `/docs/**`
- `/v3/api-docs/**`
- `/swagger-ressources/**`
- `/api-docs/**`

---

### 2. Inventory Service

**Port**: 8080  
**Java Version**: 17  
**Spring Boot Version**: 3.5.7

#### Description
The Inventory Service manages the core data model for events and venues. It provides APIs to query inventory information, check availability, and update event capacities when tickets are booked.

#### Key Features
- **Event Management**: CRUD operations for events
- **Venue Management**: CRUD operations for venues
- **Inventory Tracking**: Real-time capacity tracking
- **Database Migrations**: Flyway for schema versioning
- **API Documentation**: Swagger UI integration

#### Dependencies
- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Flyway Core & Flyway MySQL
- MySQL Connector
- SpringDoc OpenAPI (WebMVC UI & API)
- Lombok

#### Database Schema
**Venue Table**
- `id` (Long) - Primary key
- `name` (String) - Venue name
- `address` (String) - Venue address
- `total_capacity` (Long) - Total seating capacity

**Event Table**
- `id` (Long) - Primary key
- `name` (String) - Event name
- `total_capacity` (Long) - Total ticket capacity
- `left_capacity` (Long) - Remaining available tickets
- `venue_id` (Long) - Foreign key to Venue
- `ticket_price` (BigDecimal) - Price per ticket

#### Configuration
```properties
server.port=8080
spring.application.name=inventoryService

# MySQL Database
spring.datasource.url=jdbc:mysql://localhost:3306/ticketing_db
spring.datasource.username=root
spring.datasource.password=
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
spring.jpa.hibernate.ddl-auto=none
```

#### API Endpoints

**GET** `/api/v1/inventory/events`
- Returns list of all events with inventory information
- Response: `List<EventInventoryResponse>`

**GET** `/api/v1/inventory/venue/{venueId}`
- Returns venue information by ID
- Response: `VenueInventoryResponse`

**GET** `/api/v1/inventory/event/{eventId}`
- Returns event inventory details by ID
- Response: `EventInventoryResponse`

**PUT** `/api/v1/inventory/event/{eventId}/capacity/{capacity}`
- Updates event capacity (reduces available tickets)
- Path parameters: eventId, capacity (tickets booked)
- Response: 200 OK

#### Response Models

**EventInventoryResponse**
```java
{
  "eventId": Long,
  "event": String,
  "capacity": Long,
  "venue": Venue,
  "ticketPrice": BigDecimal
}
```

**VenueInventoryResponse**
```java
{
  "venueId": Long,
  "venueName": String,
  "totalCapacity": Long
}
```

---

### 3. Booking Service

**Port**: 8081  
**Java Version**: 21  
**Spring Boot Version**: 3.5.7

#### Description
The Booking Service handles ticket booking requests. It validates user existence, checks inventory availability, calculates pricing, and publishes booking events to Kafka for asynchronous order processing.

#### Key Features
- **Booking Validation**: Checks user existence and inventory availability
- **Event Publishing**: Publishes booking events to Kafka topic
- **Inventory Integration**: Calls Inventory Service to check availability
- **Pricing Calculation**: Calculates total price based on ticket count
- **API Documentation**: Swagger UI integration

#### Dependencies
- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Spring Kafka
- MySQL Connector
- SpringDoc OpenAPI (WebMVC UI & API)
- Lombok
- Spring Boot DevTools

#### Database Schema
**Customer Table**
- `id` (Long) - Primary key
- `name` (String) - Customer name
- `email` (String) - Customer email
- `address` (String) - Customer address

#### Configuration
```properties
server.port=8081
spring.application.name=Booking Service

# MySQL Database
spring.datasource.url=jdbc:mysql://localhost:3306/ticketing_db
spring.datasource.username=root
spring.datasource.password=
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
spring.jpa.hibernate.ddl-auto=none

# Kafka
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.template.default-topic=booking
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer

# Inventory Service
inventory.service.url=http://localhost:8000/api/v1/inventory
```

#### API Endpoints

**POST** `/api/v1/booking`
- Creates a new booking
- Request body: `BookingRequest`
- Response: `BookingResponse`

#### Request/Response Models

**BookingRequest**
```java
{
  "userId": Long,
  "eventId": Long,
  "ticketCount": Long
}
```

**BookingResponse**
```java
{
  "userId": Long,
  "eventId": Long,
  "ticketCount": Long,
  "totalPrice": BigDecimal
}
```

**BookingEvent** (Kafka Message)
```java
{
  "userId": Long,
  "eventId": Long,
  "ticketCount": Long,
  "totalPrice": BigDecimal
}
```

#### Booking Flow
1. Receive booking request
2. Validate customer exists in database
3. Call Inventory Service to check event availability
4. Calculate total price (ticketCount × ticketPrice)
5. Create BookingEvent
6. Publish BookingEvent to Kafka topic "booking"
7. Return BookingResponse to client

---

### 4. Order Service

**Port**: 8082  
**Java Version**: 21  
**Spring Boot Version**: 3.4.12

#### Description
The Order Service consumes booking events from Kafka and processes them asynchronously. It creates order records in the database and updates the inventory service to reflect the booked tickets.

#### Key Features
- **Event Consumption**: Listens to Kafka topic "booking"
- **Order Creation**: Persists orders to database
- **Inventory Update**: Calls Inventory Service to reduce capacity
- **Asynchronous Processing**: Decouples booking from order creation
- **Error Handling**: Logs processing errors

#### Dependencies
- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Spring Kafka
- MySQL Connector
- Lombok
- Spring Kafka Test

#### Database Schema
**Order Table**
- `id` (Long) - Primary key (auto-generated)
- `total` (BigDecimal) - Total order price
- `quantity` (Long) - Number of tickets
- `place_at` (LocalDateTime) - Order timestamp
- `customer_id` (Long) - Foreign key to customer
- `event_id` (Long) - Foreign key to event

#### Configuration
```properties
server.port=8082
spring.application.name=orderservice

# MySQL Database
spring.datasource.url=jdbc:mysql://localhost:3306/ticketing_db
spring.datasource.username=root
spring.datasource.password=
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
spring.jpa.hibernate.ddl-auto=none

# Kafka Consumer
spring.kafka.bootstrap-server=localhost:9092
spring.kafka.consumer.group-id=order-service
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.type.mapping=bookingEvent:com.exemple.elz.event.bookingEvent

# Inventory Service
inventory.service.url=http://localhost:8080/api/v1/inventory
```

#### Order Processing Flow
1. Listen to Kafka topic "booking" with group-id "order-service"
2. Receive BookingEvent message
3. Create Order entity from BookingEvent
4. Save Order to database
5. Call Inventory Service to update event capacity
6. Log successful processing

#### Order Entity
```java
{
  "id": Long,
  "totalPrice": BigDecimal,
  "ticketCount": Long,
  "placeAt": LocalDateTime,
  "customerId": Long,
  "eventId": Long
}
```

---

## System Architecture Flow

```
Client Request
     ↓
API Gateway (8090)
     ↓
     ├─→ Booking Service (8081)
     │        ↓
     │   Check Inventory
     │        ↓
     │   Publish to Kafka
     │        ↓
     │   Kafka Topic: "booking"
     │        ↓
     │   Order Service (8082)
     │        ↓
     │   Create Order
     │        ↓
     │   Update Inventory
     │
     └─→ Inventory Service (8080)
              ↓
         Query/Update Data
```

## Prerequisites

- Java 17 or 21
- Maven 3.6+
- MySQL 8.0+
- Apache Kafka 2.8+
- Keycloak (optional, for security)

## Database Setup

Create MySQL database:
```sql
CREATE DATABASE ticketing_db;
```

Run Flyway migrations (Inventory Service will handle this automatically).

## Running the Services

### Start Kafka and MySQL
```bash
# Start Kafka
kafka-server-start.sh config/server.properties

# Start MySQL
mysql -u root -p
```

### Start Services (in order)
```bash
# 1. Inventory Service
cd inventoryService
mvn spring-boot:run

# 2. Booking Service
cd bookingService
mvn spring-boot:run

# 3. Order Service
cd orderService
mvn spring-boot:run

# 4. API Gateway
cd apigateway
mvn spring-boot:run
```

## API Documentation

Once all services are running, access the aggregated Swagger UI:
```
http://localhost:8090/swagger-ui.html
```

Individual service documentation:
- Inventory Service: http://localhost:8080/swagger-ui.html
- Booking Service: http://localhost:8081/swagger-ui.html

## Example Usage

### Create a Booking
```bash
curl -X POST http://localhost:8090/api/v1/booking \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "eventId": 1,
    "ticketCount": 2
  }'
```

### Check Event Inventory
```bash
curl http://localhost:8090/api/v1/inventory/event/1
```

### Check Venue Information
```bash
curl http://localhost:8090/api/v1/inventory/venue/1
```

### Get All Events
```bash
curl http://localhost:8090/api/v1/inventory/events
```

## Monitoring

The API Gateway exposes Spring Boot Actuator endpoints:
```
http://localhost:8090/actuator
http://localhost:8090/actuator/health
http://localhost:8090/actuator/circuitbreakers
```

## Security Configuration

The system uses Keycloak for OAuth2 authentication. Configure Keycloak realm:
- Realm: `ticketing-security-realm`
- URL: `http://localhost:8091`

Update the gateway configuration with your Keycloak instance details.

## Circuit Breaker Configuration

The API Gateway uses Resilience4J with the following defaults:
- Sliding Window Size: 8 calls
- Failure Rate Threshold: 50%
- Wait Duration in Open State: 5 seconds
- Timeout Duration: 3 seconds
- Max Retry Attempts: 3
- Retry Wait Duration: 2 seconds

## Troubleshooting

### Service Unavailable
If a service is down, the circuit breaker will return:
```
Status: 503 SERVICE_UNAVAILABLE
Body: "Booking service is down"
```

### Kafka Connection Issues
Ensure Kafka is running on `localhost:9092` and the topic "booking" exists.

### Database Connection Issues
Verify MySQL is running and the database `ticketing_db` exists with proper credentials.

## Future Enhancements

- Add order history endpoint
- Implement payment processing
- Add email notifications
- Implement distributed tracing
- Add comprehensive logging
- Implement caching with Redis
- Add service discovery with Eureka
- Implement rate limiting
- Add comprehensive unit and integration tests