package ru.sigmachan.luckystar;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import ru.sigmachan.luckystar.entity.StarFriend;

@Mod(modid = LuckyStarMod.ID, name = "Lucky Star Friends", version = LuckyStarMod.VERSION,
        acceptedMinecraftVersions = "[1.12.2]")
public class LuckyStarMod {
    public static final String ID = "luckystar";
    public static final String VERSION = "0.1.0";
    @Mod.Instance(ID) public static LuckyStarMod instance;
    @SidedProxy(clientSide = "ru.sigmachan.luckystar.client.ClientProxy",
            serverSide = "ru.sigmachan.luckystar.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        register("konata", StarFriend.Konata.class, 0, 0x387fc3, 0x65be4c);
        register("kagami", StarFriend.Kagami.class, 1, 0x9273cb, 0xc54362);
        register("tsukasa", StarFriend.Tsukasa.class, 2, 0xa890d4, 0xf3db73);
        register("miyuki", StarFriend.Miyuki.class, 3, 0xe6a6b8, 0x8467bb);
        proxy.registerRenderers();
    }

    private void register(String name, Class<? extends StarFriend> type, int id, int egg, int spots) {
        EntityRegistry.registerModEntity(new ResourceLocation(ID, name), type, ID + "." + name, id,
                instance, 64, 3, true, egg, spots);
    }
}
