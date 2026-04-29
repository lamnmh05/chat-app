package com.doan.frontend.store;

import com.doan.frontend.model.channel.ChannelResponse;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ChannelStore {
    private final ObservableList<ChannelResponse> channelList = FXCollections.observableArrayList();
    private final ObjectProperty<ChannelResponse> selectedChannel = new SimpleObjectProperty<>();

    public ObservableList<ChannelResponse> getChannelList() {
        return channelList;
    }

    public ObjectProperty<ChannelResponse> selectedChannelProperty() {
        return selectedChannel;
    }

    public ChannelResponse getSelectedChannel() {
        return selectedChannel.get();
    }

    public void setSelectedChannel(ChannelResponse channel) {
        selectedChannel.set(channel);
    }

    public void clear() {
        channelList.clear();
        selectedChannel.set(null);
    }
}
