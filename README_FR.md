# Microservices de Billetterie avec Kafka

Un système de billetterie distribué construit avec une architecture de microservices Spring Boot, utilisant Apache Kafka pour la communication orientée événements et MySQL pour la persistance des données.

## Vue d'ensemble de l'architecture

Ce système se compose de 4 microservices :

- **API Gateway** (Port 8090) - Point d'entrée central avec routage, sécurité et disjoncteur
- **Inventory Service** (Port 8080) - Gère les événements, les lieux et l'inventaire des billets
- **Booking Service** (Port 8081) - Gère les demandes de réservation et publie des événements sur Kafka
- **Order Service** (Port 8082) - Consomme les événements de réservation de Kafka et crée des commandes

## Stack technologique

- **Java** : Java 21 (API Gateway, Booking Service, Order Service), Java 17 (Inventory Service)
- **Spring Boot** : 3.5.14 (API Gateway), 3.5.7 (Booking Service, Inventory Service), 3.4.12 (Order Service)
- **Spring Cloud** : 2025.0.2 (API Gateway)
- **Apache Kafka** : Messagerie orientée événements entre les services
- **MySQL** : Persistance des données (ticketing_db)
- **Flyway** : Migrations de base de données (Inventory Service)
- **Spring Cloud Gateway** : Routage API et équilibrage de charge
- **Resilience4J** : Implémentation du pattern disjoncteur
- **Keycloak** : Authentification et autorisation OAuth2
- **SpringDoc OpenAPI** : Documentation API avec Swagger UI
- **Lombok** : Réduction du code boilerplate

---

## Détails des microservices

### 1. API Gateway

**Port** : 8090  
**Version Java** : 21  
**Version Spring Boot** : 3.5.14  
**Version Spring Cloud** : 2025.0.2

#### Description
L'API Gateway sert de point d'entrée unique pour toutes les requêtes clients. Elle fournit le routage vers les services en aval, la sécurité via l'intégration OAuth2/Keycloak, les patterns de disjoncteur pour la tolérance aux pannes, et agrège la documentation API de tous les services.

#### Fonctionnalités clés
- **Routage** : Achemine les requêtes vers le Booking Service et l'Inventory Service
- **Sécurité** : Resource Server OAuth2 avec intégration Keycloak
- **Disjoncteur** : Implémentation Resilience4J avec seuils configurables
- **Documentation API** : Agrège Swagger UI de tous les services
- **Monitoring** : Spring Boot Actuator avec tous les endpoints exposés
- **Timeout & Retry** : Mécanismes de limiteur de temps et de retry configurables

#### Dépendances
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

# Configuration du Disjoncteur
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
- **GET** `/docs/bookingservice/v3/api-docs` → Docs API Booking Service
- **GET** `/docs/inventoryservice/v3/api-docs` → Docs API Inventory Service
- **GET** `/swagger-ui.html` → Swagger UI agrégé

#### Sécurité
Exclut l'authentification pour :
- `/swagger-ui.html`
- `/swagger-ui/**`
- `/docs/**`
- `/v3/api-docs/**`
- `/swagger-ressources/**`
- `/api-docs/**`

---

### 2. Inventory Service

**Port** : 8080  
**Version Java** : 17  
**Version Spring Boot** : 3.5.7

#### Description
L'Inventory Service gère le modèle de données central pour les événements et les lieux. Il fournit des API pour interroger les informations d'inventaire, vérifier la disponibilité et mettre à jour les capacités des événements lorsque des billets sont réservés.

#### Fonctionnalités clés
- **Gestion des événements** : Opérations CRUD pour les événements
- **Gestion des lieux** : Opérations CRUD pour les lieux
- **Suivi de l'inventaire** : Suivi en temps réel des capacités
- **Migrations de base de données** : Flyway pour le versionnement du schéma
- **Documentation API** : Intégration Swagger UI

#### Dépendances
- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Flyway Core & Flyway MySQL
- MySQL Connector
- SpringDoc OpenAPI (WebMVC UI & API)
- Lombok

#### Schéma de base de données
**Table Venue**
- `id` (Long) - Clé primaire
- `name` (String) - Nom du lieu
- `address` (String) - Adresse du lieu
- `total_capacity` (Long) - Capacité totale des sièges

**Table Event**
- `id` (Long) - Clé primaire
- `name` (String) - Nom de l'événement
- `total_capacity` (Long) - Capacité totale des billets
- `left_capacity` (Long) - Billets restants disponibles
- `venue_id` (Long) - Clé étrangère vers Venue
- `ticket_price` (BigDecimal) - Prix par billet

#### Configuration
```properties
server.port=8080
spring.application.name=inventoryService

# Base de données MySQL
spring.datasource.url=jdbc:mysql://localhost:3306/ticketing_db
spring.datasource.username=root
spring.datasource.password=
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
spring.jpa.hibernate.ddl-auto=none
```

#### Endpoints API

**GET** `/api/v1/inventory/events`
- Retourne la liste de tous les événements avec les informations d'inventaire
- Réponse : `List<EventInventoryResponse>`

**GET** `/api/v1/inventory/venue/{venueId}`
- Retourne les informations du lieu par ID
- Réponse : `VenueInventoryResponse`

**GET** `/api/v1/inventory/event/{eventId}`
- Retourne les détails d'inventaire de l'événement par ID
- Réponse : `EventInventoryResponse`

**PUT** `/api/v1/inventory/event/{eventId}/capacity/{capacity}`
- Met à jour la capacité de l'événement (réduit les billets disponibles)
- Paramètres de chemin : eventId, capacity (billets réservés)
- Réponse : 200 OK

#### Modèles de réponse

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

**Port** : 8081  
**Version Java** : 21  
**Version Spring Boot** : 3.5.7

#### Description
Le Booking Service gère les demandes de réservation de billets. Il valide l'existence de l'utilisateur, vérifie la disponibilité de l'inventaire, calcule les prix et publie des événements de réservation sur Kafka pour le traitement asynchrone des commandes.

#### Fonctionnalités clés
- **Validation de réservation** : Vérifie l'existence de l'utilisateur et la disponibilité de l'inventaire
- **Publication d'événements** : Publie des événements de réservation sur le topic Kafka
- **Intégration d'inventaire** : Appelle l'Inventory Service pour vérifier la disponibilité
- **Calcul des prix** : Calcule le prix total basé sur le nombre de billets
- **Documentation API** : Intégration Swagger UI

#### Dépendances
- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Spring Kafka
- MySQL Connector
- SpringDoc OpenAPI (WebMVC UI & API)
- Lombok
- Spring Boot DevTools

#### Schéma de base de données
**Table Customer**
- `id` (Long) - Clé primaire
- `name` (String) - Nom du client
- `email` (String) - Email du client
- `address` (String) - Adresse du client

#### Configuration
```properties
server.port=8081
spring.application.name=Booking Service

# Base de données MySQL
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

#### Endpoints API

**POST** `/api/v1/booking`
- Crée une nouvelle réservation
- Corps de la requête : `BookingRequest`
- Réponse : `BookingResponse`

#### Modèles de requête/réponse

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

**BookingEvent** (Message Kafka)
```java
{
  "userId": Long,
  "eventId": Long,
  "ticketCount": Long,
  "totalPrice": BigDecimal
}
```

#### Flux de réservation
1. Réception de la demande de réservation
2. Validation de l'existence du client dans la base de données
3. Appel de l'Inventory Service pour vérifier la disponibilité de l'événement
4. Calcul du prix total (ticketCount × ticketPrice)
5. Création du BookingEvent
6. Publication du BookingEvent sur le topic Kafka "booking"
7. Retour du BookingResponse au client

---

### 4. Order Service

**Port** : 8082  
**Version Java** : 21  
**Version Spring Boot** : 3.4.12

#### Description
L'Order Service consomme les événements de réservation de Kafka et les traite de manière asynchrone. Il crée des enregistrements de commande dans la base de données et met à jour le service d'inventaire pour refléter les billets réservés.

#### Fonctionnalités clés
- **Consommation d'événements** : Écoute le topic Kafka "booking"
- **Création de commandes** : Persiste les commandes dans la base de données
- **Mise à jour de l'inventaire** : Appelle l'Inventory Service pour réduire la capacité
- **Traitement asynchrone** : Découple la réservation de la création de commande
- **Gestion des erreurs** : Journalise les erreurs de traitement

#### Dépendances
- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Spring Kafka
- MySQL Connector
- Lombok
- Spring Kafka Test

#### Schéma de base de données
**Table Order**
- `id` (Long) - Clé primaire (auto-générée)
- `total` (BigDecimal) - Prix total de la commande
- `quantity` (Long) - Nombre de billets
- `place_at` (LocalDateTime) - Horodatage de la commande
- `customer_id` (Long) - Clé étrangère vers le client
- `event_id` (Long) - Clé étrangère vers l'événement

#### Configuration
```properties
server.port=8082
spring.application.name=orderservice

# Base de données MySQL
spring.datasource.url=jdbc:mysql://localhost:3306/ticketing_db
spring.datasource.username=root
spring.datasource.password=
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
spring.jpa.hibernate.ddl-auto=none

# Consommateur Kafka
spring.kafka.bootstrap-server=localhost:9092
spring.kafka.consumer.group-id=order-service
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.type.mapping=bookingEvent:com.exemple.elz.event.bookingEvent

# Inventory Service
inventory.service.url=http://localhost:8080/api/v1/inventory
```

#### Flux de traitement des commandes
1. Écoute du topic Kafka "booking" avec le group-id "order-service"
2. Réception du message BookingEvent
3. Création de l'entité Order à partir du BookingEvent
4. Sauvegarde de l'Order dans la base de données
5. Appel de l'Inventory Service pour mettre à jour la capacité de l'événement
6. Journalisation du traitement réussi

#### Entité Order
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

## Flux d'architecture système

```
Requête Client
     ↓
API Gateway (8090)
     ↓
     ├─→ Booking Service (8081)
     │        ↓
     │   Vérifier l'Inventaire
     │        ↓
     │   Publier sur Kafka
     │        ↓
     │   Topic Kafka : "booking"
     │        ↓
     │   Order Service (8082)
     │        ↓
     │   Créer la Commande
     │        ↓
     │   Mettre à jour l'Inventaire
     │
     └─→ Inventory Service (8080)
              ↓
         Interroger/Mettre à jour les Données
```

## Prérequis

- Java 17 ou 21
- Maven 3.6+
- MySQL 8.0+
- Apache Kafka 2.8+
- Keycloak (optionnel, pour la sécurité)

## Configuration de la base de données

Créer la base de données MySQL :
```sql
CREATE DATABASE ticketing_db;
```

Exécuter les migrations Flyway (l'Inventory Service gérera cela automatiquement).

## Démarrage des services

### Démarrer Kafka et MySQL
```bash
# Démarrer Kafka
kafka-server-start.sh config/server.properties

# Démarrer MySQL
mysql -u root -p
```

### Démarrer les services (dans l'ordre)
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

## Documentation API

Une fois tous les services démarrés, accédez à Swagger UI agrégé :
```
http://localhost:8090/swagger-ui.html
```

Documentation des services individuels :
- Inventory Service : http://localhost:8080/swagger-ui.html
- Booking Service : http://localhost:8081/swagger-ui.html

## Exemple d'utilisation

### Créer une réservation
```bash
curl -X POST http://localhost:8090/api/v1/booking \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "eventId": 1,
    "ticketCount": 2
  }'
```

### Vérifier l'inventaire d'un événement
```bash
curl http://localhost:8090/api/v1/inventory/event/1
```

### Vérifier les informations d'un lieu
```bash
curl http://localhost:8090/api/v1/inventory/venue/1
```

### Obtenir tous les événements
```bash
curl http://localhost:8090/api/v1/inventory/events
```

## Monitoring

L'API Gateway expose les endpoints Spring Boot Actuator :
```
http://localhost:8090/actuator
http://localhost:8090/actuator/health
http://localhost:8090/actuator/circuitbreakers
```

## Configuration de la sécurité

Le système utilise Keycloak pour l'authentification OAuth2. Configurez le realm Keycloak :
- Realm : `ticketing-security-realm`
- URL : `http://localhost:8091`

Mettez à jour la configuration du gateway avec les détails de votre instance Keycloak.

## Configuration du disjoncteur

L'API Gateway utilise Resilience4J avec les valeurs par défaut suivantes :
- Taille de la fenêtre glissante : 8 appels
- Seuil de taux d'échec : 50%
- Durée d'attente à l'état ouvert : 5 secondes
- Durée du timeout : 3 secondes
- Tentatives de retry maximales : 3
- Durée d'attente de retry : 2 secondes

## Dépannage

### Service indisponible
Si un service est en panne, le disjoncteur retournera :
```
Status: 503 SERVICE_UNAVAILABLE
Body: "Booking service is down"
```

### Problèmes de connexion Kafka
Assurez-vous que Kafka fonctionne sur `localhost:9092` et que le topic "booking" existe.

### Problèmes de connexion base de données
Vérifiez que MySQL fonctionne et que la base de données `ticketing_db` existe avec les identifiants corrects.

## Améliorations futures

- Ajouter un endpoint d'historique des commandes
- Implémenter le traitement des paiements
- Ajouter des notifications par email
- Implémenter le traçage distribué
- Ajouter une journalisation complète
- Implémenter le cache avec Redis
- Ajouter la découverte de services avec Eureka
- Implémenter la limitation de taux
- Ajouter des tests unitaires et d'intégration complets
