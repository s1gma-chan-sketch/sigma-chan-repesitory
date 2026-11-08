package dev.sigmachan.companions.registry;

import dev.sigmachan.companions.CompanionProfile;
import dev.sigmachan.companions.entity.CompanionEntity;
import dev.sigmachan.companions.item.InvitationItem;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.component.type.FoodComponents;
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
                    .dimensions(0.6f, 1.65f).eyeHeight(1.4f).maxTrackingRange(8).trackingTickInterval(2).build(COMPANION_ID.toString()));
    public static final Item CHOCOLATE_CORNET = Registry.register(Registries.ITEM, id("chocolate_cornet"),
            new Item(new Item.Settings().maxCount(16).food(FoodComponents.COOKIE)));
    public static final Map<CompanionProfile, InvitationItem> INVITATIONS = new LinkedHashMap<>();
    public static final Map<String, InvitationItem> LEGACY_INVITATIONS = new LinkedHashMap<>();
    // Inert alpha-world item: keeps old inventories loadable, but has no recipe, creative entry or romance action.
    public static final Item LEGACY_RING = Registry.register(Registries.ITEM, id("promise_ring"), new Item(new Item.Settings().maxCount(1)));
    public static final net.minecraft.registry.RegistryKey<net.minecraft.item.ItemGroup> GROUP_KEY = net.minecraft.registry.RegistryKey.of(Registries.ITEM_GROUP.getKey(), id("group"));
    public static final net.minecraft.item.ItemGroup GROUP = Registry.register(Registries.ITEM_GROUP, GROUP_KEY,
            FabricItemGroup.builder().displayName(Text.translatable("itemGroup.sigma_companions")).icon(() -> new ItemStack(CHOCOLATE_CORNET)).build());

    public static Identifier id(String path) { return Identifier.of("sigma_companions", path); }
    public static void initialize() {
        FabricDefaultAttributeRegistry.register(COMPANION, CompanionEntity.attributes());
        for (CompanionProfile profile : CompanionProfile.values()) {
            InvitationItem item = Registry.register(Registries.ITEM, id("invitation_" + profile.id()), new InvitationItem(profile, new Item.Settings().maxCount(16)));
            INVITATIONS.put(profile, item);
        }
        registerLegacy("invitation_akari", CompanionProfile.TSUKASA);
        registerLegacy("invitation_mio", CompanionProfile.KAGAMI);
        registerLegacy("invitation_rei", CompanionProfile.KONATA);
        net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents.modifyEntriesEvent(GROUP_KEY).register(entries -> {
            INVITATIONS.values().forEach(entries::add);
            entries.add(CHOCOLATE_CORNET);
        });
    }
    private static void registerLegacy(String id, CompanionProfile profile) {
        LEGACY_INVITATIONS.put(id, Registry.register(Registries.ITEM, id(id), new InvitationItem(profile, new Item.Settings().maxCount(16))));
    }
    private ModRegistry() {}
}