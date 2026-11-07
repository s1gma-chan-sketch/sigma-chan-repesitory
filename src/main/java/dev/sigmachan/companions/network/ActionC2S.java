package dev.sigmachan.companions.network;

import dev.sigmachan.companions.registry.ModRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record ActionC2S(int entityId, String action, String message) implements CustomPayload {
    public static final Id<ActionC2S> ID = new Id<>(ModRegistry.id("action"));
    public static final PacketCodec<RegistryByteBuf, ActionC2S> CODEC = CustomPayload.codecOf(ActionC2S::write, ActionC2S::new);
    private ActionC2S(RegistryByteBuf buf) { this(buf.readVarInt(), buf.readString(32), buf.readString(240)); }
    private void write(RegistryByteBuf buf) { buf.writeVarInt(entityId); buf.writeString(action, 32); buf.writeString(message, 240); }
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}