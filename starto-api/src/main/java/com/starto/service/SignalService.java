package com.starto.service;

import com.starto.dto.NearbyUserDTO;
import com.starto.dto.SignalInsightsDTO;
import com.starto.model.NearbySpace;
import com.starto.model.Signal;
import com.starto.model.SignalView;
import com.starto.model.User;
import com.starto.enums.Plan;
import com.starto.repository.ConnectionRepository;
import com.starto.repository.NearbySpaceRepository;
import com.starto.repository.SignalRepository;
import com.starto.repository.SignalViewRepository;
import com.starto.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import com.starto.service.WebSocketService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.starto.exception.SignalLimitExceededException;

@Service
@RequiredArgsConstructor
public class SignalService {

    private final SignalRepository signalRepository;
    private final SignalViewRepository signalViewRepository; 
    private final NearbySpaceRepository nearbySpaceRepository; 
    private final ConnectionRepository connectionRepository;
    private final WebSocketService webSocketService;
    private final PlanService planService;
    private final UserRepository userRepository;


    @Caching(evict = {
    @CacheEvict(value = "signalCache", key = "'activeSignals'"),
    @CacheEvict(value = "signalCache", allEntries = true)
})
    @Transactional
    public Signal createSignal(Signal signal) {
        if (signal.getExpiresAt() == null) {
            signal.setExpiresAt(OffsetDateTime.now().plusDays(7));
        }
        return signalRepository.save(signal);
    }


    public void validateSignalCreation(User user) {

    System.out.println("=== validateSignalCreation called ===");

    

    //  PLAN CONVERSION
    Plan plan = user.getPlan();

    //  COUNT USER SIGNALS
    long signalCount = signalRepository.countByUserId(user.getId());

    System.out.println("Plan: " + plan);
    System.out.println("Signal Count: " + signalCount);

    //  PLAN LIMIT CHECK
    if (!planService.canPostSignal(plan, (int) signalCount)) {
        throw new RuntimeException("Signal limit reached. Upgrade your plan.");
    }
}

@Caching(evict = {
    @CacheEvict(value = "signalCache", key = "#id"),
    @CacheEvict(value = "signalCache", key = "'activeSignals'")
})
    @Transactional
    public Signal updateSignal(UUID id, Signal updatedSignal) {
        Signal existing = getSignalById(id);

        if (updatedSignal.getType() != null) existing.setType(updatedSignal.getType());
        if (updatedSignal.getSeeking() != null) existing.setSeeking(updatedSignal.getSeeking());
        if (updatedSignal.getCategory() != null) existing.setCategory(updatedSignal.getCategory());
        if (updatedSignal.getTitle() != null) existing.setTitle(updatedSignal.getTitle());
        if (updatedSignal.getDescription() != null) existing.setDescription(updatedSignal.getDescription());
        if (updatedSignal.getStage() != null) existing.setStage(updatedSignal.getStage());
        if (updatedSignal.getCity() != null) existing.setCity(updatedSignal.getCity());
        if (updatedSignal.getState() != null) existing.setState(updatedSignal.getState());
        if (updatedSignal.getLat() != null) existing.setLat(updatedSignal.getLat());
        if (updatedSignal.getLng() != null) existing.setLng(updatedSignal.getLng());
        if (updatedSignal.getTimelineDays() != null) existing.setTimelineDays(updatedSignal.getTimelineDays());
        if (updatedSignal.getCompensation() != null) existing.setCompensation(updatedSignal.getCompensation());
        if (updatedSignal.getVisibility() != null) existing.setVisibility(updatedSignal.getVisibility());
        if (updatedSignal.getSignalStrength() != null) existing.setSignalStrength(updatedSignal.getSignalStrength());
        if (updatedSignal.getExpiresAt() != null) existing.setExpiresAt(updatedSignal.getExpiresAt());

        return signalRepository.save(existing);
    }

    @Cacheable(value = "signalCache", key = "'activeSignals'")
    public List<Signal> getActiveSignals() {
        return signalRepository.findByStatusOrderByCreatedAtDesc("open");
    }

    public List<Signal> getSignalsByCity(String city) {
        return signalRepository.findByCity(city);
    }

    public List<Signal> getSignalsByUser(UUID userId) {
        return signalRepository.findByUserId(userId);
    }

    @Cacheable(value = "signalCache", key = "#id")
    public Signal getSignalById(UUID id) {
        return signalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Signal not found"));
    }

    @CacheEvict(value = "signalCache", key = "#signalId")
   @Transactional
public void trackView(UUID signalId, UUID viewerUserId) {



    Boolean isFollower = false;

   
    UUID ownerId = getSignalById(signalId).getUserId();

    if (viewerUserId != null) {

        boolean alreadyViewed = signalViewRepository
                .existsBySignalIdAndViewerUserId(signalId, viewerUserId);

        if (alreadyViewed) return;

    
        isFollower =
            connectionRepository.existsByRequester_IdAndReceiver_IdAndStatus(viewerUserId, ownerId, "ACCEPTED")
            ||
            connectionRepository.existsByRequester_IdAndReceiver_IdAndStatus(ownerId, viewerUserId, "ACCEPTED");
    }

    signalViewRepository.save(
            SignalView.builder()
                    .signalId(signalId)
                    .viewerUserId(viewerUserId)
                    .isFollower(isFollower)
                    .build()
    );

    signalRepository.findById(signalId).ifPresent(signal -> {
        signal.setViewCount(signal.getViewCount() + 1);
        signalRepository.save(signal);
        
        webSocketService.send(
        "/topic/insights/" + signalId,
        getInsights(signalId)
    );
    });

   

}





    //  Insights
    public SignalInsightsDTO getInsights(UUID signalId) {

    Signal signal = getSignalById(signalId);

    long followerViews = signalViewRepository
            .countBySignalIdAndIsFollower(signalId, true);

    long nonFollowerViews = signalViewRepository
            .countBySignalIdAndIsFollower(signalId, false);

    List<Object[]> raw = signalViewRepository.findViewsGroupedByDay(signalId);

    Map<String, Long> dbData = raw.stream()
        .collect(Collectors.toMap(
                row -> row[0].toString(),
                row -> ((Number) row[1]).longValue()
        ));

List<Map<String, Object>> viewsOverTime = new ArrayList<>();

for (int i = 6; i >= 0; i--) {
    String date = LocalDate.now().minusDays(i).toString();

    viewsOverTime.add(Map.of(
            "date", date,
            "count", dbData.getOrDefault(date, 0L)
    ));
}



    return SignalInsightsDTO.builder() 
            .totalViews(signal.getViewCount())
            .totalResponses(signal.getResponseCount()) 
        .totalOffers(signal.getOfferCount())  
            .followerViews(followerViews)
            .nonFollowerViews(nonFollowerViews)
            .viewsOverTime(viewsOverTime)
            .build();
        }

        @Caching(evict = {
    @CacheEvict(value = "signalCache", key = "#id"),
    @CacheEvict(value = "signalCache", key = "'activeSignals'")
})
    public void deleteSignal(UUID id) {
        signalRepository.deleteById(id);
    }


    public List<Signal> getSignalsByUsername(String username) {
    return signalRepository.findByUsername(username);
}

public List<Signal> getSignalsByUsernameAndSeeking(String username, String seeking) {
    return signalRepository.findByUsernameAndSeeking(username, seeking);
}

public List<Signal> getSignalsBySeekingAndCity(String seeking, String city) {
    return signalRepository.findBySeekingAndCity(seeking, city);
}

public List<Signal> getSignalsBySeeking(String seeking) {
        return signalRepository.findBySeeking(seeking);
    }

    @Cacheable(value = "signalCache", key = "#lat + '-' + #lng + '-' + #radiusKm")
    public List<Signal> getNearbySignals(double lat, double lng, double radiusKm) {
        if (lat == 0 && lng == 0) {
            return getActiveSignals();
        }

        double latDiff = radiusKm / 111.0;
        double lngDiff = radiusKm / (111.0 * Math.cos(Math.toRadians(lat)));

        java.math.BigDecimal latMin = java.math.BigDecimal.valueOf(lat - latDiff);
        java.math.BigDecimal latMax = java.math.BigDecimal.valueOf(lat + latDiff);
        java.math.BigDecimal lngMin = java.math.BigDecimal.valueOf(lng - lngDiff);
        java.math.BigDecimal lngMax = java.math.BigDecimal.valueOf(lng + lngDiff);

        List<Signal> candidates = signalRepository.findByStatusAndLatBetweenAndLngBetween("open", latMin, latMax, lngMin, lngMax);

        return candidates.stream()
                .filter(signal -> signal.getLat() != null && signal.getLng() != null)
                .filter(signal -> haversineDistanceKm(lat, lng, signal.getLat().doubleValue(), signal.getLng().doubleValue()) <= radiusKm)
                .toList();
    }

    public List<NearbySpace> getNearbySpaces(double lat, double lng, double radiusKm) {
        double latDiff = radiusKm / 111.0;
        double lngDiff = radiusKm / (111.0 * Math.cos(Math.toRadians(lat)));

        java.math.BigDecimal latMin = java.math.BigDecimal.valueOf(lat - latDiff);
        java.math.BigDecimal latMax = java.math.BigDecimal.valueOf(lat + latDiff);
        java.math.BigDecimal lngMin = java.math.BigDecimal.valueOf(lng - lngDiff);
        java.math.BigDecimal lngMax = java.math.BigDecimal.valueOf(lng + lngDiff);

        List<NearbySpace> candidates = nearbySpaceRepository.findByLatBetweenAndLngBetween(latMin, latMax, lngMin, lngMax);

        return candidates.stream()
                .filter(space -> space.getLat() != null && space.getLng() != null)
                .filter(space -> haversineDistanceKm(lat, lng, space.getLat().doubleValue(), space.getLng().doubleValue()) <= radiusKm)
                .toList();
    }

    @Transactional
    public NearbySpace createNearbySpace(NearbySpace nearbySpace) {
        if (nearbySpace.getLat() == null || nearbySpace.getLng() == null) {
            throw new IllegalArgumentException("lat and lng are required for nearby spaces");
        }

        return nearbySpaceRepository.save(nearbySpace);
    }

    private double haversineDistanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371 * c;
    }


    public List<NearbySpace> getSpacesByUser(UUID userId) {
    return nearbySpaceRepository.findByUser_Id(userId);
}

@Cacheable(value = "signalCache", key = "#id")
public Signal getSignalByIdSafe(UUID id) {
    return signalRepository.findById(id).orElse(null);
}

@Cacheable(value = "spaceCache", key = "#id")
public NearbySpace getNearbySpaceById(UUID id) {
    return nearbySpaceRepository.findById(id).orElse(null);
}


@Caching(evict = {
        @CacheEvict(value = "signalCache", allEntries = true),
        @CacheEvict(value = "spaceCache", allEntries = true)
})
@Transactional
public Object updatePost(UUID id, User user, Signal updatedSignal) {

    //  Try Signal
    Signal existing = signalRepository.findById(id).orElse(null);

    if (existing != null) {

        //  ownership check
        if (!existing.getUserId().equals(user.getId())) {
            throw new RuntimeException("Forbidden: You don't own this signal");
        }

        //  update fields
        existing.setType(updatedSignal.getType());
        existing.setSeeking(updatedSignal.getSeeking());
        existing.setCategory(updatedSignal.getCategory());
        existing.setTitle(updatedSignal.getTitle());
        existing.setDescription(updatedSignal.getDescription());
        existing.setStage(updatedSignal.getStage());
        existing.setCity(updatedSignal.getCity());
        existing.setState(updatedSignal.getState());
        existing.setLat(updatedSignal.getLat());
        existing.setLng(updatedSignal.getLng());
        existing.setTimelineDays(updatedSignal.getTimelineDays());
        existing.setCompensation(updatedSignal.getCompensation());
        existing.setVisibility(updatedSignal.getVisibility());
        existing.setSignalStrength(updatedSignal.getSignalStrength());

        return signalRepository.save(existing);
    }

    //  Try Space
    NearbySpace space = nearbySpaceRepository.findById(id).orElse(null);

    if (space != null) {

        //  ownership check
        if (!space.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Forbidden: You don't own this space");
        }

        //  update fields (mapped)
        space.setName(updatedSignal.getTitle());
        space.setDescription(updatedSignal.getDescription());
        space.setCity(updatedSignal.getCity());
        space.setState(updatedSignal.getState());
        space.setLat(updatedSignal.getLat());
        space.setLng(updatedSignal.getLng());

        return nearbySpaceRepository.save(space);
    }

    //  not found
    throw new RuntimeException("Post not found");
}
    
@Transactional
@Caching(evict = {
        @CacheEvict(value = "signalCache", allEntries = true),
        @CacheEvict(value = "spaceCache", allEntries = true)
})
public String deletePost(UUID id, User user) {

    //  TRY SIGNAL
    Signal signal = signalRepository.findById(id).orElse(null);

    if (signal != null) {

        //  ownership check
        if (!signal.getUserId().equals(user.getId())) {
            throw new RuntimeException("Forbidden: You don't own this signal");
        }

        signalRepository.delete(signal);
        return "Signal deleted successfully";
    }

    //  TRY SPACE
    NearbySpace space = nearbySpaceRepository.findById(id).orElse(null);

    if (space != null) {

        //  ownership check 
        if (!space.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Forbidden: You don't own this space");
        }

        nearbySpaceRepository.delete(space);
        return "Space deleted successfully";
    }

    throw new RuntimeException("Post not found");
}


public List<Signal> searchSignalsByUsername(String username) {
    return signalRepository.findByUsernameLike(username)
            .stream()
            .collect(Collectors.groupingBy(
                s -> s.getUserId(),
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    list -> list.stream().limit(2).collect(Collectors.toList())
                )
            ))
            .values()
            .stream()
            .flatMap(List::stream)
            .collect(Collectors.toList());
}

public List<Signal> getSignalsByUserAndCategory(UUID userId, String category) {
    return signalRepository.findByUserIdAndCategoryIgnoreCase(userId, category);
}

public List<com.starto.dto.NearbyUserDTO> getNearbyUsers(double lat, double lng, double radiusKm) {

    double latDiff = radiusKm / 111.0;
    double lngDiff = radiusKm / (111.0 * Math.cos(Math.toRadians(lat)));

    BigDecimal latMin = BigDecimal.valueOf(lat - latDiff);
    BigDecimal latMax = BigDecimal.valueOf(lat + latDiff);
    BigDecimal lngMin = BigDecimal.valueOf(lng - lngDiff);
    BigDecimal lngMax = BigDecimal.valueOf(lng + lngDiff);

    List<User> candidates = userRepository
            .findByLatBetweenAndLngBetween(latMin, latMax, lngMin, lngMax);

    return candidates.stream()
    .filter((User u) -> u.getLat() != null && u.getLng() != null)
    .filter((User u) -> haversineDistanceKm(
            lat, lng,
            u.getLat().doubleValue(),
            u.getLng().doubleValue()
    ) <= radiusKm)
    .map((User u) -> new NearbyUserDTO(
            u.getId(),
            u.getUsername(),
            u.getLat(),
            u.getLng()
    ))
    .collect(Collectors.toList());
}
}
