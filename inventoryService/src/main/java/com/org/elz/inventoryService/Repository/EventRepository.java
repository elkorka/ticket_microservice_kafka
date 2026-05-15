package com.org.elz.inventoryService.Repository;

import com.org.elz.inventoryService.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository <Event, Long>{
}
