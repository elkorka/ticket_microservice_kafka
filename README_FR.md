# Architecture de Microservices de Réservation de Billets

Un système de réservation de billets distribué construit avec une architecture de microservices Spring Boot, utilisant Apache Kafka pour la communication événementielle et Spring Cloud Gateway pour le routage d'API.

## 📋 Table des Matières

- [Vue d'ensemble de l'Architecture](#vue-densemble-de-larchitecture)
- [Pile Technologique](#pile-technologique)
- [Prérequis](#prérequis)
- [Description des Microservices](#description-des-microservices)
  - [API Gateway](#1-api-gateway)
  - [Service de Réservation](#2-service-de-réservation)
  - [Service d'Inventaire](#3-service-dinventaire)
  - [Service de Commande](#4-service-de-commande)
- [Schéma de Base de Données](#schéma-de-base-de-données)
- [Sujets Kafka](#sujets-kafka)
- [Instructions de Configuration](#instructions-de-configuration)
- [Exécution de l'Application](#exécution-de-lapplication)
- [Documentation de l'API](#documentation-de-lapi)
- [Tests](#tests)

## 🏗️ Vue d'ensemble de l'Architecture

Ce système de réservation suit une architecture de microservices avec les composants suivants :

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
       ├───► Service de Réservation (Port 8081)
       │        │
       │        ├──► Sujet Kafka : "booking"
       │        │
       │        └──► Service d'Inventaire (Port 8080)
       │
       └───► Service d'Inventaire (Port 8080)
                │
                └──► Service de Commande (Port 8082)
                         │
                         └──► Consommateur Kafka : "booking"
```

### Flux de Communication

1. **Client** demande une réservation via l'API Gateway
2. **API Gateway** route la requête vers le Service de Réservation
3. **Service de Réservation** valide l'existence de l'utilisateur et vérifie la disponibilité de l'inventaire
4. **Service de Réservation** publie un événement de réservation sur Kafka
5. **Service de Commande** consomme l'événement de réservation depuis Kafka
6. **Service de Commande** crée une commande et met à jour l'inventaire

## 🛠️ Pile Technologique

### Technologies Communes
- **Java** : 17 (Service d'Inventaire), 21 (API Gateway, Service de Réservation, Service de Commande)
- **Spring Boot** : 3.4.12 - 3.5.14
- **Spring Cloud** : 2025.0.2
- **MySQL** : Base de données
- **Lombok** : Génération de code
- **SpringDoc OpenAPI** : Documentation de l'API (v2.8.16)

### Technologies Spécifiques aux Services

#### API Gateway
- Spring Cloud Gateway Server MVC
- Spring Cloud Circuit Breaker (Resilience4J)
- Spring Boot OAuth2 Resource Server
- Intégration Keycloak

#### Service de Réservation
- Spring Kafka (Producteur)
- Spring Data JPA
- MySQL Connector

#### Service d'Inventaire
- Spring Data JPA
- Flyway (Migration de Base de Données)
- MySQL Connector

#### Service de Commande
- Spring Kafka (Consommateur)
- Spring Data JPA
- MySQL Connector

## 📦 Prérequis

Avant d'exécuter cette application, assurez-vous d'avoir installé les éléments suivants :

- **Java JDK** : 17 ou 21
- **Maven** : 3.6+
- **MySQL** : 8.0+
- **Apache Kafka** : 2.8+
- **Keycloak** : 20+ (pour l'authentification)
- **Git** : Pour cloner le dépôt

## 🔍 Description des Microservices

### 1. API Gateway

**Port** : 8090  
**Package** : `com.elz.apigateway`

L'API Gateway sert de point d'entrée unique pour toutes les requêtes client, fournissant le routage, l'équilibrage de charge, la sécurité et la résilience.

#### Fonctionnalités Clés
- **Routage des Requêtes** : Route les requêtes vers les microservices appropriés
- **Circuit Breaker** : Implémente Resilience4J pour la tolérance aux pannes
- **Sécurité OAuth2** : Intègre Keycloak pour l'authentification
- **Agrégation de Documentation API** : Agrège les docs OpenAPI de tous les services
- **Endpoints Actuator** : Vérifications de santé et monitoring

#### Configuration
```properties
spring.application.name=apigateway
server.port=8090

# Configuration Keycloak
keycloak.auth.jwk-set-uri=http://localhost:8091/realms/ticketing-security-realm/protocol/openid-connect/certs
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8091/realms/ticketing-security-realm

# Configuration du Circuit Breaker
resilience4j.circuitbreaker.configs.default.slidingWindowSize=8
resilience4j.circuitbreaker.configs.default.failureRateThreshold=50
resilience4j.circuitbreaker.configs.default.waitDurationInOpenState=5s
```

#### Routes
- **POST** `/api/v1/booking` → Service de Réservation (localhost:8081)
- **GET** `/api/v1/inventory/venue/{venueId}` → Service d'Inventaire (localhost:8080)
- **GET** `/api/v1/inventory/event/{eventId}` → Service d'Inventaire (localhost:8080)
- **GET** `/docs/bookingservice/v3/api-docs` → Docs API Service de Réservation
- **GET** `/docs/inventoryservice/v3/api-docs` → Docs API Service d'Inventaire

#### Dépendances
- spring-cloud-starter-gateway-server-webmvc
- spring-cloud-starter-circuitbreaker-resilience4j
- spring-boot-starter-oauth2-resource-server
- springdoc-openapi-starter-webmvc-ui
- spring-boot-starter-actuator

---

### 2. Service de Réservation

**Port** : 8081  
**Package** : `com.elz.bookingService`

Le Service de Réservation gère les demandes de réservation de billets, valide les informations utilisateur, vérifie la disponibilité de l'inventaire et publie des événements de réservation sur Kafka.

#### Fonctionnalités Clés
- **Création de Réservation** : Crée de nouvelles demandes de réservation
- **Validation Utilisateur** : Vérifie l'existence du client dans la base de données
- **Vérification d'Inventaire** : Valide la disponibilité des billets avant réservation
- **Publication d'Événements** : Publie des événements de réservation sur le sujet Kafka
- **API REST** : Fournit des endpoints RESTful pour les opérations de réservation

#### Entités
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

#### Événements
```java
public class BookingEvent {
    private Long userId;
    private Long eventId;
    private Long ticketCount;
    private BigDecimal totalPrice;
}
```

#### Endpoints API
- **POST** `/api/v1/booking` - Créer une nouvelle réservation

**Corps de la Requête** :
```json
{
  "userId": 1,
  "eventId": 1,
  "ticketCount": 2
}
```

**Réponse** :
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

# Base de Données
spring.datasource.url=jdbc:mysql://localhost:3306/ticketing_db
spring.datasource.username=root
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=none

# Kafka
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.template.default-topic=booking
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer

# Service d'Inventaire
inventory.service.url=http://localhost:8000/api/v1/inventory
```

#### Dépendances
- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-kafka
- mysql-connector-j
- lombok
- springdoc-openapi-starter-webmvc-ui

---

### 3. Service d'Inventaire

**Port** : 8080  
**Package** : `com.org.elz.inventoryService`

Le Service d'Inventaire gère les informations sur les événements et les lieux, suit la disponibilité des billets et fournit les données d'inventaire aux autres services.

#### Fonctionnalités Clés
- **Gestion des Événements** : Gère les informations et la capacité des événements
- **Gestion des Lieux** : Gère les détails des lieux
- **Suivi de l'Inventaire** : Suit les billets disponibles pour les événements
- **Mises à jour de Capacité** : Met à jour la capacité des événements après les réservations
- **Migration de Base de Données** : Utilise Flyway pour la gestion du schéma

#### Entités
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

#### Endpoints API
- **GET** `/api/v1/inventory/events` - Obtenir tous les événements avec inventaire
- **GET** `/api/v1/inventory/venue/{venueId}` - Obtenir les informations du lieu
- **GET** `/api/v1/inventory/event/{eventId}` - Obtenir les détails d'inventaire de l'événement
- **PUT** `/api/v1/inventory/event/{eventId}/capacity/{capacity}` - Mettre à jour la capacité de l'événement

#### Configuration
```properties
spring.application.name=inventoryService
server.port=8080

# Base de Données
spring.datasource.url=jdbc:mysql://localhost:3306/ticketing_db
spring.datasource.username=root
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=none
```

#### Dépendances
- spring-boot-starter-web
- spring-boot-starter-data-jpa
- flyway-core
- flyway-mysql
- mysql-connector-j
- lombok
- springdoc-openapi-starter-webmvc-ui

---

### 4. Service de Commande

**Port** : 8082  
**Package** : `com.elz.orderservice`

Le Service de Commande consomme les événements de réservation depuis Kafka, crée des commandes dans la base de données et met à jour l'inventaire en conséquence.

#### Fonctionnalités Clés
- **Consommation d'Événements** : Consomme les événements de réservation depuis Kafka
- **Création de Commande** : Crée et persiste les commandes dans la base de données
- **Mise à jour de l'Inventaire** : Met à jour la capacité de l'événement après création de commande
- **Traitement Asynchrone** : Traite les commandes de manière asynchrone via Kafka

#### Entités
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

#### Écouteur Kafka
```java
@KafkaListener(topics = "booking", groupId = "order-service")
public void orderEvent(BookingEvent bookingEvent) {
    // Créer la commande
    Order order = createOrder(bookingEvent);
    orderRepository.saveAndFlush(order);
    
    // Mettre à jour l'inventaire
    inventoryServiceClient.updateInventory(order.getEventId(), order.getTicketCount());
}
```

#### Configuration
```properties
spring.application.name=orderservice
server.port=8082

# Base de Données
spring.datasource.url=jdbc:mysql://localhost:3306/ticketing_db
spring.datasource.username=root
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=none

# Consommateur Kafka
spring.kafka.bootstrap-server=localhost:9092
spring.kafka.consumer.groupe-id=order-service
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.type.mapping=bookingEvent:com.exemple.elz.event.bookingEvent

# Service d'Inventaire
inventory.service.url=http://localhost:8080/api/v1/inventory
```

#### Dépendances
- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-kafka
- mysql-connector-j
- lombok
- spring-kafka-test

## 🗄️ Schéma de Base de Données

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

### Nom de la Base de Données
- **ticketing_db**

## 📨 Sujets Kafka

### booking
- **Objectif** : Transporte les événements de réservation du Service de Réservation vers le Service de Commande
- **Producteur** : Service de Réservation
- **Consommateur** : Service de Commande
- **Format de Message** : JSON (BookingEvent)

**Structure du Message** :
```json
{
  "userId": 1,
  "eventId": 1,
  "ticketCount": 2,
  "totalPrice": 100.00
}
```

## 🚀 Instructions de Configuration

### 1. Cloner le Dépôt
```bash
git clone <repository-url>
cd ticket_microservice_kafka
```

### 2. Configuration de la Base de Données

#### Créer la Base de Données MySQL
```sql
CREATE DATABASE ticketing_db;
```

#### Exécuter les Migrations Flyway (Service d'Inventaire)
Les migrations Flyway seront exécutées automatiquement lors du démarrage du Service d'Inventaire.

#### Insérer des Données d'Exemple
```sql
-- Insérer des lieux
INSERT INTO venue (id, name, address, total_capacity) VALUES 
(1, 'Grand Stade', '123 Rue Principale', 10000),
(2, 'Aréna de la Ville', '456 Avenue du Chêne', 5000);

-- Insérer des événements
INSERT INTO event (id, name, total_capacity, left_capacity, venue_id, ticket_price) VALUES 
(1, 'Concert de Rock', 1000, 1000, 1, 50.00),
(2, 'Match de Basket', 2000, 2000, 2, 75.00);

-- Insérer des clients
INSERT INTO customer (id, name, email, address) VALUES 
(1, 'Jean Dupont', 'jean@example.com', '789 Rue du Pin'),
(2, 'Marie Martin', 'marie@example.com', '321 Avenue de l'Orme');
```

### 3. Configuration de Kafka

#### Démarrer Kafka
```bash
# Démarrer Zookeeper
bin/zookeeper-server-start.sh config/zookeeper.properties

# Démarrer le Serveur Kafka
bin/kafka-server-start.sh config/server.properties
```

#### Créer le Sujet Kafka
```bash
bin/kafka-topics.sh --create --topic booking --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
```

### 4. Configuration de Keycloak (Optionnel)

Configurer Keycloak pour l'authentification OAuth2 :
- Créer un realm : `ticketing-security-realm`
- Configurer l'URI de l'émetteur JWT : `http://localhost:8091/realms/ticketing-security-realm`

### 5. Construire les Services
```bash
# Construire tous les services
mvn clean install

# Ou construire les services individuellement
cd apigateway && mvn clean install
cd ../bookingService && mvn clean install
cd ../inventoryService && mvn clean install
cd ../orderService && mvn clean install
```

## ▶️ Exécution de l'Application

### Démarrer les Services dans l'Ordre

1. **Démarrer Kafka** (si pas déjà en cours)
2. **Démarrer MySQL** (si pas déjà en cours)
3. **Démarrer le Service d'Inventaire** (Port 8080)
   ```bash
   cd inventoryService
   mvn spring-boot:run
   ```

4. **Démarrer le Service de Commande** (Port 8082)
   ```bash
   cd orderService
   mvn spring-boot:run
   ```

5. **Démarrer le Service de Réservation** (Port 8081)
   ```bash
   cd bookingService
   mvn spring-boot:run
   ```

6. **Démarrer l'API Gateway** (Port 8090)
   ```bash
   cd apigateway
   mvn spring-boot:run
   ```

### Vérifier que les Services sont en Cours

```bash
# Vérifier le Service d'Inventaire
curl http://localhost:8080/api/v1/inventory/events

# Vérifier le Service de Réservation
curl http://localhost:8081/actuator/health

# Vérifier le Service de Commande
curl http://localhost:8082/actuator/health

# Vérifier l'API Gateway
curl http://localhost:8090/actuator/health
```

## 📚 Documentation de l'API

### Accès à l'Interface Swagger

- **API Gateway** : http://localhost:8090/swagger-ui.html
- **Service de Réservation** : http://localhost:8081/swagger-ui.html
- **Service d'Inventaire** : http://localhost:8080/swagger-ui.html
- **Service de Commande** : http://localhost:8082/swagger-ui.html

### Documentation API Agrégée

L'API Gateway agrège la documentation OpenAPI de tous les services :
- Docs Service d'Inventaire : http://localhost:8090/docs/inventoryservice/v3/api-docs
- Docs Service de Réservation : http://localhost:8090/docs/bookingservice/v3/api-docs

## 🧪 Tests

### Tester le Flux de Réservation

1. **Vérifier l'Inventaire de l'Événement**
   ```bash
   curl http://localhost:8090/api/v1/inventory/event/1
   ```

2. **Créer une Réservation**
   ```bash
   curl -X POST http://localhost:8090/api/v1/booking \
     -H "Content-Type: application/json" \
     -d '{
       "userId": 1,
       "eventId": 1,
       "ticketCount": 2
     }'
   ```

3. **Vérifier la Création de Commande**
   - Vérifier les logs du Service de Commande pour la consommation Kafka
   - Vérifier la commande dans la base de données

4. **Vérifier la Mise à jour de l'Inventaire**
   ```bash
   curl http://localhost:8090/api/v1/inventory/event/1
   ```
   La `leftCapacity` devrait être réduite du nombre de billets.

### Tester le Circuit Breaker

1. Arrêter le Service de Réservation
2. Envoyer une demande de réservation via la Gateway
3. Le circuit breaker devrait s'activer et retourner une réponse de repli
4. Vérifier l'endpoint de santé de la Gateway pour le statut du circuit breaker

## 🔧 Configuration

### Variables d'Environnement

Vous pouvez remplacer les propriétés de configuration en utilisant des variables d'environnement :

```bash
export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/ticketing_db
export SPRING_DATASOURCE_USERNAME=root
export SPRING_DATASOURCE_PASSWORD=votremotdepasse
export SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

### Ports des Services

- API Gateway : 8090
- Service de Réservation : 8081
- Service d'Inventaire : 8080
- Service de Commande : 8082
- Keycloak : 8091 (si utilisé)

## 📊 Monitoring

### Endpoints Actuator

Tous les services exposent les endpoints Spring Boot Actuator :

- **Santé** : `/actuator/health`
- **Métriques** : `/actuator/metrics`
- **Info** : `/actuator/info`
- **Circuit Breakers** : `/actuator/circuitbreakers`

### Configuration du Circuit Breaker

L'API Gateway utilise Resilience4J avec la configuration suivante :
- Taille de la Fenêtre Glissante : 8 appels
- Seuil de Taux d'Échec : 50%
- Durée d'Attente en État Ouvert : 5 secondes
- Nombre Minimum d'Appels : 4
- Nombre d'Appels Permis en État Semi-Ouvert : 2

## 🐛 Dépannage

### Problèmes Courants

1. **Échec de Connexion Kafka**
   - Assurez-vous que Kafka fonctionne sur localhost:9092
   - Vérifiez les paramètres du pare-feu
   - Vérifiez que le sujet Kafka existe

2. **Échec de Connexion à la Base de Données**
   - Assurez-vous que MySQL fonctionne
   - Vérifiez les identifiants de la base de données
   - Vérifiez que la base de données existe

3. **Service Ne Démarre Pas**
   - Vérifiez les conflits de ports
   - Vérifiez la compatibilité de la version Java
   - Consultez les logs de l'application

4. **Circuit Breaker Toujours Ouvert**
   - Vérifiez si les services en aval fonctionnent
   - Ajustez les seuils du circuit breaker
   - Consultez les endpoints de santé des services

