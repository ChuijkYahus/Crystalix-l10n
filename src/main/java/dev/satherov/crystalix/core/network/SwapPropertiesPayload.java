package dev.satherov.crystalix.core.network;

import lombok.extern.slf4j.Slf4j;

import dev.satherov.crystalix.Crystalix;
import dev.satherov.crystalix.common.block.CrystalixGlass;
import dev.satherov.crystalix.common.block.CrystalixGlassTile;
import dev.satherov.crystalix.common.item.CrystalixWand;
import dev.satherov.crystalix.common.properties.CSProperties;
import dev.satherov.crystalix.common.properties.IProperty;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.NotNull;

@Slf4j
public record SwapPropertiesPayload(BlockPos pos) implements CustomPacketPayload {
    
    public static final StreamCodec<RegistryFriendlyByteBuf, SwapPropertiesPayload> STREAM_CODEC = CustomPacketPayload.codec(
            SwapPropertiesPayload::encode,
            SwapPropertiesPayload::new
    );
    
    public static final Type<SwapPropertiesPayload> TYPE = new Type<>(Crystalix.rl("swap_properties"));
    
    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return SwapPropertiesPayload.TYPE;
    }
    
    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.pos);
    }
    
    private SwapPropertiesPayload(RegistryFriendlyByteBuf buffer) {
        this(buffer.readBlockPos());
    }
    
    public static void handle(final SwapPropertiesPayload message, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.flow().isServerbound() && ctx.player() instanceof ServerPlayer player) {
                ServerLevel level = player.serverLevel();
                ItemStack wand = CrystalixWand.find(player);
                if (wand.isEmpty()) return;
                BlockPos pos = message.pos();
                BlockState state = level.getBlockState(pos);
                if (!(state.getBlock() instanceof CrystalixGlass)) return;
                BlockEntity entity = level.getBlockEntity(pos);
                CSProperties properties = CSProperties.of(wand);
                
                SwapPropertiesPayload.pick(properties, CSProperties.SHADELESS, state.getValue(CrystalixGlass.SHADELESS));
                SwapPropertiesPayload.pick(properties, CSProperties.INVISIBLE, state.getValue(CrystalixGlass.INVISIBLE));
                SwapPropertiesPayload.pick(properties, CSProperties.CLEAR, state.getValue(CrystalixGlass.TRANSPARENT));
                SwapPropertiesPayload.pick(properties, CSProperties.LIGHT, state.getValue(CrystalixGlass.LIGHT));
                SwapPropertiesPayload.pick(properties, CSProperties.GHOST, state.getValue(CrystalixGlass.GHOST));
                
                if (entity instanceof CrystalixGlassTile tile) {
                    SwapPropertiesPayload.pick(properties, CSProperties.CONDUCTOR, tile.isConductor());
                    SwapPropertiesPayload.pick(properties, CSProperties.REDSTONE, tile.isRedstone());
                    SwapPropertiesPayload.pick(properties, CSProperties.REINFORCED, tile.isReinforced());
                    SwapPropertiesPayload.pick(properties, CSProperties.WATERLOGGABLE, tile.isWaterloggable());
                    SwapPropertiesPayload.pick(properties, CSProperties.COLOR, tile.getColor());
                }
                
                player.getInventory().setChanged();
            }
        }).exceptionally(e -> {
            SwapPropertiesPayload.log.error("Failed to pick wand properties from BlockState", e);
            return null;
        });
    }
    
    @SuppressWarnings("unchecked")
    private static <VALUE> void pick(CSProperties properties, ResourceLocation location, VALUE value) {
        if (!CSProperties.isEditable(location)) {
            return;
        }
        
        ((IProperty<VALUE>) properties.properties().get(location)).set(value);
    }
}
