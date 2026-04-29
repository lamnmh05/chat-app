package com.doan.frontend;

import com.doan.frontend.app.AppContext;
import com.doan.frontend.model.auth.AuthResponse;
import com.doan.frontend.ui.LoginView;
import com.doan.frontend.ui.MainView;
import com.doan.frontend.ui.RegisterView;
import com.doan.frontend.util.UiUtils;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class ChatDesktopApplication extends Application {
    private final AppContext appContext = new AppContext();
    private Stage primaryStage;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("Mini Discord Chat App");
        stage.setMinWidth(1100);
        stage.setMinHeight(720);
        stage.setScene(createLoadingScene());
        stage.show();
        attemptRestoreSession();
    }

    @Override
    public void stop() {
        appContext.clearSession();
    }

    private void attemptRestoreSession() {
        if (appContext.restoreToken().isEmpty()) {
            showLoginView();
            return;
        }

        appContext.authApiService().getCurrentUser()
            .thenAccept(user -> UiUtils.runOnFx(() -> {
                appContext.authStore().setCurrentUser(user);
                showMainView();
            }))
            .exceptionally(throwable -> {
                appContext.clearSession();
                UiUtils.runOnFx(this::showLoginView);
                return null;
            });
    }

    private void showLoginView() {
        LoginView loginView = new LoginView(appContext, this::handleAuthenticated, this::showRegisterView);
        primaryStage.setScene(createScene(loginView, 480, 520));
    }

    private void showRegisterView() {
        RegisterView registerView = new RegisterView(appContext, this::handleAuthenticated, this::showLoginView);
        primaryStage.setScene(createScene(registerView, 520, 620));
    }

    private void showMainView() {
        MainView mainView = new MainView(appContext, this::logout);
        primaryStage.setScene(createScene(mainView, 1360, 860));
    }

    private void handleAuthenticated(AuthResponse authResponse) {
        appContext.completeLogin(authResponse);
        showMainView();
    }

    private void logout() {
        appContext.clearSession();
        showLoginView();
    }

    private Scene createLoadingScene() {
        StackPane root = new StackPane(new Label("Loading session..."));
        Scene scene = createScene(root, 480, 320);
        return scene;
    }

    private Scene createScene(javafx.scene.Parent root, double width, double height) {
        Scene scene = new Scene(root, width, height);
        String css = getClass().getResource("/app.css") == null
            ? null
            : getClass().getResource("/app.css").toExternalForm();
        if (css != null) {
            scene.getStylesheets().add(css);
        }
        return scene;
    }
}
