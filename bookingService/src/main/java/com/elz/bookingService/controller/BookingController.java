package com.elz.bookingService.controller;


import com.elz.bookingService.request.BookingRequest;
import com.elz.bookingService.response.BookingResponse;
import com.elz.bookingService.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;

    @PostMapping(consumes = "application/json" ,produces = "application/json" ,path = "/booking")
    public BookingResponse createBooking(@RequestBody BookingRequest request){
        return bookingService.createBooking(request);
    }
}
