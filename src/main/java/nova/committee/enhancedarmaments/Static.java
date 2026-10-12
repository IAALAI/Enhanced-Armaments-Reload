package nova.committee.enhancedarmaments;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Static {
    public static final String MODID = "enhancedarmaments";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    private Static() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
