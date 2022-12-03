package me.tewpingz.multichat.channel;

import me.tewpingz.multichat.MultiChatPersistence;
import me.tewpingz.multichat.MultiChatPlugin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class MultiChatChannelManager {

    private final MultiChatPersistence persistence;
    private final Map<String, MultiChatChannel> channelMap;

    public MultiChatChannelManager(MultiChatPersistence persistence) {
        this.persistence = persistence;
        this.channelMap = new HashMap<>();
        this.cacheChannel(new MultiChatChannel("Global", -1));
        this.loadChannels();
    }

    public Optional<MultiChatChannel> getChannel(String name) {
        return Optional.ofNullable(this.channelMap.get(name.toLowerCase()));
    }

    public Collection<MultiChatChannel> getChannels() {
        return Collections.unmodifiableCollection(this.channelMap.values());
    }

    private void loadChannels() {
        try {
            PreparedStatement selectStatement = this.persistence.getConnection().prepareStatement("SELECT * FROM CHANNELS");
            ResultSet resultSet = selectStatement.executeQuery();
            while (resultSet.next()) {
                String channelName = resultSet.getString("CHANNEL_NAME");
                int distance = resultSet.getInt("DISTANCE");
                this.cacheChannel(new MultiChatChannel(channelName, distance));
            }
            selectStatement.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void persistChannel(String channelName, int distance) {
        MultiChatChannel channel = new MultiChatChannel(channelName, distance);
        this.persistence.getConnectionAsync().thenAccept(connection -> {
            try {
                PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO CHANNELS VALUES(?,?)");
                insertStatement.setString(1, channelName);
                insertStatement.setInt(2, distance);
                insertStatement.executeUpdate();
                insertStatement.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        this.cacheChannel(channel);
    }

    public void deleteChannel(String channelName) {
        MultiChatChannel channel = this.channelMap.remove(channelName.toLowerCase());
        if (channel != null) {
            this.persistence.getConnectionAsync().thenAccept(connection -> {
                try {
                    PreparedStatement deleteStatement = connection.prepareStatement("DELETE FROM REGION_WHITELIST WHERE CHANNEL_NAME=?");
                    deleteStatement.executeUpdate();
                    deleteStatement.close();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private void cacheChannel(MultiChatChannel channel) {
        this.channelMap.put(channel.getChannelName().toLowerCase(), channel);
    }
}
