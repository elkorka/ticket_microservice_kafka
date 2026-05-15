package com.org.elz.inventoryService.service;

import com.org.elz.inventoryService.Repository.EventRepository;
import com.org.elz.inventoryService.Repository.VenueRepository;
import com.org.elz.inventoryService.entity.Event;
import com.org.elz.inventoryService.entity.Venue;
import com.org.elz.inventoryService.response.EventInventoryResponse;
import com.org.elz.inventoryService.response.VenueInventoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private EventRepository eventRepository;
    private VenueRepository venueRepository;

    public List<EventInventoryResponse> getAllEvents(){
        final List<Event> events = eventRepository.findAll();
        return events.stream().map(
                event ->EventInventoryResponse.builder()
                        .event(event.getName())
                        .capacity(event.getLeftCapacity())
                        .venue(event.getVenue())
                        .build()).collect(Collectors.toList());


    }

    public VenueInventoryResponse getVenueInformation(final Long venueId) {
        final Venue venue = venueRepository.findById(venueId).orElse(null);

        return VenueInventoryResponse.builder()
                .venueId(venue.getId())
                .venueName(venue.getName())
                .totalCapacity(venue.getTotalCapacity())
                .build();
    }

    public EventInventoryResponse getEventInventory(final Long eventId){
        final Event event = eventRepository.findById(eventId).orElse(null);

        return EventInventoryResponse.builder()
                .event(event.getName())
                .capacity(event.getLeftCapacity())
                .venue(event.getVenue())
                .ticketPrice(event.getTicketPrice())
                .eventId(event.getId())
                .build();
    }
}
