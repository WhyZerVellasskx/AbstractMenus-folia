
package ru.abstractmenus.services;

import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import ru.abstractmenus.api.Logger;
import ru.abstractmenus.util.StringUtil;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ProfileStorage implements Listener {

    private static ProfileStorage instance;

    public static final PlayerProfile DEF_PROFILE =
            Bukkit.createProfile(UUID.randomUUID(), StringUtil.generateRandom(16));

    private final Map<String, PlayerProfile> profiles = new ConcurrentHashMap<>();

    public ProfileStorage() {
        instance = this;
    }

    public PlayerProfile getProfile(String playerName) {
        return profiles.get(playerName.toLowerCase(Locale.ROOT));
    }

    public void add(String playerName, PlayerProfile profile) {
        if (profile == null) return;
        profiles.put(playerName.toLowerCase(Locale.ROOT), profile);
    }

    public void remove(Player player) {
        profiles.remove(player.getName().toLowerCase(Locale.ROOT));
    }

    public static ProfileStorage instance() {
        return instance;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        String key = playerName.toLowerCase(Locale.ROOT);

        PlayerProfile profile;

        try {
            profile = player.getPlayerProfile();
            add(playerName, profile);

            if (profile.hasTextures()) return;
        } catch (Exception | LinkageError exception) {
            Logger.warning("Cannot fetch profile for " + playerName + ": " + exception.getMessage());
            profile = Bukkit.createProfileExact(null, playerName);
            add(playerName, profile);
        }

        PlayerProfile cachedProfile = profile;

        try {
            Bukkit.createProfileExact(null, playerName).update().whenComplete((loadedProfile, exception) -> {
                if (exception != null) {
                    Logger.warning("Cannot load skin for " + playerName + ": " + exception.getMessage());
                    return;
                }

                if (loadedProfile == null || !loadedProfile.hasTextures()) return;

                profiles.computeIfPresent(key, (name, currentProfile) ->
                        currentProfile == cachedProfile ? loadedProfile : currentProfile);
            });
        } catch (Exception | LinkageError exception) {
            Logger.warning("Cannot update skin for " + playerName + ": " + exception.getMessage());
        }
    }
}
