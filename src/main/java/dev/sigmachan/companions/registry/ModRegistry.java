package dev.sigmachan.companions.registry;

import dev.sigmachan.companions.CompanionProfile;
import dev.sigmachan.companions.entity.CompanionEntity;
import dev.sigmachan.companions.item.InvitationItem;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModRegistry {
    public static final Identifier COMPANION_ID = id("companion");
    public static final EntityType<CompanionEntity> COMPANION = Registry.register(Registries.ENTITY_TYPE, COMPANION_ID,
            EntityType.Builder.<CompanionEntity>create(CompanionEntity::new, SpawnGroup.MISC)
                    .dimensions(0.6f, 1.9f).eyeHeight(1.65f).maxTrackingRange(8).trackingTickInterval(2).build(COMPANION_ID.toString()));
    public static final Map<CompanionProfile, InvitationItem> INVITATIONS = new LinkedHashMap<>();
    public static final Item PROMISE_RING = Registry.register(Registries.ITEM, id("promise_ring"), new Item(new Item.Settings().maxCount(1)));
    public static final net.minecraft.registry.RegistryKey<net.minecraft.item.ItemGroup> GROUP_KEY = net.minecraft.registry.RegistryKey.of(Registries.ITEM_GROUP.getKey(), id("group"));
    public static final net.minecraft.item.ItemGroup GROUP = Registry.register(Registries.ITEM_GROUP, GROUP_KEY,
            FabricItemGroup.builder().displayName(Text.translatable("itemGroup.sigma_companions")).icon(() -> new ItemStack(PROMISE_RING)).build());

    public static Identifier id(String path) { return Identifier.of("sigma_companions", path); }
    public static void initialize() {
        FabricDefaultAttributeRegistry.register(COMPANION, CompanionEntity.attributes());
        for (CompanionProfile profile : CompanionProfile.values()) {
            InvitationItem item = Registry.register(Registries.ITEM, id("invitation_" + profile.id()), new InvitationItem(profile, new Item.Settings().maxCount(16)));
            INVITATIONS.put(profile, item);
        }
        net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents.modifyEntriesEvent(GROUP_KEY).register(entries -> {
            INVITATIONS.values().forEach(entries::add);
            entries.add(PROMISE_RING);
        });
    }
    private ModRegistry() {}
}