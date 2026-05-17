package com.org.elz.inventoryService.service;

import com.org.elz.inventoryService.Repository.EventRepository;
import com.org.elz.inventoryService.Repository.VenueRepository;
import com.org.elz.inventoryService.entity.Event;
import com.org.elz.inventoryService.entity.Venue;
import com.org.elz.inventoryService.response.EventInventoryResponse;
import com.org.elz.inventoryService.response.VenueInventoryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
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

    public void updateEventCapacity(final Long eventId, final Long ticketsBooked){
        final Event event = eventRepository.findById(eventId).orElse(null);
        event.setLeftCapacity(event.getLeftCapacity() - ticketsBooked);
        eventRepository.saveAndFlush(event);
        log.info("updating event capacity of event:id {} with tickets booked: {}",eventId,ticketsBooked);

    }
}
