package com.doan.frontend.ui;

import com.doan.frontend.app.AppContext;
import com.doan.frontend.model.auth.AuthResponse;
import com.doan.frontend.util.UiUtils;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class LoginView extends VBox {
    public LoginView(AppContext appContext, Consumer<AuthResponse> onAuthenticated, Runnable showRegister) {
        setPadding(new Insets(40));
        setAlignment(Pos.CENTER);
        getStyleClass().add("auth-root");

        Label title = new Label("Sign in");
        title.getStyleClass().add("auth-title");

        Label subtitle = new Label("Use your username or email to continue.");
        subtitle.getStyleClass().add("auth-subtitle");

        TextField identifierField = createTextField("Username or email");

        PasswordField passwordField = createPasswordField("Password");

        Button loginButton = new Button("Login");
        loginButton.getStyleClass().add("primary-button");
        loginButton.setDefaultButton(true);
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setOnAction(event -> {
            loginButton.setDisable(true);
            appContext.authApiService().login(identifierField.getText(), passwordField.getText())
                .thenAccept(response -> UiUtils.runOnFx(() -> {
                    loginButton.setDisable(false);
                    onAuthenticated.accept(response);
                }))
                .exceptionally(throwable -> {
                    loginButton.setDisable(false);
                    UiUtils.showError("Login failed", throwable);
                    return null;
                });
        });

        Hyperlink registerLink = new Hyperlink("Create account");
        registerLink.getStyleClass().add("secondary-link");
        registerLink.setOnAction(event -> showRegister.run());

        VBox form = new VBox(14, title, subtitle, identifierField, passwordField, loginButton, registerLink);
        form.getStyleClass().add("auth-form");
        form.setMaxWidth(360);

        getChildren().add(form);
    }

    private static TextField createTextField(String promptText) {
        TextField field = new TextField();
        field.setPromptText(promptText);
        field.setMaxWidth(Double.MAX_VALUE);
        field.getStyleClass().add("form-field");
        return field;
    }

    private static PasswordField createPasswordField(String promptText) {
        PasswordField field = new PasswordField();
        field.setPromptText(promptText);
        field.setMaxWidth(Double.MAX_VALUE);
        field.getStyleClass().add("form-field");
        return field;
    }
}
