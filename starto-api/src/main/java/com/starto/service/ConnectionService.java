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

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConnectionService {

    private final ConnectionRepository connectionRepository;
    private final SignalRepository signalRepository;
    private final UserRepository userRepository;

    // talent sends request to founder
    @Transactional
    public Connection sendRequest(User sender, UUID signalId, String message) {

        // check if talent already has ANY pending/accepted request to this founder
        Signal signal = signalRepository.findById(signalId)
                .orElseThrow(() -> new RuntimeException("Signal not found"));

        // block if already sent ANY request to this founder (not just this signal)
        connectionRepository.findByRequesterIdAndReceiverId(sender.getId(), signal.getUser().getId())
                .ifPresent(r -> {
                    throw new RuntimeException("You already sent a request to this founder");
                });

        Connection request = Connection.builder()
                .requester(sender)
                .receiver(signal.getUser())
                .signal(signal)
                .message(message)
                .status("pending")
                .build();

        return connectionRepository.save(request);
    }

    // founder accepts
    @Transactional
    public Connection acceptRequest(User founder, UUID connectionId) {
        Connection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new RuntimeException("Connection not found"));

        if (!connection.getReceiverId().equals(founder.getId())) {
            throw new RuntimeException("Forbidden: not your request");
        }

        connection.setStatus("accepted");
        return connectionRepository.save(connection);
    }

    // founder rejects
    @Transactional
    public Connection rejectRequest(User founder, UUID connectionId) {
        Connection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new RuntimeException("Connection not found"));

        if (!connection.getReceiverId().equals(founder.getId())) {
            throw new RuntimeException("Forbidden: not your request");
        }

        connection.setStatus("rejected");
        return connectionRepository.save(connection);
    }

    // founder sees pending requests
    public List<Connection> getPendingRequests(UUID founderId) {
        return connectionRepository.findByReceiverIdAndStatus(founderId, "pending");
    }

    // talent sees sent requests
    public List<Connection> getSentRequests(UUID talentId) {
        return connectionRepository.findByRequesterId(talentId);
    }

    // get all accepted connections for a user
    public List<Connection> getAcceptedConnections(UUID userId) {
        return connectionRepository.findAcceptedByUserId(userId);
    }

    // get whatsapp link — only works after acceptance
    public String getWhatsappLink(User requester, UUID connectionId) {
        Connection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new RuntimeException("Connection not found"));

        if (!connection.getStatus().equals("accepted")) {
            throw new RuntimeException("Connection not accepted yet");
        }

        // verify requester is part of this connection
        if (!connection.getRequesterId().equals(requester.getId()) &&
                !connection.getReceiverId().equals(requester.getId())) {
            throw new RuntimeException("Forbidden");
        }

        // get the other person's phone
        UUID otherUserId = connection.getRequesterId().equals(requester.getId())
                ? connection.getReceiverId()
                : connection.getRequesterId();

        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (otherUser.getPhone() == null || otherUser.getPhone().isBlank()) {
            throw new RuntimeException("User has no phone number registered");
        }

        String phone = otherUser.getPhone().replaceAll("[^0-9]", "");
        return "https://wa.me/" + phone;
    }

    public Connection getConnectionById(UUID connectionId) {
        return connectionRepository.findById(connectionId)
                .orElseThrow(() -> new RuntimeException("Connection not found"));
    }
}