package me.tewpingz.multichat;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import me.tewpingz.multichat.channel.MultiChatChannel;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("multichat|mc")
public class MultiChatCommand extends BaseCommand {

    @HelpCommand
    @Default
    public void doHelp(CommandSender sender, CommandHelp help) {
        help.showHelp();
    }

    @Subcommand("mode")
    @CommandCompletion("@channels")
    public void onMode(Player player, MultiChatChannel channel) {
        MultiChatPlugin.getInstance().getProfileManager().getProfile(player.getUniqueId()).ifPresent(profile -> {
            profile.setChannelId(channel.getChannelName().toLowerCase());
            profile.saveAsync();
            player.sendMessage(ChatColor.GREEN + "You have successfully changed your chat mode to %s".formatted(channel.getChannelName()));
        });
    }

    @Subcommand("ignore")
    @CommandCompletion("@channels")
    public void onIgnore(Player player, MultiChatChannel channel) {
        MultiChatPlugin.getInstance().getProfileManager().getProfile(player.getUniqueId()).ifPresent(profile -> {
            if (profile.isIgnoringChannel(channel.getChannelName())) {
                player.sendMessage(ChatColor.RED + "You are already ignoring that channel!");
                return;
            }
            profile.startIgnoringChannel(channel.getChannelName());
            player.sendMessage(ChatColor.GREEN + "You are now ignoring that channel!");
        });
    }

    @Subcommand("uningore")
    @CommandCompletion("@channels")
    public void onUnIgnore(Player player, MultiChatChannel channel) {
        MultiChatPlugin.getInstance().getProfileManager().getProfile(player.getUniqueId()).ifPresent(profile -> {
            if (profile.stopIgnoringChannel(channel.getChannelName())) {
                player.sendMessage(ChatColor.GREEN + "You have uningored that channel");
            } else {
                player.sendMessage(ChatColor.RED + "You were not ignoring that channel");
            }
        });
    }
}
