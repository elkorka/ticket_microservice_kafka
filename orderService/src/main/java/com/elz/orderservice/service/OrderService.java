package com.elz.orderservice.service;


import com.elz.orderservice.client.InventoryServiceClient;
import com.elz.orderservice.repository.OrderRepository;
import com.exemple.elz.bookinservice.event.BookingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import com.elz.orderservice.entity.Order;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {
    private OrderRepository orderRepository;
    private InventoryServiceClient inventoryServiceClient;

    @KafkaListener(topics = "booking", groupId = "order-service")
    public void orderEvent(BookingEvent bookingEvent){
        log.info("Received order event: {}",bookingEvent);
        
        // Create Order object for DB

        Order order=createOrder(bookingEvent);
        orderRepository.saveAndFlush(order);
        // Update Inventory
        inventoryServiceClient.updateInventory(order.getEventId(), order.getTicketCount());
        log.info("Inventory updated for event: {}, less tickets: {}",order.getEventId(),order.getTicketCount());
    }

    private Order createOrder(BookingEvent bookingEvent){
        return Order.builder()
                .customerId(bookingEvent.getUserId())
                .eventId(bookingEvent.getEventId())
                .ticketCount(bookingEvent.getTicketCount())
                .totalPrice(bookingEvent.getTotalPrice())
                .build();
    }

}
