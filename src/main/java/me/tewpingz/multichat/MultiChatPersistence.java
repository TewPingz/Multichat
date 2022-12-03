package me.tewpingz.multichat;

import com.mysql.cj.jdbc.MysqlDataSource;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

public class MultiChatPersistence {

    private final MultiChatPlugin plugin;
    private final String host, database, username, password;
    private final int port;

    private Connection connection;

    /**
     * The constructor for the persistence of the region plugin
     * @param plugin the plugin instance
     */
    public MultiChatPersistence(MultiChatPlugin plugin) {
        this.plugin = plugin;
        this.plugin.getConfig().options().copyDefaults();
        this.plugin.saveDefaultConfig();
        FileConfiguration configuration = this.plugin.getConfig();
        ConfigurationSection section = configuration.getConfigurationSection("mysql");
        this.host = section.getString("host");
        this.port = section.getInt("port");
        this.database = section.getString("database");
        this.username = section.getString("username");
        this.password = section.getString("password");
    }

    /**
     * A function to get the connection on an asynchronous thread to ensure the server is not affected by the connection
     * @return a completable future with the connection
     */
    public CompletableFuture<Connection> getConnectionAsync() {
        return CompletableFuture.supplyAsync(this::attemptToGetConnection);
    }

    /**
     * A function to get a connection on the sync thread
     * @return the connection instance
     */
    public Connection getConnection() {
        return this.attemptToGetConnection();
    }

    /**
     * Function to shut down the MySQL database
     * @throws SQLException when something wrong happens with the MySQL connection
     */
    public void shutdown() throws SQLException {
        this.connection.close();
    }

    private Connection attemptToGetConnection() {
        try {
            if (!this.isConnectionValid()) {
                this.connect();
            }
        } catch (Exception e) {
            this.connect();
            e.printStackTrace();
        }
        return this.connection;
    }

    private boolean isConnectionValid() throws SQLException {
        return this.connection != null && !this.connection.isClosed() && this.connection.isValid(10);
    }

    private void connect() {
        try {
            this.plugin.getLogger().info("Attempting to connect to MySQL database...");

            MysqlDataSource dataSource = new MysqlDataSource();
            dataSource.setUser(this.username);
            dataSource.setPassword(this.password);
            dataSource.setAutoReconnect(true);
            dataSource.setUrl("jdbc:mysql://%s:%s/%s".formatted(this.host, this.port, this.database));
            this.connection = dataSource.getConnection();

            PreparedStatement profilesQuery = this.connection.prepareStatement("CREATE TABLE IF NOT EXISTS PROFILES("
                    + "PLAYER_UUID VARCHAR(36) NOT NULL,"
                    + "SELECTED_CHANNEL VARCHAR(36) NOT NULL,"
                    + "PRIMARY KEY (PLAYER_UUID)"
                    + ")");
            profilesQuery.executeUpdate();
            profilesQuery.close();

            PreparedStatement channelsQuery = this.connection.prepareStatement("CREATE TABLE IF NOT EXISTS CHANNELS("
                    + "CHANNEL_NAME VARCHAR(36) NOT NULL,"
                    + "DISTANCE INTEGER NOT NULL,"
                    + "PRIMARY KEY (CHANNEL_NAME)"
                    + ")");
            channelsQuery.executeUpdate();
            channelsQuery.close();

            PreparedStatement ignoredChannelQuery = this.connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS IGNORED_CHANNELS("
                    + "ENTRY_ID INTEGER  NOT NULL AUTO_INCREMENT,"
                    + "PLAYER_UUID VARCHAR(36) NOT NULL,"
                    + "CHANNEL VARCHAR(36) NOT NULL,"
                    + "PRIMARY KEY (ENTRY_ID)"
                    + ")");
            ignoredChannelQuery.executeUpdate();
            ignoredChannelQuery.close();

            this.plugin.getLogger().info("Connected to MySQL database");
        } catch (SQLException e) {
            this.plugin.getLogger().info("Failed to make a connection to the MySQL database.");
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> this.plugin.getServer().shutdown());
            e.printStackTrace();
        }
    }
}
