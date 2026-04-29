package com.doan.frontend.store;

import com.doan.frontend.model.message.MessageResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class MessageStore {
    private final Map<String, ObservableList<MessageResponse>> messagesByChannel = new ConcurrentHashMap<>();

    public ObservableList<MessageResponse> getMessages(String channelId) {
        return messagesByChannel.computeIfAbsent(channelId, key -> FXCollections.observableArrayList());
    }

    public void replaceMessages(String channelId, List<MessageResponse> messages) {
        ObservableList<MessageResponse> list = getMessages(channelId);
        list.setAll(messages);
    }

    public void upsertMessage(MessageResponse message) {
        ObservableList<MessageResponse> list = getMessages(message.channelId());
        int index = findIndex(list, message.id());
        if (index >= 0) {
            list.set(index, message);
        } else {
            list.add(message);
        }
    }

    public void markDeleted(String channelId, String messageId) {
        ObservableList<MessageResponse> list = getMessages(channelId);
        int index = findIndex(list, messageId);
        if (index >= 0) {
            MessageResponse existing = list.get(index);
            list.set(index, new MessageResponse(
                existing.id(),
                existing.channelId(),
                existing.guildId(),
                existing.senderId(),
                existing.senderDisplayName(),
                existing.senderAvatarUrl(),
                "",
                existing.type(),
                existing.replyToMessageId(),
                existing.attachments(),
                existing.reactions(),
                existing.createdAt(),
                existing.editedAt(),
                true
            ));
        }
    }

    public void clear() {
        messagesByChannel.clear();
    }

    private int findIndex(ObservableList<MessageResponse> messages, String messageId) {
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).id().equals(messageId)) {
                return i;
            }
        }
        return -1;
    }
}
