package dev.sigmachan.companions.network;

import dev.sigmachan.companions.registry.ModRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record DialogueS2C(int entityId, String speaker, String message) implements CustomPayload {
    public static final Id<DialogueS2C> ID = new Id<>(ModRegistry.id("dialogue"));
    public static final PacketCodec<RegistryByteBuf, DialogueS2C> CODEC = CustomPayload.codecOf(DialogueS2C::write, DialogueS2C::new);
    private DialogueS2C(RegistryByteBuf b) { this(b.readVarInt(), b.readString(40), b.readString(1200)); }
    private void write(RegistryByteBuf b) { b.writeVarInt(entityId); b.writeString(speaker, 40); b.writeString(message, 1200); }
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}