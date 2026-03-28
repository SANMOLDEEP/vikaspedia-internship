package backend.service;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UserSessionService {

    private final Map<String, Set<String>> documentUsers = new HashMap<>();

    public void addUser(String documentId, String userId) {
        System.out.println("Adding user " + userId + " to document " + documentId);
        documentUsers.computeIfAbsent(documentId, k -> new HashSet<>()).add(userId);
    }

    public void removeUser(String documentId, String userId) {
        System.out.println("Removing user " + userId + " from document " + documentId);
        if (documentUsers.containsKey(documentId)) {
            documentUsers.get(documentId).remove(userId);
            // Remove empty document entries
            if (documentUsers.get(documentId).isEmpty()) {
                documentUsers.remove(documentId);
            }
        }
    }

    public Set<String> getUsers(String documentId) {
        Set<String> users = documentUsers.getOrDefault(documentId, new HashSet<>());
        System.out.println("Current users in " + documentId + ": " + users);
        return users;
    }
}
