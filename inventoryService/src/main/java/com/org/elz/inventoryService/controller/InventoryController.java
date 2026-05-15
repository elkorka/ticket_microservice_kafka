package com.org.elz.inventoryService.controller;

import com.org.elz.inventoryService.response.EventInventoryResponse;
import com.org.elz.inventoryService.response.VenueInventoryResponse;
import com.org.elz.inventoryService.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
public class InventoryController {
    private final InventoryService inventoryService;

    @GetMapping("/inventory/events")
    public @ResponseBody List<EventInventoryResponse> inventoryGetAllEvents(){
        return inventoryService.getAllEvents();
    }

    @GetMapping("/inventory/venue/{venueID}")
    public @ResponseBody VenueInventoryResponse inventoryByVenueId(@PathVariable("venueId") Long venueId){
        return inventoryService.getVenueInformation(venueId);
    }

    @GetMapping("/inventory/event/{eventId")
    public @ResponseBody EventInventoryResponse inventoryForEvent(@PathVariable("eventId") Long eventId){
        return inventoryService.getEventInventory(eventId);
    }
}
