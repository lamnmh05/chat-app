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

public class RegisterView extends VBox {
    public RegisterView(AppContext appContext, Consumer<AuthResponse> onAuthenticated, Runnable showLogin) {
        setPadding(new Insets(40));
        setAlignment(Pos.CENTER);
        getStyleClass().add("auth-root");

        Label title = new Label("Create account");
        title.getStyleClass().add("auth-title");

        Label subtitle = new Label("Create a local account for the desktop client.");
        subtitle.getStyleClass().add("auth-subtitle");

        TextField usernameField = createTextField("Username");
        TextField emailField = createTextField("Email");
        TextField displayNameField = createTextField("Display name");
        PasswordField passwordField = createPasswordField("Password");

        Button registerButton = new Button("Register");
        registerButton.getStyleClass().add("primary-button");
        registerButton.setDefaultButton(true);
        registerButton.setMaxWidth(Double.MAX_VALUE);
        registerButton.setOnAction(event -> {
            registerButton.setDisable(true);
            appContext.authApiService()
                .register(
                    usernameField.getText(),
                    emailField.getText(),
                    displayNameField.getText(),
                    passwordField.getText()
                )
                .thenAccept(response -> UiUtils.runOnFx(() -> {
                    registerButton.setDisable(false);
                    onAuthenticated.accept(response);
                }))
                .exceptionally(throwable -> {
                    registerButton.setDisable(false);
                    UiUtils.showError("Register failed", throwable);
                    return null;
                });
        });

        Hyperlink loginLink = new Hyperlink("Back to login");
        loginLink.getStyleClass().add("secondary-link");
        loginLink.setOnAction(event -> showLogin.run());

        VBox form = new VBox(
            14,
            title,
            subtitle,
            usernameField,
            emailField,
            displayNameField,
            passwordField,
            registerButton,
            loginLink
        );
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
