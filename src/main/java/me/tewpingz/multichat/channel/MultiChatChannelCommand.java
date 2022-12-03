package me.tewpingz.multichat.channel;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import me.tewpingz.multichat.MultiChatPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

@CommandPermission("multichat.admin")
@CommandAlias("chatchannel|mcchatchannel")
public class MultiChatChannelCommand extends BaseCommand {

    @Default
    @HelpCommand
    public void onHelp(CommandSender sender, CommandHelp help) {
        help.showHelp();
    }

    @Subcommand("create")
    @Syntax("<channelName> <distance>")
    public void create(CommandSender sender, String channelName, int distance) {
        boolean exists = MultiChatPlugin.getInstance().getChannelManager().getChannel(channelName).isPresent();

        if (exists) {
            sender.sendMessage(ChatColor.RED + "There is already a channel with that name!");
            return;
        }

        MultiChatPlugin.getInstance().getChannelManager().persistChannel(channelName, distance);
        sender.sendMessage(ChatColor.GREEN + "You have successfully created a channel named %s".formatted(channelName));
    }

    @Subcommand("delete")
    @Syntax("<channelName>")
    @CommandCompletion("@channels")
    public void delete(CommandSender sender, MultiChatChannel channel) {
        MultiChatPlugin.getInstance().getChannelManager().deleteChannel(channel.getChannelName());
        sender.sendMessage(ChatColor.GREEN + "You have removed the channel named %s".formatted(channel.getChannelName()));
    }
}
