package com.starto.service.explore;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.starto.service.external.GoogleMapsClient;



@Service
@RequiredArgsConstructor
public class LocationService {

    private final GoogleMapsClient mapsClient;

    public String getInsights(String location) {
        return mapsClient.fetchInsights(location);
    }
}