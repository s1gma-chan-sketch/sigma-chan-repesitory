package dev.sigmachan.companions.network;

import dev.sigmachan.companions.registry.ModRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record CompanionStateS2C(int entityId, String profile, int trust, String stage, String mode,
                                boolean consent, boolean dating, boolean married, boolean aiConsent,
                                boolean owner, boolean aiAvailable, String home, String notice) implements CustomPayload {
    public static final Id<CompanionStateS2C> ID = new Id<>(ModRegistry.id("state"));
    public static final PacketCodec<RegistryByteBuf, CompanionStateS2C> CODEC = CustomPayload.codecOf(CompanionStateS2C::write, CompanionStateS2C::new);
    private CompanionStateS2C(RegistryByteBuf b) {
        this(b.readVarInt(), b.readString(16), b.readVarInt(), b.readString(40), b.readString(16),
                b.readBoolean(), b.readBoolean(), b.readBoolean(), b.readBoolean(), b.readBoolean(), b.readBoolean(), b.readString(100), b.readString(600));
    }
    private void write(RegistryByteBuf b) {
        b.writeVarInt(entityId); b.writeString(profile, 16); b.writeVarInt(trust); b.writeString(stage, 40); b.writeString(mode, 16);
        b.writeBoolean(consent); b.writeBoolean(dating); b.writeBoolean(married); b.writeBoolean(aiConsent); b.writeBoolean(owner); b.writeBoolean(aiAvailable);
        b.writeString(home, 100); b.writeString(notice, 600);
    }
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}