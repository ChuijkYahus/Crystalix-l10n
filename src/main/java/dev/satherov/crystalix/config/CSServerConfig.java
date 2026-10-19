package dev.satherov.crystalix.config;

import lombok.Getter;

import dev.satherov.crystalix.config.annotation.Config;
import dev.satherov.crystalix.config.annotation.ConfigVal;

import net.neoforged.fml.config.ModConfig;

import java.util.List;

@Config(ModConfig.Type.SERVER)
public class CSServerConfig {
    
    @ConfigVal(
            name = "disabled_properties",
            comment = """
                Wand properties that can't be changed on a wand. Only operators can apply them to blocks.
                Valid values: "invisible", "clear", "waterloggable", "reinforced", "shadeless", "redstone", "conductor", "ghost", "light", "color", "apply_colorless"
                """
    )
    @ConfigVal.List
    private static @Getter List<String> DisabledProperties = List.of();
}
