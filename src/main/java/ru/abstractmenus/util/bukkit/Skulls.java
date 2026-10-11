
package ru.abstractmenus.util.bukkit;

import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerTextures;
import ru.abstractmenus.api.Logger;
import ru.abstractmenus.services.ProfileStorage;

import java.net.URI;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Skulls {

    private static final Set<String> loading = ConcurrentHashMap.newKeySet();

    private Skulls() {
    }

    public static ItemStack getCustomSkull(String url) {
        if (url == null || url.isBlank()) return createSkullItem();

        try {
            String textureUrl = url.contains("://") ? url : "https://textures.minecraft.net/texture/" + url;

            PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), null);
            PlayerTextures textures = profile.getTextures();

            textures.setSkin(URI.create(textureUrl).toURL());
            profile.setTextures(textures);

            return getCustomSkull(profile);
        } catch (Exception | LinkageError exception) {
            Logger.warning("Cannot create custom skull: " + exception.getMessage());
            return createSkullItem();
        }
    }

    public static ItemStack getCustomSkull(PlayerProfile profile) {
        ItemStack head = createSkullItem();
        if (profile == null) return head;

        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return head;

        meta.setPlayerProfile(profile);
        head.setItemMeta(meta);

        return head;
    }

    public static ItemStack getPlayerSkull(String playerName) {
        ProfileStorage storage = ProfileStorage.instance();
        PlayerProfile profile = storage.getProfile(playerName);

        if (profile == null) {
            profile = Bukkit.createProfileExact(null, playerName);
            storage.add(playerName, profile);
        }

        if (!profile.hasTextures()) {
            loadProfileAsync(playerName, profile);
        }

        return getCustomSkull(profile);
    }

    private static void loadProfileAsync(String playerName, PlayerProfile profile) {
        String key = playerName.toLowerCase(Locale.ROOT);
        if (!loading.add(key)) return;

        try {
            profile.update().whenComplete((loadedProfile, exception) -> {
                try {
                    if (exception != null) {
                        Logger.warning("Cannot load skin for " + playerName + ": " + exception.getMessage());
                        return;
                    }

                    if (loadedProfile == null || !loadedProfile.hasTextures()) return;

                    ProfileStorage storage = ProfileStorage.instance();

                    if (storage.getProfile(playerName) == profile) {
                        storage.add(playerName, loadedProfile);
                    }
                } finally {
                    loading.remove(key);
                }
            });
        } catch (Exception | LinkageError exception) {
            loading.remove(key);
            Logger.warning("Cannot update profile for " + playerName + ": " + exception.getMessage());
        }
    }

    public static ItemStack createSkullItem() {
        return new ItemStack(Material.PLAYER_HEAD);
    }
}
