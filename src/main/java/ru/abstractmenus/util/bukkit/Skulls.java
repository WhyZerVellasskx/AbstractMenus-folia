
package ru.abstractmenus.util.bukkit;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import ru.abstractmenus.api.Logger;
import ru.abstractmenus.services.ProfileStorage;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Skulls {

    private static final int MAX_CACHE_SIZE = 2048;

    private static final Map<String, ItemStack> customSkulls = new ConcurrentHashMap<>();
    private static final Map<String, ItemStack> playerSkulls = new ConcurrentHashMap<>();

    private Skulls() {
    }

    public static ItemStack getCustomSkull(String texture) {
        if (texture == null || texture.isBlank()) return createSkullItem();

        try {
            String encoded = resolveTexture(texture.trim());

            ItemStack cached = customSkulls.get(encoded);
            if (cached != null) return cached.clone();

            UUID uuid = UUID.nameUUIDFromBytes(
                    ("skull:" + encoded).getBytes(StandardCharsets.UTF_8)
            );

            PlayerProfile profile = Bukkit.createProfile(uuid, null);
            profile.setProperty(new ProfileProperty("textures", encoded));

            ItemStack skull = getCustomSkull(profile);

            if (customSkulls.size() >= MAX_CACHE_SIZE) {
                customSkulls.clear();
            }

            customSkulls.putIfAbsent(encoded, skull.clone());
            return skull;
        } catch (Exception | LinkageError exception) {
            Logger.warning("Cannot create custom skull: " + exception.getMessage());
            return createSkullItem();
        }
    }

    public static ItemStack getCustomSkull(PlayerProfile profile) {
        ItemStack skull = createSkullItem();
        if (profile == null) return skull;

        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta == null) return skull;

        meta.setPlayerProfile(profile);
        skull.setItemMeta(meta);

        return skull;
    }

    public static ItemStack getPlayerSkull(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return createSkullItem();
        }

        ProfileStorage storage = ProfileStorage.instance();
        if (storage == null) return createSkullItem();

        String key = playerName.toLowerCase(Locale.ROOT);
        PlayerProfile profile = storage.getOrLoadProfile(playerName);

        if (profile == null) return createSkullItem();

        if (!profile.hasTextures()) {
            return getCustomSkull(profile);
        }

        ItemStack cached = playerSkulls.get(key);
        if (cached != null) return cached.clone();

        ItemStack skull = getCustomSkull(profile);

        if (playerSkulls.size() >= MAX_CACHE_SIZE) {
            playerSkulls.clear();
        }

        playerSkulls.putIfAbsent(key, skull.clone());
        return skull;
    }

    private static String resolveTexture(String texture) {
        if (texture.regionMatches(true, 0, "base64:", 0, 7)) {
            String encoded = texture.substring(7).trim();
            Base64.getDecoder().decode(encoded);
            return encoded;
        }

        if (isEncodedTexture(texture)) {
            return texture;
        }

        String url;

        if (texture.startsWith("https://") || texture.startsWith("http://")) {
            url = texture;
        } else if (texture.matches("[a-fA-F0-9]{32,128}")) {
            url = "https://textures.minecraft.net/texture/" + texture;
        } else {
            throw new IllegalArgumentException("Unsupported texture format");
        }

        String json = "{\"textures\":{\"SKIN\":{\"url\":\""
                + url
                + "\"}}}";

        return Base64.getEncoder().encodeToString(
                json.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static boolean isEncodedTexture(String value) {
        try {
            byte[] decoded = Base64.getDecoder().decode(value);

            if (decoded.length > 16384) return false;

            String json = new String(decoded, StandardCharsets.UTF_8);

            return json.contains("\"textures\"")
                    && json.contains("\"SKIN\"")
                    && json.contains("\"url\"");
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public static void invalidatePlayerSkull(String playerName) {
        if (playerName == null) return;
        playerSkulls.remove(playerName.toLowerCase(Locale.ROOT));
    }

    public static void clearCache() {
        customSkulls.clear();
        playerSkulls.clear();
    }

    public static ItemStack createSkullItem() {
        return new ItemStack(Material.PLAYER_HEAD);
    }
}
