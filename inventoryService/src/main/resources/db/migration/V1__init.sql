CREATE TABLE venue(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR (255) NOT NULL,
    address VARCHAR (255) NOT NULL,
    total_capacity BIGINT (255) NOT NULL
   );

CREATE TABLE event(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR (255) NOT NULL,
    venue_id BIGINT (255) NOT NULL,
    total_capacity BIGINT (255) NOT NULL,
    left_capacity BIGINT (255) NOT NULL,
    CONSTRAINT fk_event_venue FOREIGN KEY (venue_id) REFERENCES venue(id) ON DELETE CASCADE
  );