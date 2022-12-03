package me.tewpingz.multichat.profile;

import lombok.RequiredArgsConstructor;
import me.tewpingz.multichat.MultiChatPersistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@RequiredArgsConstructor
public class MultiChatProfileManager {

    private final MultiChatPersistence persistence;
    private final Map<UUID, MultiChatProfile> profileMap = new HashMap<>();
    private final Executor asyncExecutor = Executors.newSingleThreadExecutor();

    /**
     * A function to load a profile synchronously
     * @param playerId the player id
     */
    public void loadProfile(UUID playerId) {
        if (this.profileMap.containsKey(playerId)) {
            return;
        }

        MultiChatProfile profile = new MultiChatProfile(playerId, "Global");
        Connection connection = this.persistence.getConnection();
        try {
            PreparedStatement selectStatement = connection.prepareStatement("SELECT * FROM PROFILES WHERE PLAYER_UUID=?");
            selectStatement.setString(1, playerId.toString());
            ResultSet resultSet = selectStatement.executeQuery();
            if (resultSet.next()) {
                profile.setChannelId(resultSet.getString("SELECTED_CHANNEL"));
                profile.loadIgnoredChannels();
            } else {
                PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO PROFILES VALUES(?,?)");
                insertStatement.setString(1, playerId.toString());
                insertStatement.setString(2, profile.getChannelId());
                insertStatement.executeUpdate();
                insertStatement.close();
            }
            selectStatement.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        this.profileMap.put(playerId, profile);
    }

    /**
     * A function to load a profile asynchronously
     * @param playerId the player id of the owner of the profile
     * @return a completable future of the void
     */
    public CompletableFuture<Void> loadProfileAsync(UUID playerId) {
        return CompletableFuture.runAsync(() -> this.loadProfile(playerId), this.asyncExecutor);
    }

    /**
     * A function to get the profile of a player
     * @param playerId the player id of the owner of the profile
     * @return an optional containing the profile
     */
    public Optional<MultiChatProfile> getProfile(UUID playerId) {
        return Optional.ofNullable(this.profileMap.get(playerId));
    }

    /**
     * A function to save the profile into the SQL database
     * @param profile the profile to save
     */
    public void saveProfile(MultiChatProfile profile) {
        try {
            PreparedStatement updateStatement = this.persistence.getConnection().prepareStatement("UPDATE PROFILES SET SELECTED_CHANNEL=? WHERE PLAYER_UUID=?");
            updateStatement.setString(1, profile.getChannelId());
            updateStatement.setString(2, profile.getPlayerId().toString());
            updateStatement.executeUpdate();
            updateStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * A function to unload the profile from the map
     * @param uniqueId
     */
    public void unloadProfile(UUID uniqueId) {
        this.profileMap.remove(uniqueId);
    }

    /**
     * A function to save the profile asynchronously
     * @param profile the profile
     * @return a completable future to track the completion of the save
     */
    public CompletableFuture<Void> saveProfileAsync(MultiChatProfile profile) {
        return CompletableFuture.runAsync(() -> this.saveProfile(profile), this.asyncExecutor);
    }
}
