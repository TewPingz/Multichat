package me.tewpingz.multichat;

import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.PaperCommandManager;
import lombok.Getter;
import me.tewpingz.multichat.channel.MultiChatChannel;
import me.tewpingz.multichat.channel.MultiChatChannelCommand;
import me.tewpingz.multichat.channel.MultiChatChannelManager;
import me.tewpingz.multichat.profile.MultiChatProfileManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.stream.Collectors;

@Getter
public final class MultiChatPlugin extends JavaPlugin {

    @Getter
    private static MultiChatPlugin instance;

    private MultiChatPersistence persistence;
    private MultiChatChannelManager channelManager;
    private MultiChatProfileManager profileManager;

    @Override
    public void onEnable() {
        instance = this;
        this.persistence = new MultiChatPersistence(this);
        this.channelManager = new MultiChatChannelManager(this.persistence);
        this.profileManager = new MultiChatProfileManager(this.persistence);
        this.getServer().getPluginManager().registerEvents(new MultiChatListener(this), this);
        this.registerCommands();
    }

    @Override
    public void onDisable() {
        try {
            this.persistence.shutdown();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void registerCommands() {
        PaperCommandManager commandManager = new PaperCommandManager(this);
        commandManager.enableUnstableAPI("help");

        commandManager.getCommandContexts().registerContext(MultiChatChannel.class, context -> {
            String argument = context.popFirstArg();
            MultiChatChannel channel = this.channelManager.getChannel(argument).orElse(null);
            if (channel == null) {
                throw new InvalidCommandArgument("That is not a valid channel");
            }
            return channel;
        });

        commandManager.getCommandCompletions().registerAsyncCompletion("channels", context ->
                this.getChannelManager().getChannels().stream().map(MultiChatChannel::getChannelName).collect(Collectors.toList()));

        commandManager.registerCommand(new MultiChatChannelCommand());
        commandManager.registerCommand(new MultiChatCommand());
    }
}
