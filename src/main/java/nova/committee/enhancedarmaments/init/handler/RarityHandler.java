package nova.committee.enhancedarmaments.init.handler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import nova.committee.enhancedarmaments.Static;
import nova.committee.enhancedarmaments.core.Rarity;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RarityHandler {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("enhancedarmaments/rarity");

    private RarityHandler() {
    }

    public static void loadRarities() {
        try {
            writeDefaultRarityFiles();
            Rarity.clear();
            loadFiles();
            Static.LOGGER.info("Loaded {} custom rarity(s)", Rarity.RARITIES.size());
            if (!Rarity.RARITIES.isEmpty() && !Rarity.hasRandomRarities()) {
                Static.LOGGER.warn("No enabled rarity has a positive finite weight; new items will keep the default rarity");
            }
        } catch (IOException e) {
            Static.LOGGER.error("Failed to load rarities", e);
        }
    }

    private static void writeDefaultRarityFiles() throws IOException {
        Files.createDirectories(DIR);
        if (hasJson()) {
            return;
        }
        write("basic", "white", 0xFFFFFF, 0.5D, 0D, true);
        write("uncommon", "dark_green", 0x00AA00, 0.18D, 0.155D, true);
        write("rare", "aqua", 0x55FFFF, 0.1D, 0.305D, true);
        write("ultra_rare", "dark_purple", 0xAA00AA, 0.05D, 0.38D, true);
        write("legendary", "gold", 0xFFAA00, 0.02D, 0.57D, true);
        write("archaic", "red", 0xFF55FF, 0.01D, 0.81D, true);
    }

    private static boolean hasJson() throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(DIR, "*.json")) {
            return stream.iterator().hasNext();
        }
    }

    private static void write(String name, String color, int hex, double weight, double effect, boolean enabled) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        json.addProperty("color", color);
        json.addProperty("hex", hex);
        json.addProperty("weight", weight);
        json.addProperty("effect", effect);
        json.addProperty("enabled", enabled);
        Path file = DIR.resolve(name + ".json");
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(json, writer);
        }
    }

    private static void loadFiles() throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(DIR, "*.json")) {
            for (Path file : stream) {
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    Rarity rarity = Rarity.loadFromJson(json);
                    if (rarity.isEnabled()) {
                        if (Double.isFinite(rarity.getWeight()) && rarity.getWeight() > 0.0D) {
                            Rarity.RANDOM_RARITIES.add(rarity.getWeight(), rarity);
                        } else {
                            Static.LOGGER.warn("Rarity {} has a non-positive or non-finite weight and will not be selected randomly",
                                    rarity.getName());
                        }
                        Rarity.RARITIES.add(rarity);
                    }
                } catch (Exception e) {
                    Static.LOGGER.error("Failed to load rarity {}", file.getFileName(), e);
                }
            }
        }
    }
}
