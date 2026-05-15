package com.elz.bookingService.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class VenueResponse {
    private Long id;
    private String name;
    private String address;
    private Long totalCapacity;
}
