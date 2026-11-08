package dev.sigmachan.companions.network;

import dev.sigmachan.companions.registry.ModRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** Sent only to the interacting player when a social gesture actually begins. */
public record GestureS2C(int entityId, int gestureId, int duration) implements CustomPayload {
    public static final Id<GestureS2C> ID = new Id<>(ModRegistry.id("gesture"));
    public static final PacketCodec<RegistryByteBuf, GestureS2C> CODEC = CustomPayload.codecOf(GestureS2C::write, GestureS2C::new);
    private GestureS2C(RegistryByteBuf b) { this(b.readVarInt(), b.readVarInt(), b.readVarInt()); }
    private void write(RegistryByteBuf b) { b.writeVarInt(entityId); b.writeVarInt(gestureId); b.writeVarInt(duration); }
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}