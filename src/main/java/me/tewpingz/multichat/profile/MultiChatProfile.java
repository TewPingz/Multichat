package me.tewpingz.multichat.profile;

import lombok.*;
import me.tewpingz.multichat.MultiChatPlugin;
import me.tewpingz.multichat.channel.MultiChatChannel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
public class MultiChatProfile {

    private final UUID playerId;
    private String channelId;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private final Set<String> ignoredChannels = new HashSet<>();

    public boolean isIgnoringChannel(String channelId) {
        return this.ignoredChannels.contains(channelId.toLowerCase());
    }

    public void startIgnoringChannel(String channelId) {
        this.addIgnoredChannel(channelId);
        MultiChatPlugin.getInstance().getPersistence().getConnectionAsync().thenAccept(connection -> {
            try {
                PreparedStatement insertIgnore = connection.prepareStatement("INSERT INTO IGNORED_CHANNELS (PLAYER_UUID, CHANNEL) VALUES(?,?)");
                insertIgnore.setString(1, this.playerId.toString());
                insertIgnore.setString(2, channelId.toLowerCase());
                insertIgnore.executeUpdate();
                insertIgnore.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public boolean stopIgnoringChannel(String channelId) {
        if (!this.removeIgnoredChannel(channelId)) {
            return false;
        }
        MultiChatPlugin.getInstance().getPersistence().getConnectionAsync().thenAccept(connection -> {
            try {
                PreparedStatement insertIgnore = connection.prepareStatement("DELETE FROM IGNORED_CHANNELS WHERE CHANNEL=? AND PLAYER_UUID=?");
                insertIgnore.setString(1, channelId.toLowerCase());
                insertIgnore.setString(2, this.playerId.toString());
                insertIgnore.executeUpdate();
                insertIgnore.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        return true;
    }

    protected void loadIgnoredChannels() {
        Connection connection = MultiChatPlugin.getInstance().getPersistence().getConnection();
        try {
            PreparedStatement selectStatement = connection.prepareStatement("SELECT * FROM IGNORED_CHANNELS WHERE PLAYER_UUID=?");
            selectStatement.setString(1, this.playerId.toString());
            ResultSet resultSet = selectStatement.executeQuery();
            while (resultSet.next()) {
                String channelId = resultSet.getString("CHANNEL");
                this.addIgnoredChannel(channelId);
            }
            selectStatement.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    protected CompletableFuture<Void> loadIgnoredChannelsAsync() {
        return CompletableFuture.runAsync(this::loadIgnoredChannels);
    }

    protected void addIgnoredChannel(String channelId) {
        this.ignoredChannels.add(channelId.toLowerCase());
    }

    protected boolean removeIgnoredChannel(String channelId) {
        return this.ignoredChannels.remove(channelId.toLowerCase());
    }

    public MultiChatChannel getChannel() {
        AtomicReference<MultiChatChannel> value = new AtomicReference<>(MultiChatPlugin.getInstance().getChannelManager().getChannel("global").orElse(null));
        MultiChatPlugin.getInstance().getChannelManager().getChannel(this.channelId).ifPresent(value::set);
        return value.get();
    }

    public void saveAsync() {
        MultiChatPlugin.getInstance().getProfileManager().saveProfileAsync(this);
    }
}
