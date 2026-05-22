# Ticketing Microservices Architecture

A distributed ticketing system built with Spring Boot microservices architecture, utilizing Apache Kafka for event-driven communication and Spring Cloud Gateway for API routing.

## 📋 Table of Contents

- [Architecture Overview](#architecture-overview)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Microservices Description](#microservices-description)
  - [API Gateway](#1-api-gateway)
  - [Booking Service](#2-booking-service)
  - [Inventory Service](#3-inventory-service)
  - [Order Service](#4-order-service)
- [Database Schema](#database-schema)
- [Kafka Topics](#kafka-topics)
- [Setup Instructions](#setup-instructions)
- [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
- [Testing](#testing)

## 🏗️ Architecture Overview

This ticketing system follows a microservices architecture with the following components:

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
       ▼
┌─────────────────┐
│  API Gateway    │ (Port 8090)
│  (Spring Cloud  │
│   Gateway MVC)  │
└──────┬──────────┘
       │
       ├───► Booking Service (Port 8081)
       │        │
       │        ├──► Kafka Topic: "booking"
       │        │
       │        └──► Inventory Service (Port 8080)
       │
       └───► Inventory Service (Port 8080)
                │
                └──► Order Service (Port 8082)
                         │
                         └──► Kafka Consumer: "booking"
```

### Communication Flow

1. **Client** requests a booking through API Gateway
2. **API Gateway** routes the request to Booking Service
3. **Booking Service** validates user existence and checks inventory availability
4. **Booking Service** publishes a booking event to Kafka
5. **Order Service** consumes the booking event from Kafka
6. **Order Service** creates an order and updates inventory

## 🛠️ Technology Stack

### Common Technologies
- **Java**: 17 (Inventory Service), 21 (API Gateway, Booking Service, Order Service)
- **Spring Boot**: 3.4.12 - 3.5.14
- **Spring Cloud**: 2025.0.2
- **MySQL**: Database
- **Lombok**: Code generation
- **SpringDoc OpenAPI**: API documentation (v2.8.16)

### Service-Specific Technologies

#### API Gateway
- Spring Cloud Gateway Server MVC
- Spring Cloud Circuit Breaker (Resilience4J)
- Spring Boot OAuth2 Resource Server
- Keycloak integration

#### Booking Service
- Spring Kafka (Producer)
- Spring Data JPA
- MySQL Connector

#### Inventory Service
- Spring Data JPA
- Flyway (Database Migration)
- MySQL Connector

#### Order Service
- Spring Kafka (Consumer)
- Spring Data JPA
- MySQL Connector

## 📦 Prerequisites

Before running this application, ensure you have the following installed:

- **Java JDK**: 17 or 21
- **Maven**: 3.6+
- **MySQL**: 8.0+
- **Apache Kafka**: 2.8+
- **Keycloak**: 20+ (for authentication)
- **Git**: For cloning the repository

## 🔍 Microservices Description

### 1. API Gateway

**Port**: 8090  
**Package**: `com.elz.apigateway`

The API Gateway serves as the single entry point for all client requests, providing routing, load balancing, security, and resilience features.

#### Key Features
- **Request Routing**: Routes requests to appropriate microservices
- **Circuit Breaker**: Implements Resilience4J for fault tolerance
- **OAuth2 Security**: Integrates with Keycloak for authentication
- **API Documentation Aggregation**: Aggregates OpenAPI docs from all services
- **Actuator Endpoints**: Health checks and monitoring

#### Configuration
```properties
spring.application.name=apigateway
server.port=8090

# Keycloak Configuration
keycloak.auth.jwk-set-uri=http://localhost:8091/realms/ticketing-security-realm/protocol/openid-connect/certs
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8091/realms/ticketing-security-realm

# Circuit Breaker Configuration
resilience4j.circuitbreaker.configs.default.slidingWindowSize=8
resilience4j.circuitbreaker.configs.default.failureRateThreshold=50
resilience4j.circuitbreaker.configs.default.waitDurationInOpenState=5s
```

#### Routes
- **POST** `/api/v1/booking` → Booking Service (localhost:8081)
- **GET** `/api/v1/inventory/venue/{venueId}` → Inventory Service (localhost:8080)
- **GET** `/api/v1/inventory/event/{eventId}` → Inventory Service (localhost:8080)
- **GET** `/docs/bookingservice/v3/api-docs` → Booking Service API Docs
- **GET** `/docs/inventoryservice/v3/api-docs` → Inventory Service API Docs

#### Dependencies
- spring-cloud-starter-gateway-server-webmvc
- spring-cloud-starter-circuitbreaker-resilience4j
- spring-boot-starter-oauth2-resource-server
- springdoc-openapi-starter-webmvc-ui
- spring-boot-starter-actuator

---

### 2. Booking Service

**Port**: 8081  
**Package**: `com.elz.bookingService`

The Booking Service handles ticket booking requests, validates user information, checks inventory availability, and publishes booking events to Kafka.

#### Key Features
- **Booking Creation**: Creates new booking requests
- **User Validation**: Verifies customer existence in database
- **Inventory Check**: Validates ticket availability before booking
- **Event Publishing**: Publishes booking events to Kafka topic
- **REST API**: Provides RESTful endpoints for booking operations

#### Entities
```java
@Entity
@Table(name = "customer")
public class Customer {
    @Id
    private Long id;
    private String name;
    private String email;
    private String address;
}
```

#### Events
```java
public class BookingEvent {
    private Long userId;
    private Long eventId;
    private Long ticketCount;
    private BigDecimal totalPrice;
}
```

#### API Endpoints
- **POST** `/api/v1/booking` - Create a new booking

**Request Body**:
```json
{
  "userId": 1,
  "eventId": 1,
  "ticketCount": 2
}
```

**Response**:
```json
{
  "userId": 1,
  "eventId": 1,
  "ticketCount": 2,
  "totalPrice": 100.00
}
```

#### Configuration
```properties
spring.application.name=Booking Service
server.port=8081

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/ticketing_db
spring.datasource.username=root
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=none

# Kafka
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.template.default-topic=booking
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer

# Inventory Service
inventory.service.url=http://localhost:8000/api/v1/inventory
```

#### Dependencies
- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-kafka
- mysql-connector-j
- lombok
- springdoc-openapi-starter-webmvc-ui

---

### 3. Inventory Service

**Port**: 8080  
**Package**: `com.org.elz.inventoryService`

The Inventory Service manages event and venue information, tracks ticket availability, and provides inventory data to other services.

#### Key Features
- **Event Management**: Manages event information and capacity
- **Venue Management**: Manages venue details
- **Inventory Tracking**: Tracks available tickets for events
- **Capacity Updates**: Updates event capacity after bookings
- **Database Migration**: Uses Flyway for schema management

#### Entities
```java
@Entity
@Table(name = "event")
public class Event {
    @Id
    private Long id;
    private String name;
    private Long totalCapacity;
    private Long leftCapacity;
    
    @ManyToOne
    @JoinColumn(name = "venue_id")
    private Venue venue;
    
    private BigDecimal ticketPrice;
}

@Entity
@Table(name = "venue")
public class Venue {
    @Id
    private Long id;
    private String name;
    private String address;
    private Long totalCapacity;
}
```

#### API Endpoints
- **GET** `/api/v1/inventory/events` - Get all events with inventory
- **GET** `/api/v1/inventory/venue/{venueId}` - Get venue information
- **GET** `/api/v1/inventory/event/{eventId}` - Get event inventory details
- **PUT** `/api/v1/inventory/event/{eventId}/capacity/{capacity}` - Update event capacity

#### Configuration
```properties
spring.application.name=inventoryService
server.port=8080

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/ticketing_db
spring.datasource.username=root
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=none
```

#### Dependencies
- spring-boot-starter-web
- spring-boot-starter-data-jpa
- flyway-core
- flyway-mysql
- mysql-connector-j
- lombok
- springdoc-openapi-starter-webmvc-ui

---

### 4. Order Service

**Port**: 8082  
**Package**: `com.elz.orderservice`

The Order Service consumes booking events from Kafka, creates orders in the database, and updates inventory accordingly.

#### Key Features
- **Event Consumption**: Consumes booking events from Kafka
- **Order Creation**: Creates and persists orders in database
- **Inventory Update**: Updates event capacity after order creation
- **Asynchronous Processing**: Processes orders asynchronously via Kafka

#### Entities
```java
@Entity
@Table(name = "`order`")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private BigDecimal totalPrice;
    private Long ticketCount;
    
    @CreationTimestamp
    @Column(name = "place_at", nullable = false, updatable = false)
    private LocalDateTime placeAt;
    
    private Long customerId;
    private Long eventId;
}
```

#### Kafka Listener
```java
@KafkaListener(topics = "booking", groupId = "order-service")
public void orderEvent(BookingEvent bookingEvent) {
    // Create order
    Order order = createOrder(bookingEvent);
    orderRepository.saveAndFlush(order);
    
    // Update inventory
    inventoryServiceClient.updateInventory(order.getEventId(), order.getTicketCount());
}
```

#### Configuration
```properties
spring.application.name=orderservice
server.port=8082

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/ticketing_db
spring.datasource.username=root
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=none

# Kafka Consumer
spring.kafka.bootstrap-server=localhost:9092
spring.kafka.consumer.groupe-id=order-service
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.type.mapping=bookingEvent:com.exemple.elz.event.bookingEvent

# Inventory Service
inventory.service.url=http://localhost:8080/api/v1/inventory
```

#### Dependencies
- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-kafka
- mysql-connector-j
- lombok
- spring-kafka-test

## 🗄️ Database Schema

### Tables

#### customer
```sql
CREATE TABLE customer (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255),
    email VARCHAR(255),
    address VARCHAR(255)
);
```

#### venue
```sql
CREATE TABLE venue (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255),
    address VARCHAR(255),
    total_capacity BIGINT
);
```

#### event
```sql
CREATE TABLE event (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255),
    total_capacity BIGINT,
    left_capacity BIGINT,
    venue_id BIGINT,
    ticket_price DECIMAL(10,2),
    FOREIGN KEY (venue_id) REFERENCES venue(id)
);
```

#### `order`
```sql
CREATE TABLE `order` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    total DECIMAL(10,2),
    quantity BIGINT,
    place_at TIMESTAMP,
    customer_id BIGINT,
    event_id BIGINT
);
```

### Database Name
- **ticketing_db**

## 📨 Kafka Topics

### booking
- **Purpose**: Carries booking events from Booking Service to Order Service
- **Producer**: Booking Service
- **Consumer**: Order Service
- **Message Format**: JSON (BookingEvent)

**Message Structure**:
```json
{
  "userId": 1,
  "eventId": 1,
  "ticketCount": 2,
  "totalPrice": 100.00
}
```

## 🚀 Setup Instructions

### 1. Clone the Repository
```bash
git clone <repository-url>
cd ticket_microservice_kafka
```

### 2. Database Setup

#### Create MySQL Database
```sql
CREATE DATABASE ticketing_db;
```

#### Run Flyway Migrations (Inventory Service)
Flyway migrations will be automatically executed when the Inventory Service starts.

#### Insert Sample Data
```sql
-- Insert venues
INSERT INTO venue (id, name, address, total_capacity) VALUES 
(1, 'Grand Stadium', '123 Main Street', 10000),
(2, 'City Arena', '456 Oak Avenue', 5000);

-- Insert events
INSERT INTO event (id, name, total_capacity, left_capacity, venue_id, ticket_price) VALUES 
(1, 'Rock Concert', 1000, 1000, 1, 50.00),
(2, 'Basketball Game', 2000, 2000, 2, 75.00);

-- Insert customers
INSERT INTO customer (id, name, email, address) VALUES 
(1, 'John Doe', 'john@example.com', '789 Pine Street'),
(2, 'Jane Smith', 'jane@example.com', '321 Elm Road');
```

### 3. Kafka Setup

#### Start Kafka
```bash
# Start Zookeeper
bin/zookeeper-server-start.sh config/zookeeper.properties

# Start Kafka Server
bin/kafka-server-start.sh config/server.properties
```

#### Create Kafka Topic
```bash
bin/kafka-topics.sh --create --topic booking --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
```

### 4. Keycloak Setup (Optional)

Configure Keycloak for OAuth2 authentication:
- Create a realm: `ticketing-security-realm`
- Configure JWT issuer URI: `http://localhost:8091/realms/ticketing-security-realm`

### 5. Build Services
```bash
# Build all services
mvn clean install

# Or build individual services
cd apigateway && mvn clean install
cd ../bookingService && mvn clean install
cd ../inventoryService && mvn clean install
cd ../orderService && mvn clean install
```

## ▶️ Running the Application

### Start Services in Order

1. **Start Kafka** (if not already running)
2. **Start MySQL** (if not already running)
3. **Start Inventory Service** (Port 8080)
   ```bash
   cd inventoryService
   mvn spring-boot:run
   ```

4. **Start Order Service** (Port 8082)
   ```bash
   cd orderService
   mvn spring-boot:run
   ```

5. **Start Booking Service** (Port 8081)
   ```bash
   cd bookingService
   mvn spring-boot:run
   ```

6. **Start API Gateway** (Port 8090)
   ```bash
   cd apigateway
   mvn spring-boot:run
   ```

### Verify Services are Running

```bash
# Check Inventory Service
curl http://localhost:8080/api/v1/inventory/events

# Check Booking Service
curl http://localhost:8081/actuator/health

# Check Order Service
curl http://localhost:8082/actuator/health

# Check API Gateway
curl http://localhost:8090/actuator/health
```

## 📚 API Documentation

### Swagger UI Access

- **API Gateway**: http://localhost:8090/swagger-ui.html
- **Booking Service**: http://localhost:8081/swagger-ui.html
- **Inventory Service**: http://localhost:8080/swagger-ui.html
- **Order Service**: http://localhost:8082/swagger-ui.html

### Aggregated API Documentation

The API Gateway aggregates OpenAPI documentation from all services:
- Inventory Service Docs: http://localhost:8090/docs/inventoryservice/v3/api-docs
- Booking Service Docs: http://localhost:8090/docs/bookingservice/v3/api-docs

## 🧪 Testing

### Test Booking Flow

1. **Check Event Inventory**
   ```bash
   curl http://localhost:8090/api/v1/inventory/event/1
   ```

2. **Create a Booking**
   ```bash
   curl -X POST http://localhost:8090/api/v1/booking \
     -H "Content-Type: application/json" \
     -d '{
       "userId": 1,
       "eventId": 1,
       "ticketCount": 2
     }'
   ```

3. **Verify Order Creation**
   - Check the Order Service logs for Kafka consumption
   - Verify the order in the database

4. **Verify Inventory Update**
   ```bash
   curl http://localhost:8090/api/v1/inventory/event/1
   ```
   The `leftCapacity` should be reduced by the ticket count.

### Test Circuit Breaker

1. Stop the Booking Service
2. Send a booking request through the Gateway
3. The circuit breaker should activate and return a fallback response
4. Check the Gateway health endpoint for circuit breaker status

## 🔧 Configuration

### Environment Variables

You can override configuration properties using environment variables:

```bash
export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/ticketing_db
export SPRING_DATASOURCE_USERNAME=root
export SPRING_DATASOURCE_PASSWORD=yourpassword
export SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

### Service Ports

- API Gateway: 8090
- Booking Service: 8081
- Inventory Service: 8080
- Order Service: 8082
- Keycloak: 8091 (if used)

## 📊 Monitoring

### Actuator Endpoints

All services expose Spring Boot Actuator endpoints:

- **Health**: `/actuator/health`
- **Metrics**: `/actuator/metrics`
- **Info**: `/actuator/info`
- **Circuit Breakers**: `/actuator/circuitbreakers`

### Circuit Breaker Configuration

The API Gateway uses Resilience4J with the following configuration:
- Sliding Window Size: 8 calls
- Failure Rate Threshold: 50%
- Wait Duration in Open State: 5 seconds
- Minimum Number of Calls: 4
- Permitted Calls in Half-Open State: 2

## 🐛 Troubleshooting

### Common Issues

1. **Kafka Connection Failed**
   - Ensure Kafka is running on localhost:9092
   - Check firewall settings
   - Verify Kafka topic exists

2. **Database Connection Failed**
   - Ensure MySQL is running
   - Verify database credentials
   - Check database exists

3. **Service Not Starting**
   - Check port conflicts
   - Verify Java version compatibility
   - Review application logs

4. **Circuit Breaker Always Open**
   - Check if downstream services are running
   - Adjust circuit breaker thresholds
   - Review service health endpoints
