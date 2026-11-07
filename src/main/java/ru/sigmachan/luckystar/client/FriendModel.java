package ru.sigmachan.luckystar.client;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;

/** Block-built hair, accessories and skirt on an animated vanilla humanoid base. */
public class FriendModel extends ModelBiped {
    public FriendModel(int style) {
        super(0.0F, 0.0F, 128, 128);
        bipedHeadwear.showModel = false;
        // All hair pieces are children of the head, so they follow head rotation.
        hair(-4.5F, -8.6F, -4.5F, 9, 2, 9);
        hair(-4.5F, -7.0F, -4.5F, 2, 6, 2);
        hair(2.5F, -7.0F, -4.5F, 2, 6, 2);
        hair(-3.0F, -7.5F, -4.6F, 6, 2, 1);
        if (style == 0) { // Konata: long blue hair and an ahoge
            hair(-4.5F, -7.0F, 3.1F, 9, 18, 2);
            hair(0.0F, -11.0F, 0.0F, 1, 3, 1);
        } else if (style == 1) { // Kagami: twin tails
            hair(-4.5F, -7.0F, 3.1F, 9, 6, 2);
            hair(-6.0F, -5.5F, 1.0F, 2, 19, 3);
            hair(4.0F, -5.5F, 1.0F, 2, 19, 3);
        } else if (style == 2) { // Tsukasa: short bob and a bow
            hair(-4.5F, -7.0F, 3.1F, 9, 8, 2);
            ModelRenderer bow = new ModelRenderer(this, 64, 96);
            bow.addBox(-4.0F, -10.0F, -0.5F, 8, 2, 2);
            bipedHead.addChild(bow);
        } else { // Miyuki: long hair and glasses
            hair(-4.5F, -7.0F, 3.1F, 9, 19, 2);
            ModelRenderer glasses = new ModelRenderer(this, 100, 96);
            glasses.addBox(-4.0F, -4.2F, -4.75F, 8, 1, 1);
            bipedHead.addChild(glasses);
        }
        ModelRenderer skirt = new ModelRenderer(this, 64, 64);
        skirt.addBox(-4.5F, 10.0F, -2.5F, 9, 5, 5);
        bipedBody.addChild(skirt);
    }

    private void hair(float x, float y, float z, int w, int h, int d) {
        ModelRenderer part = new ModelRenderer(this, 0, 64);
        part.addBox(x, y, z, w, h, d);
        bipedHead.addChild(part);
    }
}
