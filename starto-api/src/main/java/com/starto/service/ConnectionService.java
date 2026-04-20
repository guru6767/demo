package com.starto.service;

import com.starto.model.Connection;
import com.starto.model.Signal;
import com.starto.model.User;
import com.starto.repository.ConnectionRepository;
import com.starto.repository.SignalRepository;
import com.starto.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConnectionService {

    private final ConnectionRepository connectionRepository;
    private final SignalRepository signalRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // SIGNAL BASED REQUEST

    @Transactional
public Connection sendRequest(User sender,
                              UUID receiverId,
                              UUID signalId,
                              String message) {

    User receiver;

    // CASE 1: SIGNAL BASED REQUEST
    if (signalId != null) {

        Signal signal = signalRepository.findById(signalId)
                .orElseThrow(() -> new RuntimeException("Signal not found"));

        receiver = signal.getUser();
    }

    // CASE 2: PROFILE BASED REQUEST
    else if (receiverId != null) {

        receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    //  invalid request
    else {
        throw new RuntimeException("receiverId or signalId required");
    }

    // prevent self request
    if (sender.getId().equals(receiver.getId())) {
        throw new RuntimeException("Cannot send request to yourself");
    }

    // duplicate check
    boolean exists = connectionRepository
            .existsByRequester_IdAndReceiver_IdAndStatus(
                    sender.getId(),
                    receiver.getId(),
                    "PENDING"
            );

    if (exists) {
        throw new RuntimeException("Request already exists");
    }

    Connection connection = Connection.builder()
            .requester(sender)
            .receiver(receiver)
            .signal(signalId != null
                    ? signalRepository.findById(signalId).orElse(null)
                    : null)
            .message(message)
            .status("PENDING")
            .build();

    return connectionRepository.save(connection);
}

   
  
    // ACCEPT REQUEST

    @Transactional
    public Connection acceptRequest(User receiver, UUID connectionId) {

        Connection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new RuntimeException("Connection not found"));

        // only receiver can accept
        if (!connection.getReceiver().getId().equals(receiver.getId())) {
            throw new RuntimeException("Forbidden");
        }

        if (!"PENDING".equalsIgnoreCase(connection.getStatus())) {
            throw new RuntimeException("Request not pending");
        }

        connection.setStatus("ACCEPTED");
        connection.setUpdatedAt(OffsetDateTime.now());

        notificationService.send(
    connection.getRequester().getId(),
    "CONNECTION_ACCEPTED",
    "Connection Accepted!",
    connection.getReceiver().getName() + " accepted your request",
    null
);

        return connectionRepository.save(connection);
    }


    // REJECT REQUEST

    @Transactional
    public Connection rejectRequest(User receiver, UUID connectionId) {

        Connection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new RuntimeException("Connection not found"));

        if (!connection.getReceiver().getId().equals(receiver.getId())) {
            throw new RuntimeException("Forbidden");
        }

        if (!"PENDING".equalsIgnoreCase(connection.getStatus())) {
            throw new RuntimeException("Request not pending");
        }

        connection.setStatus("REJECTED");
        connection.setUpdatedAt(OffsetDateTime.now());

        return connectionRepository.save(connection);
    }

    // GET REQUESTS

    public List<Connection> getPendingRequests(UUID userId) {
        return connectionRepository.findByReceiverIdAndStatus(userId, "PENDING");
    }

    public List<Connection> getSentRequests(UUID userId) {
        return connectionRepository.findByRequesterId(userId);
    }

    public List<Connection> getAcceptedConnections(UUID userId) {
        return connectionRepository.findAcceptedByUserId(userId);
    }

    // GET BY ID
    public Connection getConnectionById(UUID connectionId) {
        return connectionRepository.findById(connectionId)
                .orElseThrow(() -> new RuntimeException("Connection not found"));
    }

    // WHATSAPP LINK
    public String getWhatsappLink(User requester, UUID connectionId) {

        Connection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new RuntimeException("Connection not found"));

        if (!"ACCEPTED".equalsIgnoreCase(connection.getStatus())) {
            throw new RuntimeException("Connection not accepted yet");
        }

        if (!connection.getRequester().getId().equals(requester.getId()) &&
            !connection.getReceiver().getId().equals(requester.getId())) {
            throw new RuntimeException("Forbidden");
        }

        User otherUser = connection.getRequester().getId().equals(requester.getId())
                ? connection.getReceiver()
                : connection.getRequester();

        if (otherUser.getPhone() == null || otherUser.getPhone().isBlank()) {
            throw new RuntimeException("Phone not found");
        }

        String phone = otherUser.getPhone().replaceAll("[^0-9]", "");
        return "https://wa.me/" + phone;
    }
}