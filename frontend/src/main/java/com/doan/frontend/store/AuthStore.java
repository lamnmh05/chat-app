package com.doan.frontend.store;

import com.doan.frontend.model.user.UserResponse;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class AuthStore {
    private final ObjectProperty<UserResponse> currentUser = new SimpleObjectProperty<>();
    private final StringProperty accessToken = new SimpleStringProperty();
    private final BooleanProperty loggedIn = new SimpleBooleanProperty(false);

    public ObjectProperty<UserResponse> currentUserProperty() {
        return currentUser;
    }

    public StringProperty accessTokenProperty() {
        return accessToken;
    }

    public BooleanProperty loggedInProperty() {
        return loggedIn;
    }

    public UserResponse getCurrentUser() {
        return currentUser.get();
    }

    public void setCurrentUser(UserResponse user) {
        currentUser.set(user);
        loggedIn.set(user != null && accessToken.get() != null && !accessToken.get().isBlank());
    }

    public String getAccessToken() {
        return accessToken.get();
    }

    public void setAccessToken(String token) {
        accessToken.set(token);
        loggedIn.set(token != null && !token.isBlank() && currentUser.get() != null);
    }

    public boolean isLoggedIn() {
        return loggedIn.get();
    }

    public void clear() {
        currentUser.set(null);
        accessToken.set(null);
        loggedIn.set(false);
    }
}
