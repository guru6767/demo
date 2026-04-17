package com.starto.service;

import com.starto.dto.SignalInsightsDTO;
import com.starto.model.NearbySpace;
import com.starto.model.Signal;
import com.starto.model.SignalView;
import com.starto.model.User;
import com.starto.repository.ConnectionRepository;
import com.starto.repository.NearbySpaceRepository;
import com.starto.repository.SignalRepository;
import com.starto.repository.SignalViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.access.AccessDeniedException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import com.starto.exception.SignalLimitExceededException;

@Service
@RequiredArgsConstructor
public class SignalService {

    private final SignalRepository signalRepository;
    private final SignalViewRepository signalViewRepository;
    private final NearbySpaceRepository nearbySpaceRepository;
    private final ConnectionRepository connectionRepository;

    @Transactional
    public Signal createSignal(Signal signal) {
        if (signal.getExpiresAt() == null) {
            signal.setExpiresAt(OffsetDateTime.now().plusDays(7));
        }
        return signalRepository.save(signal);
    }

    public void validateSignalCreation(User user) {
        System.out.println("=== validateSignalCreation called ===");
        System.out.println("Role: " + user.getRole());
        System.out.println("Plan: " + user.getPlan());

        if (!user.getRole().equalsIgnoreCase("Founder")) {
            throw new RuntimeException("Only founders can create signals");
        }

        boolean isPremium = user.getPlan() != null && user.getPlan().equalsIgnoreCase("premium");
        System.out.println("isPremium: " + isPremium);

        if (!isPremium) {
            long signalCount = signalRepository.countByUserId(user.getId());
            System.out.println("signalCount: " + signalCount);
            if (signalCount >= 3) {
                throw new RuntimeException("Free plan limit reached. Upgrade to premium.");
            }
        }
    }

    @Transactional
    public Signal updateSignal(UUID id, Signal updatedSignal) {
        Signal existing = getSignalById(id);

        if (updatedSignal.getType() != null)
            existing.setType(updatedSignal.getType());
        if (updatedSignal.getSeeking() != null)
            existing.setSeeking(updatedSignal.getSeeking());
        if (updatedSignal.getCategory() != null)
            existing.setCategory(updatedSignal.getCategory());
        if (updatedSignal.getTitle() != null)
            existing.setTitle(updatedSignal.getTitle());
        if (updatedSignal.getDescription() != null)
            existing.setDescription(updatedSignal.getDescription());
        if (updatedSignal.getStage() != null)
            existing.setStage(updatedSignal.getStage());
        if (updatedSignal.getCity() != null)
            existing.setCity(updatedSignal.getCity());
        if (updatedSignal.getState() != null)
            existing.setState(updatedSignal.getState());
        if (updatedSignal.getLat() != null)
            existing.setLat(updatedSignal.getLat());
        if (updatedSignal.getLng() != null)
            existing.setLng(updatedSignal.getLng());
        if (updatedSignal.getTimelineDays() != null)
            existing.setTimelineDays(updatedSignal.getTimelineDays());
        if (updatedSignal.getCompensation() != null)
            existing.setCompensation(updatedSignal.getCompensation());
        if (updatedSignal.getVisibility() != null)
            existing.setVisibility(updatedSignal.getVisibility());
        if (updatedSignal.getSignalStrength() != null)
            existing.setSignalStrength(updatedSignal.getSignalStrength());
        if (updatedSignal.getExpiresAt() != null)
            existing.setExpiresAt(updatedSignal.getExpiresAt());

        return signalRepository.save(existing);
    }

    public List<Signal> getActiveSignals() {
        return signalRepository.findAllSignals();
    }

    public List<Signal> getSignalsByCity(String city) {
        return signalRepository.findByCity(city);
    }

    public List<Signal> getSignalsByUser(UUID userId) {
        return signalRepository.findByUserId(userId);
    }

    public Signal getSignalById(UUID id) {
        return signalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Signal not found"));
    }

    @Transactional
    public void trackView(UUID signalId, UUID viewerUserId) {

        Boolean isFollower = false;

        UUID ownerId = getSignalById(signalId).getUserId();

        if (viewerUserId != null) {

            boolean alreadyViewed = signalViewRepository
                    .existsBySignalIdAndViewerUserId(signalId, viewerUserId);

            if (alreadyViewed)
                return;

            isFollower = connectionRepository.existsByRequester_IdAndReceiver_IdAndStatus(viewerUserId, ownerId,
                    "ACCEPTED")
                    ||
                    connectionRepository.existsByRequester_IdAndReceiver_IdAndStatus(ownerId, viewerUserId, "ACCEPTED");
        }

        signalViewRepository.save(
                SignalView.builder()
                        .signalId(signalId)
                        .viewerUserId(viewerUserId)
                        .isFollower(isFollower)
                        .build());

        signalRepository.findById(signalId).ifPresent(signal -> {
            signal.setViewCount(signal.getViewCount() + 1);
            signalRepository.save(signal);
        });

    }

    // Insights
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
                        row -> ((Number) row[1]).longValue()));

        List<Map<String, Object>> viewsOverTime = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            String date = LocalDate.now().minusDays(i).toString();

            viewsOverTime.add(Map.of(
                    "date", date,
                    "count", dbData.getOrDefault(date, 0L)));
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

        List<Signal> candidates = signalRepository.findByStatusAndLatBetweenAndLngBetween("open", latMin, latMax,
                lngMin, lngMax);

        return candidates.stream()
                .filter(signal -> signal.getLat() != null && signal.getLng() != null)
                .filter(signal -> haversineDistanceKm(lat, lng, signal.getLat().doubleValue(),
                        signal.getLng().doubleValue()) <= radiusKm)
                .toList();
    }

    public List<NearbySpace> getNearbySpaces(double lat, double lng, double radiusKm) {
        double latDiff = radiusKm / 111.0;
        double lngDiff = radiusKm / (111.0 * Math.cos(Math.toRadians(lat)));

        java.math.BigDecimal latMin = java.math.BigDecimal.valueOf(lat - latDiff);
        java.math.BigDecimal latMax = java.math.BigDecimal.valueOf(lat + latDiff);
        java.math.BigDecimal lngMin = java.math.BigDecimal.valueOf(lng - lngDiff);
        java.math.BigDecimal lngMax = java.math.BigDecimal.valueOf(lng + lngDiff);

        List<NearbySpace> candidates = nearbySpaceRepository.findByLatBetweenAndLngBetween(latMin, latMax, lngMin,
                lngMax);

        return candidates.stream()
                .filter(space -> space.getLat() != null && space.getLng() != null)
                .filter(space -> haversineDistanceKm(lat, lng, space.getLat().doubleValue(),
                        space.getLng().doubleValue()) <= radiusKm)
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

}