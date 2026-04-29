package com.doan.frontend.store;

import com.doan.frontend.model.guild.GuildResponse;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class GuildStore {
    private final ObservableList<GuildResponse> guildList = FXCollections.observableArrayList();
    private final ObjectProperty<GuildResponse> selectedGuild = new SimpleObjectProperty<>();

    public ObservableList<GuildResponse> getGuildList() {
        return guildList;
    }

    public ObjectProperty<GuildResponse> selectedGuildProperty() {
        return selectedGuild;
    }

    public GuildResponse getSelectedGuild() {
        return selectedGuild.get();
    }

    public void setSelectedGuild(GuildResponse guild) {
        selectedGuild.set(guild);
    }

    public void clear() {
        guildList.clear();
        selectedGuild.set(null);
    }
}
