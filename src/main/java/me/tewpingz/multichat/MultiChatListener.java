package me.tewpingz.multichat;

import io.papermc.paper.event.player.AsyncChatEvent;
import lombok.RequiredArgsConstructor;
import me.tewpingz.multichat.channel.MultiChatChannel;
import me.tewpingz.multichat.profile.MultiChatProfile;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@RequiredArgsConstructor
public class MultiChatListener implements Listener {

    private final MultiChatPlugin plugin;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        this.plugin.getProfileManager().loadProfileAsync(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.plugin.getProfileManager().unloadProfile(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        this.plugin.getProfileManager().getProfile(player.getUniqueId()).ifPresent(profile -> {
            MultiChatChannel channel = profile.getChannel();
            int range = channel.getRange();

            // Ignore it because its global
            if (range == -1) {
                return;
            }

            event.viewers().removeIf(audience -> {
                if (audience instanceof Player) {
                    Player target = (Player) audience;

                    MultiChatProfile targetProfile = this.plugin.getProfileManager().getProfile(target.getUniqueId()).orElse(null);
                    if (targetProfile != null && targetProfile.isIgnoringChannel(channel.getChannelName())) {
                        return true;
                    }

                    Location from = player.getLocation();
                    Location to = player.getLocation();

                    // Since they are in different worlds we can already assume they are not close
                    if (from.getWorld().getUID() != to.getWorld().getUID()) {
                        return true;
                    }

                    return range < player.getLocation().distance(target.getLocation());
                }
                return false;
            });
        });
    }
}
