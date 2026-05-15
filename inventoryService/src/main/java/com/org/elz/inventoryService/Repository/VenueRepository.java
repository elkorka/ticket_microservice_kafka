package com.org.elz.inventoryService.Repository;

import com.org.elz.inventoryService.entity.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VenueRepository extends JpaRepository<Venue,Long> {
}
