package com.elz.bookingService.service;


import com.elz.bookingService.client.InventoryServiceClient;
import com.elz.bookingService.entity.Customer;
import com.elz.bookingService.event.BookingEvent;
import com.elz.bookingService.repository.CustomerRepository;
import com.elz.bookingService.request.BookingRequest;
import com.elz.bookingService.response.BookingResponse;
import com.elz.bookingService.response.InventoryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {
    private final CustomerRepository customerRepository;
    private final InventoryServiceClient inventoryServiceClient;
    private final KafkaTemplate<String, BookingEvent> kafkaTemplate;

    public BookingResponse createBooking(final BookingRequest request){
        // check if user exists
        final Customer customer = customerRepository.findById(request.getUserId()).orElse(null);
        if (customer == null){
            throw new RuntimeException("User not found");
        }
        // check if there is enough inventory
        final InventoryResponse inventoryResponse = inventoryServiceClient.getInventory(request.getEventId());
        System.out.println("Inventory response: " + inventoryResponse);
        if (inventoryResponse.getCapacity() < request.getTicketCount()){
            throw new RuntimeException("Not enough inventory");
        }
        // create booking
        final BookingEvent bookingEvent = createBookingEvent(request,customer,inventoryResponse);
        // send booking to Order service on a kafka topic
        kafkaTemplate.send("booking",bookingEvent);
        log.info("Booking sent to kafka: {} ", bookingEvent);
        return BookingResponse.builder()
                .userId(bookingEvent.getUserId())
                .eventId(bookingEvent.getEventId())
                .ticketCount((bookingEvent.getTicketCount()))
                .totalPrice(bookingEvent.getTotalPrice())
                .build();
    }

    private BookingEvent createBookingEvent(BookingRequest request, Customer customer, InventoryResponse inventoryResponse) {
        return BookingEvent.builder()
                .userId(customer.getId())
                .eventId(request.getEventId())
                .ticketCount(request.getTicketCount())
                .totalPrice(inventoryResponse.getTicketPrice().multiply(BigDecimal.valueOf(request.getTicketCount())) )
                .build();
    }
}
