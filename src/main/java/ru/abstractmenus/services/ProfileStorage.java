
package ru.abstractmenus.services;

import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import ru.abstractmenus.api.Logger;
import ru.abstractmenus.util.StringUtil;
import ru.abstractmenus.util.bukkit.Skulls;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class ProfileStorage implements Listener {

    private static final long RETRY_DELAY = TimeUnit.MINUTES.toMillis(1);

    private static volatile ProfileStorage instance;

    public static final PlayerProfile DEF_PROFILE =
            Bukkit.createProfile(UUID.randomUUID(), StringUtil.generateRandom(16));

    private final Plugin plugin;
    private final Map<String, PlayerProfile> profiles = new ConcurrentHashMap<>();
    private final Map<String, Long> retryAfter = new ConcurrentHashMap<>();
    private final Set<String> loading = ConcurrentHashMap.newKeySet();

    public ProfileStorage() {
        plugin = JavaPlugin.getProvidingPlugin(ProfileStorage.class);
        instance = this;
    }

    public static ProfileStorage instance() {
        return instance;
    }

    public PlayerProfile getProfile(String playerName) {
        if (playerName == null || playerName.isBlank()) return null;
        return profiles.get(normalize(playerName));
    }

    public PlayerProfile getOrLoadProfile(String playerName) {
        if (playerName == null || playerName.isBlank()) return null;

        String key = normalize(playerName);

        PlayerProfile profile = profiles.computeIfAbsent(
                key,
                ignored -> Bukkit.createProfileExact(null, playerName)
        );

        if (!profile.hasTextures()) {
            loadProfileAsync(playerName, profile);
        }

        return profile;
    }

    public void add(String playerName, PlayerProfile profile) {
        if (playerName == null || playerName.isBlank() || profile == null) return;

        String key = normalize(playerName);
        boolean[] shouldRefresh = {false};

        profiles.compute(key, (ignored, previous) -> {
            if (previous != null && previous.hasTextures() && !profile.hasTextures()) {
                return previous;
            }

            if (profile.hasTextures() && (previous == null || !previous.hasTextures())) {
                shouldRefresh[0] = true;
            }

            return profile;
        });

        Skulls.invalidatePlayerSkull(playerName);

        if (shouldRefresh[0]) {
            refreshOpenSkulls(playerName, profile);
        }
    }

    public void remove(Player player) {
        if (player == null) return;

        String key = normalize(player.getName());

        profiles.remove(key);
        retryAfter.remove(key);
        Skulls.invalidatePlayerSkull(player.getName());
    }

    private void loadProfileAsync(String playerName, PlayerProfile profile) {
        String key = normalize(playerName);

        if (System.currentTimeMillis() < retryAfter.getOrDefault(key, 0L)) {
            return;
        }

        if (!loading.add(key)) return;

        try {
            profile.update().whenComplete((updatedProfile, exception) -> {
                try {
                    if (exception != null) {
                        retryAfter.put(key, System.currentTimeMillis() + RETRY_DELAY);

                        Logger.warning(
                                "Cannot load skin for " + playerName + ": "
                                        + exception.getMessage()
                        );

                        return;
                    }

                    if (updatedProfile == null || !updatedProfile.hasTextures()) {
                        retryAfter.put(key, System.currentTimeMillis() + RETRY_DELAY);
                        return;
                    }

                    retryAfter.remove(key);
                    addLoadedProfile(playerName, updatedProfile);
                } finally {
                    loading.remove(key);
                }
            });
        } catch (Exception | LinkageError exception) {
            loading.remove(key);
            retryAfter.put(key, System.currentTimeMillis() + RETRY_DELAY);

            Logger.warning(
                    "Cannot update profile for " + playerName + ": "
                            + exception.getMessage()
            );
        }
    }

    private void addLoadedProfile(String playerName, PlayerProfile profile) {
        String key = normalize(playerName);
        boolean[] shouldRefresh = {false};

        profiles.compute(key, (ignored, previous) -> {
            if (previous != null && previous.hasTextures()) {
                return previous;
            }

            shouldRefresh[0] = true;
            return profile;
        });

        if (!shouldRefresh[0]) return;

        Skulls.invalidatePlayerSkull(playerName);
        refreshOpenSkulls(playerName, profile);
    }

    private void refreshOpenSkulls(String playerName, PlayerProfile profile) {
        if (!plugin.isEnabled()) return;

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            viewer.getScheduler().run(
                    plugin,
                    task -> refreshInventory(viewer, playerName, profile),
                    null
            );
        }
    }

    private void refreshInventory(
            Player viewer,
            String playerName,
            PlayerProfile profile
    ) {
        if (!viewer.isOnline()) return;

        Inventory inventory = viewer.getOpenInventory().getTopInventory();

        if (inventory.getType() != InventoryType.CHEST) return;

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);

            if (item == null || item.getType() != Material.PLAYER_HEAD) {
                continue;
            }

            if (!(item.getItemMeta() instanceof SkullMeta meta)) {
                continue;
            }

            PlayerProfile current = meta.getPlayerProfile();

            if (current == null || current.hasTextures()) {
                continue;
            }

            String currentName = current.getName();

            if (currentName == null || !currentName.equalsIgnoreCase(playerName)) {
                continue;
            }

            ItemStack updatedItem = item.clone();
            SkullMeta updatedMeta = (SkullMeta) updatedItem.getItemMeta();

            if (updatedMeta == null) continue;

            updatedMeta.setPlayerProfile(profile);
            updatedItem.setItemMeta(updatedMeta);

            inventory.setItem(slot, updatedItem);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();

        try {
            PlayerProfile profile = player.getPlayerProfile();
            add(playerName, profile);
        } catch (Exception | LinkageError exception) {
            Logger.warning(
                    "Cannot fetch profile for " + playerName + ": "
                            + exception.getMessage()
            );
        }

        getOrLoadProfile(playerName);
    }

    private static String normalize(String playerName) {
        return playerName.toLowerCase(Locale.ROOT);
    }
}
