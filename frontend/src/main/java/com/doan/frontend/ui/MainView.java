package com.doan.frontend.ui;

import com.doan.frontend.app.AppContext;
import com.doan.frontend.model.channel.ChannelResponse;
import com.doan.frontend.model.file.FileUploadResponse;
import com.doan.frontend.model.guild.GuildMemberResponse;
import com.doan.frontend.model.guild.GuildResponse;
import com.doan.frontend.model.message.MessageCreateRequest;
import com.doan.frontend.model.message.MessageResponse;
import com.doan.frontend.model.message.MessageType;
import com.doan.frontend.model.user.UserResponse;
import com.doan.frontend.model.user.UserStatus;
import com.doan.frontend.model.ws.ChannelSocketEventResponse;
import com.doan.frontend.model.ws.DirectSocketEventResponse;
import com.doan.frontend.model.ws.PresenceUpdateSocketRequest;
import com.doan.frontend.model.ws.SocketEventType;
import com.doan.frontend.model.ws.TypingSocketRequest;
import com.doan.frontend.service.StompWebSocketService;
import com.doan.frontend.util.UiUtils;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.geometry.Side;

public class MainView extends BorderPane {
    private static final DateTimeFormatter TIME_FORMATTER =
        DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    private final AppContext appContext;
    private final Runnable onLogout;
    private final ListView<GuildResponse> guildListView = new ListView<>();
    private final ListView<ChannelResponse> channelListView = new ListView<>();
    private final ListView<MessageResponse> messageListView = new ListView<>();
    private final ListView<GuildMemberResponse> memberListView = new ListView<>();
    private final ObservableList<GuildMemberResponse> currentMembers = FXCollections.observableArrayList();
    private final ObservableList<MessageResponse> emptyMessages = FXCollections.observableArrayList();
    private final Label selectedGuildLabel = new Label("No server selected");
    private final Label currentChannelLabel = new Label("Select a channel");
    private final Label typingLabel = new Label("Select a server and channel to begin.");
    private final Label onlineCountLabel = new Label("0 online");
    private final Label totalCountLabel = new Label("0 total");
    private final Label currentUserLabel = new Label("Unknown user");
    private final TextArea messageInput = new TextArea();
    private final Button createGuildButton = new Button("+");
    private final Button createChannelButton = new Button("+");
    private final Button guildMenuButton = new Button("...");
    private final Button channelMenuButton = new Button("...");
    private final Button uploadButton = new Button("Upload");
    private final Button sendButton = new Button("Send");
    private final Button logoutButton = new Button("Logout");
    private final PauseTransition clearTypingLabelTransition = new PauseTransition(Duration.seconds(2));
    private final PauseTransition stopTypingTransition = new PauseTransition(Duration.seconds(1.2));
    private StompWebSocketService.SubscriptionHandle channelSubscription;
    private StompWebSocketService.SubscriptionHandle presenceSubscription;
    private StompWebSocketService.SubscriptionHandle directSubscription;

    public MainView(AppContext appContext, Runnable onLogout) {
        this.appContext = appContext;
        this.onLogout = onLogout;
        buildLayout();
        bindStores();
        wireActions();
        initializeSession();
    }

    private void buildLayout() {
        getStyleClass().add("main-root");

        VBox guildPane = createGuildPane();
        VBox channelPane = createChannelPane();
        VBox chatPane = createChatPane();
        VBox memberPane = createMemberPane();

        HBox shell = new HBox(guildPane, channelPane, chatPane, memberPane);
        shell.getStyleClass().add("main-shell");
        setCenter(shell);
    }

    private VBox createGuildPane() {
        Label title = new Label("Servers");
        title.getStyleClass().add("panel-title");

        configureToolbarButton(createGuildButton, "Create server");
        createGuildButton.setOnAction(event -> createGuild());

        guildListView.getStyleClass().add("guild-list");
        guildListView.setItems(appContext.guildStore().getGuildList());
        guildListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        guildListView.setPlaceholder(createEmptyState("No servers", "Create your first server to get started."));
        guildListView.setCellFactory(list -> new GuildListCell());

        VBox pane = new VBox(12, createPanelHeader(title, createGuildButton), guildListView);
        pane.getStyleClass().addAll("sidebar-pane", "guild-pane");
        pane.setPadding(new Insets(16));
        pane.setPrefWidth(200);
        pane.setMinWidth(180);
        VBox.setVgrow(guildListView, Priority.ALWAYS);
        return pane;
    }

    private VBox createChannelPane() {
        Label title = new Label("Channels");
        title.getStyleClass().add("panel-title");
        selectedGuildLabel.getStyleClass().add("panel-subtitle");

        configureToolbarButton(createChannelButton, "Create channel");
        createChannelButton.setOnAction(event -> createChannel());
        configureToolbarButton(guildMenuButton, "Server actions");
        guildMenuButton.setOnAction(event -> showGuildActionsMenu(guildMenuButton));

        channelListView.getStyleClass().add("channel-list");
        channelListView.setItems(appContext.channelStore().getChannelList());
        channelListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        channelListView.setPlaceholder(createEmptyState("No channels", "Create a text channel for this server."));
        channelListView.setCellFactory(list -> new ChannelListCell());

        HBox toolbar = new HBox(8, createChannelButton, guildMenuButton);
        toolbar.getStyleClass().add("toolbar-group");
        VBox pane = new VBox(12, createPanelHeader(new VBox(2, title, selectedGuildLabel), toolbar), channelListView);
        pane.getStyleClass().add("sidebar-pane");
        pane.setPadding(new Insets(16));
        pane.setPrefWidth(260);
        pane.setMinWidth(240);
        VBox.setVgrow(channelListView, Priority.ALWAYS);
        return pane;
    }

    private VBox createChatPane() {
        currentChannelLabel.getStyleClass().add("chat-title");
        typingLabel.getStyleClass().add("typing-label");

        messageListView.getStyleClass().add("message-list");
        messageListView.setItems(emptyMessages);
        messageListView.setPlaceholder(createEmptyState("No messages", "Choose a channel or send the first message."));
        messageListView.setCellFactory(list -> new MessageListCell());
        messageListView.setFocusTraversable(false);
        VBox.setVgrow(messageListView, Priority.ALWAYS);

        messageInput.getStyleClass().add("message-input");
        messageInput.setPromptText("Select a channel to message");
        messageInput.setWrapText(true);
        messageInput.setPrefRowCount(3);
        messageInput.setMinHeight(86);
        messageInput.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER && !event.isShiftDown()) {
                event.consume();
                sendCurrentMessage(null);
            }
        });

        uploadButton.getStyleClass().add("panel-button");
        uploadButton.setOnAction(event -> chooseAndUploadFile());

        sendButton.getStyleClass().add("primary-button");
        sendButton.setDefaultButton(true);
        sendButton.setOnAction(event -> sendCurrentMessage(null));

        configureToolbarButton(channelMenuButton, "Channel actions");
        channelMenuButton.setOnAction(event -> showChannelActionsMenu(channelMenuButton));

        Region composerSpacer = new Region();
        HBox.setHgrow(composerSpacer, Priority.ALWAYS);
        HBox composerActions = new HBox(10, uploadButton, composerSpacer, sendButton);
        composerActions.setAlignment(Pos.CENTER_LEFT);

        VBox composer = new VBox(10, messageInput, composerActions);
        composer.getStyleClass().add("composer-pane");

        updateCurrentUserLabel();
        currentUserLabel.getStyleClass().add("footer-label");

        logoutButton.getStyleClass().add("quiet-button");
        logoutButton.setOnAction(event -> logout());

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);
        HBox footer = new HBox(12, currentUserLabel, footerSpacer, logoutButton);
        footer.getStyleClass().add("chat-footer");
        footer.setAlignment(Pos.CENTER_LEFT);

        VBox headerContent = new VBox(4, currentChannelLabel, typingLabel);
        HBox header = createPanelHeader(headerContent, channelMenuButton);
        header.getStyleClass().add("chat-header");

        VBox pane = new VBox(16, header, messageListView, composer, footer);
        pane.getStyleClass().add("chat-pane");
        pane.setPadding(new Insets(16));
        HBox.setHgrow(pane, Priority.ALWAYS);
        VBox.setVgrow(messageListView, Priority.ALWAYS);
        return pane;
    }

    private VBox createMemberPane() {
        Label title = new Label("Members");
        title.getStyleClass().add("panel-title");
        onlineCountLabel.getStyleClass().add("member-stat-online");
        totalCountLabel.getStyleClass().add("member-stat-total");
        HBox memberSummary = new HBox(8, onlineCountLabel, totalCountLabel);
        memberSummary.getStyleClass().add("member-summary");

        memberListView.getStyleClass().add("member-list");
        memberListView.setItems(currentMembers);
        memberListView.setPlaceholder(createEmptyState("No members", "Select a server to view members."));
        memberListView.setCellFactory(list -> new MemberListCell());

        VBox pane = new VBox(12, createPanelHeader(new VBox(6, title, memberSummary), null), memberListView);
        pane.getStyleClass().addAll("sidebar-pane", "member-pane");
        pane.setPadding(new Insets(16));
        pane.setPrefWidth(260);
        pane.setMinWidth(240);
        VBox.setVgrow(memberListView, Priority.ALWAYS);
        return pane;
    }

    private HBox createPanelHeader(Node leftContent, Node rightContent) {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(12);
        header.getStyleClass().add("panel-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.getChildren().add(leftContent);
        header.getChildren().add(spacer);
        if (rightContent != null) {
            header.getChildren().add(rightContent);
        }
        return header;
    }

    private VBox createEmptyState(String titleText, String copyText) {
        Label title = new Label(titleText);
        title.getStyleClass().add("empty-state-title");
        Label copy = new Label(copyText);
        copy.getStyleClass().add("empty-state-copy");
        copy.setWrapText(true);

        VBox box = new VBox(6, title, copy);
        box.getStyleClass().add("empty-state");
        box.setAlignment(Pos.CENTER);
        box.setMaxWidth(260);
        return box;
    }

    private void configureToolbarButton(Button button, String tooltipText) {
        button.getStyleClass().setAll("toolbar-button");
        button.setTooltip(new Tooltip(tooltipText));
        button.setFocusTraversable(false);
        button.setMinSize(36, 36);
        button.setPrefSize(36, 36);
        button.setMaxSize(36, 36);
    }

    private void bindStores() {
        guildListView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            appContext.guildStore().setSelectedGuild(newValue);
            if (newValue != null) {
                onGuildSelected(newValue);
            } else {
                resetGuildSelection();
            }
        });

        channelListView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            appContext.channelStore().setSelectedChannel(newValue);
            if (newValue != null) {
                onChannelSelected(newValue);
            } else {
                resetChannelSelection();
            }
        });

        clearTypingLabelTransition.setOnFinished(event -> typingLabel.setText(""));
        stopTypingTransition.setOnFinished(event -> sendTyping(false));
        updateActionStates();
    }

    private void wireActions() {
        messageInput.textProperty().addListener((obs, oldValue, newValue) -> {
            updateActionStates();
            if (newValue != null && !newValue.isBlank()) {
                sendTyping(true);
                stopTypingTransition.playFromStart();
            } else {
                stopTypingTransition.stop();
                sendTyping(false);
            }
        });
    }

    private void initializeSession() {
        updateCurrentUserLabel();
        appContext.stompWebSocketService().setErrorHandler(error -> UiUtils.showError("WebSocket error", error));
        if (appContext.stompWebSocketService().isConnected()) {
            onSocketReady();
            return;
        }
        appContext.stompWebSocketService().connect()
            .thenRun(() -> UiUtils.runOnFx(this::onSocketReady))
            .exceptionally(throwable -> {
                UiUtils.showError("WebSocket connection failed", throwable);
                UiUtils.runOnFx(this::loadGuilds);
                return null;
            });
    }

    private void onSocketReady() {
        subscribeDirectMessages();
        loadGuilds();
    }

    private void loadGuilds() {
        appContext.guildApiService().getMyGuilds()
            .thenAccept(guilds -> UiUtils.runOnFx(() -> {
                appContext.guildStore().getGuildList().setAll(guilds);
                if (!guilds.isEmpty()) {
                    guildListView.getSelectionModel().selectFirst();
                } else {
                    resetGuildSelection();
                }
            }))
            .exceptionally(throwable -> {
                UiUtils.showError("Cannot load guilds", throwable);
                return null;
            });
    }

    private void onGuildSelected(GuildResponse guild) {
        selectedGuildLabel.setText(guild.name());
        currentMembers.clear();
        updateMemberSummary();
        appContext.channelStore().clear();
        resetChannelSelection();
        updateActionStates();

        appContext.guildApiService().getGuildMembers(guild.id())
            .thenAccept(members -> UiUtils.runOnFx(() -> {
                currentMembers.setAll(members);
                updateMemberSummary();
                updateActionStates();
                memberListView.refresh();
            }))
            .exceptionally(throwable -> {
                UiUtils.showError("Cannot load guild members", throwable);
                return null;
            });

        appContext.channelApiService().getChannelsByGuild(guild.id())
            .thenAccept(channels -> UiUtils.runOnFx(() -> {
                appContext.channelStore().getChannelList().setAll(channels);
                if (!channels.isEmpty()) {
                    channelListView.getSelectionModel().selectFirst();
                } else {
                    resetChannelSelection();
                }
            }))
            .exceptionally(throwable -> {
                UiUtils.showError("Cannot load channels", throwable);
                return null;
            });

        subscribePresence(guild.id());
        sendPresence(UserStatus.ONLINE);
    }

    private void resetGuildSelection() {
        selectedGuildLabel.setText("No server selected");
        currentMembers.clear();
        updateMemberSummary();
        if (presenceSubscription != null) {
            presenceSubscription.unsubscribe();
            presenceSubscription = null;
        }
        resetChannelSelection();
        updateActionStates();
    }

    private void onChannelSelected(ChannelResponse channel) {
        currentChannelLabel.setText("# " + channel.name());
        typingLabel.setText("");
        messageListView.setItems(appContext.messageStore().getMessages(channel.id()));
        updateActionStates();
        loadMessages(channel.id());
        subscribeChannel(channel);
    }

    private void resetChannelSelection() {
        currentChannelLabel.setText("Select a channel");
        typingLabel.setText("Select a server and channel to begin.");
        messageListView.setItems(emptyMessages);
        if (channelSubscription != null) {
            channelSubscription.unsubscribe();
            channelSubscription = null;
        }
        updateActionStates();
    }

    private void loadMessages(String channelId) {
        appContext.messageApiService().getMessagesByChannel(channelId)
            .thenAccept(messages -> UiUtils.runOnFx(() -> {
                appContext.messageStore().replaceMessages(channelId, messages);
                scrollMessagesToBottom();
            }))
            .exceptionally(throwable -> {
                UiUtils.showError("Cannot load messages", throwable);
                return null;
            });
    }

    private void subscribeChannel(ChannelResponse channel) {
        if (channelSubscription != null) {
            channelSubscription.unsubscribe();
        }
        if (!appContext.stompWebSocketService().isConnected()) {
            return;
        }
        channelSubscription = appContext.stompWebSocketService()
            .subscribeChannel(channel.id(), event -> UiUtils.runOnFx(() -> handleChannelEvent(event)));
    }

    private void subscribePresence(String guildId) {
        if (presenceSubscription != null) {
            presenceSubscription.unsubscribe();
        }
        if (!appContext.stompWebSocketService().isConnected()) {
            return;
        }
        presenceSubscription = appContext.stompWebSocketService()
            .subscribePresence(guildId, event -> UiUtils.runOnFx(() -> handlePresenceEvent(event)));
    }

    private void subscribeDirectMessages() {
        if (directSubscription != null || !appContext.stompWebSocketService().isConnected()) {
            return;
        }
        directSubscription = appContext.stompWebSocketService()
            .subscribeDirectMessages(event -> UiUtils.runOnFx(() -> handleDirectMessageEvent(event)));
    }

    private void handleChannelEvent(ChannelSocketEventResponse event) {
        if (event == null || event.eventType() == null) {
            return;
        }

        if (event.eventType() == SocketEventType.MESSAGE_CREATED || event.eventType() == SocketEventType.MESSAGE_UPDATED) {
            if (event.message() != null) {
                appContext.messageStore().upsertMessage(event.message());
                if (isSelectedChannel(event.message().channelId())) {
                    messageListView.setItems(appContext.messageStore().getMessages(event.message().channelId()));
                    scrollMessagesToBottom();
                }
            }
            return;
        }

        if (event.eventType() == SocketEventType.MESSAGE_DELETED && appContext.channelStore().getSelectedChannel() != null) {
            appContext.messageStore().markDeleted(appContext.channelStore().getSelectedChannel().id(), event.messageId());
            return;
        }

        if (event.eventType() == SocketEventType.TYPING && event.typing() != null) {
            if (isSelectedChannel(event.typing().channelId())
                && !event.typing().userId().equals(appContext.authStore().getCurrentUser().id())
                && event.typing().typing()) {
                typingLabel.setText(event.typing().displayName() + " is typing...");
                clearTypingLabelTransition.playFromStart();
            } else if (!event.typing().typing()) {
                typingLabel.setText("");
            }
        }
    }

    private void handlePresenceEvent(ChannelSocketEventResponse event) {
        if (event == null || event.presence() == null) {
            return;
        }
        if (event.presence().status() == UserStatus.ONLINE || event.presence().status() == UserStatus.IDLE) {
            appContext.presenceStore().markOnline(event.presence().userId());
        } else {
            appContext.presenceStore().markOffline(event.presence().userId());
        }
        updateMemberSummary();
        memberListView.refresh();
    }

    private void handleDirectMessageEvent(DirectSocketEventResponse event) {
        if (event != null && event.directMessage() != null
            && !event.directMessage().senderId().equals(appContext.authStore().getCurrentUser().id())) {
            UiUtils.showInfo("Direct message", "New direct message from user " + event.directMessage().senderId());
        }
    }

    private void sendCurrentMessage(List<String> attachmentIds) {
        ChannelResponse selectedChannel = appContext.channelStore().getSelectedChannel();
        GuildResponse selectedGuild = appContext.guildStore().getSelectedGuild();
        if (selectedChannel == null || selectedGuild == null) {
            UiUtils.showInfo("No channel", "Select a guild and channel first.");
            return;
        }

        String content = messageInput.getText() == null ? "" : messageInput.getText().trim();
        boolean hasAttachments = attachmentIds != null && !attachmentIds.isEmpty();
        if (content.isBlank() && !hasAttachments) {
            return;
        }

        MessageType messageType = resolveMessageType(attachmentIds);
        appContext.messageApiService()
            .sendMessageRest(
                selectedChannel.id(),
                new MessageCreateRequest(content, messageType, null, attachmentIds)
            )
            .thenAccept(message -> UiUtils.runOnFx(() -> {
                appContext.messageStore().upsertMessage(message);
                scrollMessagesToBottom();
                messageInput.clear();
            }))
            .exceptionally(throwable -> {
                UiUtils.showError("Cannot send message", throwable);
                return null;
            });
    }

    private void chooseAndUploadFile() {
        if (appContext.channelStore().getSelectedChannel() == null) {
            UiUtils.showInfo("No channel", "Select a channel before uploading a file.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        java.io.File selectedFile = fileChooser.showOpenDialog(getScene().getWindow());
        if (selectedFile == null) {
            return;
        }
        Path path = selectedFile.toPath();
        appContext.fileApiService().uploadFile(path)
            .thenAccept(file -> UiUtils.runOnFx(() -> sendAttachmentMessage(file)))
            .exceptionally(throwable -> {
                UiUtils.showError("File upload failed", throwable);
                return null;
            });
    }

    private void sendAttachmentMessage(FileUploadResponse fileUploadResponse) {
        sendCurrentMessage(List.of(fileUploadResponse.id()));
    }

    private void createGuild() {
        UiUtils.prompt("Create guild", "Enter guild name", "")
            .ifPresent(name -> appContext.guildApiService().createGuild(name)
                .thenAccept(guild -> UiUtils.runOnFx(() -> {
                    appContext.guildStore().getGuildList().add(guild);
                    guildListView.getSelectionModel().select(guild);
                }))
                .exceptionally(throwable -> {
                    UiUtils.showError("Cannot create guild", throwable);
                    return null;
                }));
    }

    private void createChannel() {
        GuildResponse guild = appContext.guildStore().getSelectedGuild();
        if (guild == null) {
            UiUtils.showInfo("No guild", "Select a guild first.");
            return;
        }
        UiUtils.prompt("Create channel", "Enter channel name", "")
            .ifPresent(name -> appContext.channelApiService().createChannel(guild.id(), name)
                .thenAccept(channel -> UiUtils.runOnFx(() -> {
                    appContext.channelStore().getChannelList().add(channel);
                    channelListView.getSelectionModel().select(channel);
                }))
                .exceptionally(throwable -> {
                    UiUtils.showError("Cannot create channel", throwable);
                    return null;
                }));
    }

    private void editMessage(MessageResponse message) {
        UiUtils.prompt("Edit message", "Update message content", message.content())
            .ifPresent(content -> appContext.messageApiService().editMessage(message.id(), content)
                .thenAccept(updated -> UiUtils.runOnFx(() -> appContext.messageStore().upsertMessage(updated)))
                .exceptionally(throwable -> {
                    UiUtils.showError("Cannot edit message", throwable);
                    return null;
                }));
    }

    private void deleteMessage(MessageResponse message) {
        if (!UiUtils.confirm("Delete message", "Delete this message?", "This action cannot be undone.")) {
            return;
        }
        appContext.messageApiService().deleteMessage(message.id())
            .thenRun(() -> UiUtils.runOnFx(() -> appContext.messageStore().markDeleted(message.channelId(), message.id())))
            .exceptionally(throwable -> {
                UiUtils.showError("Cannot delete message", throwable);
                return null;
            });
    }

    private void reactToMessage(MessageResponse message, String emoji) {
        appContext.messageApiService().addReaction(message.id(), emoji)
            .thenAccept(updated -> UiUtils.runOnFx(() -> appContext.messageStore().upsertMessage(updated)))
            .exceptionally(throwable -> {
                UiUtils.showError("Cannot react to message", throwable);
                return null;
            });
    }

    private void logout() {
        sendPresence(UserStatus.OFFLINE);
        onLogout.run();
    }

    private void deleteSelectedGuild() {
        deleteGuild(appContext.guildStore().getSelectedGuild());
    }

    private void deleteGuild(GuildResponse guild) {
        if (guild == null) {
            UiUtils.showInfo("No server", "Select a server first.");
            return;
        }
        if (!canDeleteGuild(guild)) {
            UiUtils.showInfo("Permission denied", "Only the server owner can delete this server.");
            return;
        }
        if (!UiUtils.confirm("Delete server", "Delete server '" + guild.name() + "'?", "Channels and messages in this server will be removed.")) {
            return;
        }
        appContext.guildApiService().deleteGuild(guild.id())
            .thenRun(() -> UiUtils.runOnFx(() -> removeGuildFromView(guild)))
            .exceptionally(throwable -> {
                UiUtils.showError("Cannot delete server", throwable);
                return null;
            });
    }

    private void deleteSelectedChannel() {
        deleteChannel(appContext.channelStore().getSelectedChannel());
    }

    private void deleteChannel(ChannelResponse channel) {
        if (channel == null) {
            UiUtils.showInfo("No channel", "Select a channel first.");
            return;
        }
        if (!currentUserCanManageChannels()) {
            UiUtils.showInfo("Permission denied", "Only OWNER or ADMIN can delete channels.");
            return;
        }
        if (!UiUtils.confirm("Delete channel", "Delete channel '# " + channel.name() + "'?", "Messages in this channel will be removed.")) {
            return;
        }
        appContext.channelApiService().deleteChannel(channel.id())
            .thenRun(() -> UiUtils.runOnFx(() -> removeChannelFromView(channel)))
            .exceptionally(throwable -> {
                UiUtils.showError("Cannot delete channel", throwable);
                return null;
            });
    }

    private void showGuildActionsMenu(Node owner) {
        GuildResponse selectedGuild = appContext.guildStore().getSelectedGuild();
        if (selectedGuild == null) {
            return;
        }
        ContextMenu menu = buildGuildContextMenu(selectedGuild);
        if (!menu.getItems().isEmpty()) {
            menu.show(owner, Side.BOTTOM, 0, 6);
        }
    }

    private void showChannelActionsMenu(Node owner) {
        ChannelResponse selectedChannel = appContext.channelStore().getSelectedChannel();
        if (selectedChannel == null) {
            return;
        }
        ContextMenu menu = buildChannelContextMenu(selectedChannel);
        if (!menu.getItems().isEmpty()) {
            menu.show(owner, Side.BOTTOM, 0, 6);
        }
    }

    private ContextMenu buildGuildContextMenu(GuildResponse guild) {
        ContextMenu menu = new ContextMenu();
        MenuItem deleteItem = new MenuItem("Delete Server");
        deleteItem.setDisable(!canDeleteGuild(guild));
        deleteItem.setOnAction(event -> deleteGuild(guild));
        menu.getItems().add(deleteItem);
        return menu;
    }

    private ContextMenu buildChannelContextMenu(ChannelResponse channel) {
        ContextMenu menu = new ContextMenu();
        MenuItem deleteItem = new MenuItem("Delete Channel");
        deleteItem.setDisable(!currentUserCanManageChannels());
        deleteItem.setOnAction(event -> deleteChannel(channel));
        menu.getItems().add(deleteItem);
        return menu;
    }

    private void sendTyping(boolean typing) {
        ChannelResponse channel = appContext.channelStore().getSelectedChannel();
        GuildResponse guild = appContext.guildStore().getSelectedGuild();
        if (channel == null || guild == null || !appContext.stompWebSocketService().isConnected()) {
            return;
        }
        try {
            appContext.stompWebSocketService().sendTyping(new TypingSocketRequest(guild.id(), channel.id(), typing));
        } catch (Exception ignored) {
        }
    }

    private void sendPresence(UserStatus status) {
        GuildResponse guild = appContext.guildStore().getSelectedGuild();
        if (guild == null || !appContext.stompWebSocketService().isConnected()) {
            return;
        }
        String currentUserId = currentUserId();
        if (currentUserId != null) {
            if (status == UserStatus.ONLINE || status == UserStatus.IDLE) {
                appContext.presenceStore().markOnline(currentUserId);
            } else {
                appContext.presenceStore().markOffline(currentUserId);
            }
            updateMemberSummary();
            memberListView.refresh();
        }
        try {
            appContext.stompWebSocketService().sendPresence(new PresenceUpdateSocketRequest(guild.id(), status));
        } catch (Exception ignored) {
        }
    }

    private MessageType resolveMessageType(List<String> attachmentIds) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return MessageType.TEXT;
        }
        return MessageType.FILE;
    }

    private void updateCurrentUserLabel() {
        UserResponse currentUser = appContext.authStore().getCurrentUser();
        if (currentUser == null) {
            currentUserLabel.setText("Unknown user");
            return;
        }
        currentUserLabel.setText(currentUser.displayName() + "  @" + currentUser.username());
    }

    private void updateMemberSummary() {
        long onlineCount = currentMembers.stream()
            .filter(this::isMemberOnline)
            .count();
        onlineCountLabel.setText(onlineCount + " online");
        totalCountLabel.setText(currentMembers.size() + " total");
    }

    private void updateActionStates() {
        GuildResponse selectedGuild = appContext.guildStore().getSelectedGuild();
        ChannelResponse selectedChannel = appContext.channelStore().getSelectedChannel();
        boolean hasGuild = selectedGuild != null;
        boolean hasChannel = selectedChannel != null;
        boolean hasMessage = messageInput.getText() != null && !messageInput.getText().trim().isBlank();

        createChannelButton.setDisable(!hasGuild);
        guildMenuButton.setDisable(!hasGuild || !canDeleteGuild(selectedGuild));
        channelMenuButton.setDisable(!hasChannel || !currentUserCanManageChannels());
        uploadButton.setDisable(!hasChannel);
        sendButton.setDisable(!hasChannel || !hasMessage);
        messageInput.setDisable(!hasChannel);
        messageInput.setPromptText(hasChannel ? "Message #" + selectedChannel.name() : "Select a channel to message");
    }

    private boolean canDeleteGuild(GuildResponse guild) {
        return guild != null && guild.ownerId() != null && guild.ownerId().equals(currentUserId());
    }

    private boolean isMemberOnline(GuildMemberResponse member) {
        if (member == null) {
            return false;
        }
        if (appContext.presenceStore().isOnline(member.userId())) {
            return true;
        }
        return member.status() == UserStatus.ONLINE || member.status() == UserStatus.IDLE;
    }

    private boolean currentUserCanManageChannels() {
        GuildResponse selectedGuild = appContext.guildStore().getSelectedGuild();
        if (selectedGuild == null) {
            return false;
        }
        if (canDeleteGuild(selectedGuild)) {
            return true;
        }
        GuildMemberResponse currentMember = currentMember();
        return currentMember != null
            && currentMember.roleTypes() != null
            && currentMember.roleTypes().stream().anyMatch(role -> "OWNER".equals(role) || "ADMIN".equals(role));
    }

    private GuildMemberResponse currentMember() {
        String currentUserId = currentUserId();
        return currentMembers.stream()
            .filter(member -> member.userId().equals(currentUserId))
            .findFirst()
            .orElse(null);
    }

    private String currentUserId() {
        UserResponse currentUser = appContext.authStore().getCurrentUser();
        return currentUser == null ? null : currentUser.id();
    }

    private boolean canDeleteMessage(MessageResponse message) {
        return message != null
            && !message.deleted()
            && (message.senderId().equals(currentUserId()) || currentUserCanManageChannels());
    }

    private boolean canEditMessage(MessageResponse message) {
        return message != null && !message.deleted() && message.senderId().equals(currentUserId());
    }

    private void removeGuildFromView(GuildResponse guild) {
        int removedIndex = appContext.guildStore().getGuildList().indexOf(guild);
        appContext.guildStore().getGuildList().remove(guild);
        if (appContext.guildStore().getGuildList().isEmpty()) {
            guildListView.getSelectionModel().clearSelection();
            resetGuildSelection();
            return;
        }
        guildListView.getSelectionModel().select(Math.min(removedIndex, appContext.guildStore().getGuildList().size() - 1));
    }

    private void removeChannelFromView(ChannelResponse channel) {
        int removedIndex = appContext.channelStore().getChannelList().indexOf(channel);
        appContext.channelStore().getChannelList().remove(channel);
        if (appContext.channelStore().getChannelList().isEmpty()) {
            channelListView.getSelectionModel().clearSelection();
            resetChannelSelection();
            return;
        }
        channelListView.getSelectionModel().select(Math.min(removedIndex, appContext.channelStore().getChannelList().size() - 1));
    }

    private boolean isSelectedChannel(String channelId) {
        ChannelResponse selectedChannel = appContext.channelStore().getSelectedChannel();
        return selectedChannel != null && selectedChannel.id().equals(channelId);
    }

    private void scrollMessagesToBottom() {
        if (messageListView.getItems() != null && !messageListView.getItems().isEmpty()) {
            messageListView.scrollTo(messageListView.getItems().size() - 1);
        }
    }

    private String resolveDisplayName(GuildMemberResponse member) {
        if (member.nickname() != null && !member.nickname().isBlank()) {
            return member.nickname();
        }
        if (member.displayName() != null && !member.displayName().isBlank()) {
            return member.displayName();
        }
        return member.username();
    }

    private String resolveRoleSummary(GuildMemberResponse member) {
        if (member.roleTypes() == null || member.roleTypes().isEmpty()) {
            return "Member";
        }
        return String.join(", ", member.roleTypes());
    }

    private String initials(String value) {
        if (value == null || value.isBlank()) {
            return "?";
        }
        String[] parts = value.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ROOT);
        }
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }

    private final class GuildListCell extends ListCell<GuildResponse> {
        private final HBox root = new HBox(12);
        private final Label badgeLabel = new Label();
        private final Label nameLabel = new Label();

        private GuildListCell() {
            root.getStyleClass().add("guild-cell");
            root.setAlignment(Pos.CENTER_LEFT);
            badgeLabel.getStyleClass().add("cell-badge");
            nameLabel.getStyleClass().add("cell-title");
            nameLabel.setWrapText(true);
            root.getChildren().addAll(badgeLabel, nameLabel);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            setOnContextMenuRequested(event -> {
                if (getItem() != null) {
                    guildListView.getSelectionModel().select(getItem());
                }
            });
        }

        @Override
        protected void updateItem(GuildResponse item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setContextMenu(null);
                return;
            }
            badgeLabel.setText(initials(item.name()));
            nameLabel.setText(item.name());
            setGraphic(root);
            setContextMenu(buildGuildContextMenu(item));
        }
    }

    private final class ChannelListCell extends ListCell<ChannelResponse> {
        private final HBox root = new HBox(10);
        private final Label hashLabel = new Label("#");
        private final Label nameLabel = new Label();

        private ChannelListCell() {
            root.getStyleClass().add("channel-cell");
            root.setAlignment(Pos.CENTER_LEFT);
            hashLabel.getStyleClass().add("channel-prefix");
            nameLabel.getStyleClass().add("cell-title");
            root.getChildren().addAll(hashLabel, nameLabel);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            setOnContextMenuRequested(event -> {
                if (getItem() != null) {
                    channelListView.getSelectionModel().select(getItem());
                }
            });
        }

        @Override
        protected void updateItem(ChannelResponse item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setContextMenu(null);
                return;
            }
            nameLabel.setText(item.name());
            setGraphic(root);
            setContextMenu(buildChannelContextMenu(item));
        }
    }

    private final class MemberListCell extends ListCell<GuildMemberResponse> {
        private final HBox root = new HBox(10);
        private final Label badgeLabel = new Label();
        private final Label nameLabel = new Label();
        private final Label roleLabel = new Label();
        private final Label statusLabel = new Label();
        private final VBox textBox = new VBox(2);

        private MemberListCell() {
            root.getStyleClass().add("member-cell");
            root.setAlignment(Pos.CENTER_LEFT);

            badgeLabel.getStyleClass().add("cell-badge");
            nameLabel.getStyleClass().add("cell-title");
            roleLabel.getStyleClass().add("cell-subtitle");
            statusLabel.getStyleClass().add("status-pill");

            textBox.getChildren().addAll(nameLabel, roleLabel);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            root.getChildren().addAll(badgeLabel, textBox, spacer, statusLabel);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }

        @Override
        protected void updateItem(GuildMemberResponse item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }

            boolean isOnline = isMemberOnline(item);
            badgeLabel.setText(initials(resolveDisplayName(item)));
            nameLabel.setText(resolveDisplayName(item));
            roleLabel.setText(resolveRoleSummary(item));
            statusLabel.setText(isOnline ? "online" : item.status().name().toLowerCase(Locale.ROOT));
            statusLabel.getStyleClass().setAll("status-pill", isOnline ? "status-online" : "status-offline");
            setGraphic(root);
        }
    }

    private final class MessageListCell extends ListCell<MessageResponse> {
        private final VBox root = new VBox(6);
        private final HBox header = new HBox(10);
        private final Label authorLabel = new Label();
        private final Label timeLabel = new Label();
        private final HBox actionBox = new HBox(6);
        private final Button editButton = new Button("Edit");
        private final Button deleteButton = new Button("Delete");
        private final Label contentLabel = new Label();
        private final Label metaLabel = new Label();
        private final Label reactionLabel = new Label();
        private final ContextMenu contextMenu = new ContextMenu();

        private MessageListCell() {
            authorLabel.getStyleClass().add("message-author");
            timeLabel.getStyleClass().add("message-time");
            contentLabel.getStyleClass().add("message-content");
            metaLabel.getStyleClass().add("message-meta");
            reactionLabel.getStyleClass().add("message-reactions");

            contentLabel.setWrapText(true);
            root.getStyleClass().add("message-card");
            root.prefWidthProperty().bind(messageListView.widthProperty().subtract(36));
            contentLabel.maxWidthProperty().bind(root.prefWidthProperty().subtract(24));
            actionBox.getStyleClass().add("message-action-bar");
            actionBox.managedProperty().bind(actionBox.visibleProperty());
            actionBox.setVisible(false);
            editButton.getStyleClass().add("message-action-button");
            deleteButton.getStyleClass().addAll("message-action-button", "message-action-danger");
            actionBox.getChildren().addAll(editButton, deleteButton);
            hoverProperty().addListener((obs, oldValue, newValue) -> updateActionBoxVisibility());

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            header.getChildren().addAll(authorLabel, spacer, actionBox, timeLabel);
            root.getChildren().addAll(header, contentLabel, metaLabel, reactionLabel);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }

        @Override
        protected void updateItem(MessageResponse item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setContextMenu(null);
                return;
            }

            String displayName = item.senderDisplayName() == null || item.senderDisplayName().isBlank()
                ? item.senderId()
                : item.senderDisplayName();
            authorLabel.setText(displayName);
            timeLabel.setText(TIME_FORMATTER.format(item.createdAt()));

            boolean deleted = item.deleted();
            contentLabel.setText(deleted ? "[deleted]" : item.content());
            contentLabel.getStyleClass().setAll("message-content", deleted ? "deleted-message" : "message-content");
            editButton.setVisible(canEditMessage(item));
            deleteButton.setVisible(canDeleteMessage(item));
            editButton.setOnAction(event -> editMessage(item));
            deleteButton.setOnAction(event -> deleteMessage(item));
            updateActionBoxVisibility();

            String meta = buildMeta(item);
            metaLabel.setText(meta);
            metaLabel.setManaged(!meta.isBlank());
            metaLabel.setVisible(!meta.isBlank());

            String reactions = buildReactions(item);
            reactionLabel.setText(reactions);
            reactionLabel.setManaged(!reactions.isBlank());
            reactionLabel.setVisible(!reactions.isBlank());

            contextMenu.getItems().setAll(buildContextMenu(item));
            setContextMenu(contextMenu);
            setGraphic(root);
        }

        private void updateActionBoxVisibility() {
            MessageResponse item = getItem();
            if (item == null) {
                actionBox.setVisible(false);
                return;
            }
            actionBox.setVisible(isHover() && (canEditMessage(item) || canDeleteMessage(item)));
        }

        private String buildMeta(MessageResponse item) {
            StringBuilder builder = new StringBuilder();
            if (item.editedAt() != null) {
                builder.append("Edited");
            }
            if (item.attachments() != null && !item.attachments().isEmpty()) {
                if (!builder.isEmpty()) {
                    builder.append("  ");
                }
                builder.append("Attachments: ");
                builder.append(item.attachments().stream()
                    .map(attachment -> attachment.fileName())
                    .reduce((left, right) -> left + ", " + right)
                    .orElse(""));
            }
            return builder.toString();
        }

        private String buildReactions(MessageResponse item) {
            if (item.reactions() == null || item.reactions().isEmpty()) {
                return "";
            }
            return item.reactions().stream()
                .map(reaction -> reaction.emoji() + " " + reaction.userIds().size())
                .reduce((left, right) -> left + "   " + right)
                .orElse("");
        }

        private List<MenuItem> buildContextMenu(MessageResponse item) {
            String thumbsUpEmoji = "\uD83D\uDC4D";
            MenuItem reactThumb = new MenuItem("React " + thumbsUpEmoji);
            reactThumb.setOnAction(event -> reactToMessage(item, thumbsUpEmoji));

            MenuItem editItem = new MenuItem("Edit");
            editItem.setOnAction(event -> editMessage(item));
            editItem.setDisable(!canEditMessage(item));

            MenuItem deleteItem = new MenuItem("Delete");
            deleteItem.setOnAction(event -> deleteMessage(item));
            deleteItem.setDisable(!canDeleteMessage(item));

            return List.of(reactThumb, editItem, deleteItem);
        }
    }
}
