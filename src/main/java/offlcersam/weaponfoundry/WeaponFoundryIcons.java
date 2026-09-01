package offlcersam.weaponfoundry;

import illuminatus.core.graphics.Texture;
import items.Item;
import mods.ModLogger;
import offlcersam.weaponfoundry.json.JsonValue;
import org.newdawn.slick.opengl.TextureLoader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom weapon/ammo icons, loaded directly from PNG files dropped next to a weapon/ammo JSON.
 */
public final class WeaponFoundryIcons {

    // Limitation warning: if other mods use the same starting base for their own similar system, indices could collide.
    // Until a shared allocation scheme exists between mods, this only has to avoid vanilla's own icon range.
    public static final int CUSTOM_ICON_BASE = 1024;

    private static int nextIconIndex = CUSTOM_ICON_BASE;

    // Dedupes repeated references to the same image file (e.g. a weapon and its ammo sharing an icon)
    // so it's only loaded onto the GPU once, keyed by the image's normalized absolute path.
    private static final Map<String, Integer> PATH_TO_ICON_INDEX = new HashMap<>();

    private static final Map<Integer, Texture> ICON_TEXTURES = new HashMap<>();

    private WeaponFoundryIcons() {
    }

    /**
     * Resolves a weapon/ammo JSON's "icon" field, which is either:
     * - a NUMBER: a plain vanilla spritesheet index, returned as-is.
     * - a STRING: a filename (or relative path) of a PNG sitting next to the JSON in the same mod folder,
     *   loaded on demand and assigned a custom icon index.
     */
    public static int resolveIcon(JsonValue iconValue, Path baseDir) {
        if (iconValue.type() == JsonValue.Type.NUMBER) {
            return iconValue.asInt();
        }

        if (iconValue.type() != JsonValue.Type.STRING) {
            throw new JsonValue.JsonException("\"icon\" must be a number (vanilla index) or a string (image path)");
        }

        return registerCustomIcon(baseDir.resolve(iconValue.asString()));
    }

    /** Loads (or reuses) a custom icon texture from disk, returning the icon index that identifies it. */
    private static int registerCustomIcon(Path imageFile) {
        String key = imageFile.toAbsolutePath().normalize().toString();

        Integer existing = PATH_TO_ICON_INDEX.get(key);
        if (existing != null) {
            return existing;
        }

        if (!Files.isRegularFile(imageFile)) {
            throw new IllegalArgumentException("Custom icon image not found: " + imageFile);
        }

        Texture texture;

        try (InputStream in = Files.newInputStream(imageFile)) {
            BufferedImage img = ImageIO.read(in);

            if (img == null) {
                throw new IllegalArgumentException("Could not read custom icon image (unsupported/corrupt PNG): " + imageFile);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "PNG", baos);

            org.newdawn.slick.opengl.Texture slickTex =
                    TextureLoader.getTexture("PNG", new ByteArrayInputStream(baos.toByteArray()));

            texture = new Texture(slickTex);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to load custom icon image " + imageFile + ": " + e, e);
        }

        int iconIndex = nextIconIndex++;
        ICON_TEXTURES.put(iconIndex, texture);
        PATH_TO_ICON_INDEX.put(key, iconIndex);

        ModLogger.log("[WeaponFoundry] Loaded custom icon \"" + imageFile.getFileName() + "\" as icon index " + iconIndex);

        return iconIndex;
    }

    /** Whether this item's icon points at a custom-loaded texture rather than the vanilla spritesheet. */
    public static boolean isCustomIcon(Item item) {
        return ICON_TEXTURES.containsKey(item.getIcon());
    }

    /** The full-image Texture for this item's icon, or null if it's not one of ours (vanilla icon). */
    public static Texture getTexture(Item item) {
        return ICON_TEXTURES.get(item.getIcon());
    }
}