package com.doan.frontend.store;

import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

public class PresenceStore {
    private final ObservableSet<String> onlineUsers = FXCollections.observableSet();

    public ObservableSet<String> getOnlineUsers() {
        return onlineUsers;
    }

    public void markOnline(String userId) {
        onlineUsers.add(userId);
    }

    public void markOffline(String userId) {
        onlineUsers.remove(userId);
    }

    public boolean isOnline(String userId) {
        return onlineUsers.contains(userId);
    }

    public void clear() {
        onlineUsers.clear();
    }
}
