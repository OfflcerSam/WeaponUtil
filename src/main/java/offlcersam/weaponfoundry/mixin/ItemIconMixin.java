package offlcersam.weaponfoundry.mixin;

import illuminatus.core.graphics.Texture;
import items.Item;
import offlcersam.weaponfoundry.WeaponFoundryIcons;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = Item.class, remap = false)
public abstract class ItemIconMixin {

    @Redirect(method = "drawIconAsMount(DDDF)V",
            at = @At(value = "INVOKE",
                    target = "Lilluminatus/core/graphics/Texture;drawPartRotatedScaled(DDDDDDDDDDDDD)V"))
    private void weaponfoundry$redirectMountDraw(Texture original,
                                              double x, double y, double width, double height,
                                              double xTex1, double yTex1, double xTex2, double yTex2,
                                              double xScale, double yScale, double rotation, double rotOriginX, double rotOriginY) {

        Item self = (Item)(Object)this;
        if (WeaponFoundryIcons.isCustomIcon(self)) {
            WeaponFoundryIcons.TEXTURE.drawPartRotatedScaled(x, y, width, height,
                    WeaponFoundryIcons.localX(self), WeaponFoundryIcons.localY(self),
                    WeaponFoundryIcons.localX(self) + 32, WeaponFoundryIcons.localY(self) + 32,
                    xScale, yScale, rotation, rotOriginX, rotOriginY);
        } else {
            original.drawPartRotatedScaled(x, y, width, height, xTex1, yTex1, xTex2, yTex2,
                    xScale, yScale, rotation, rotOriginX, rotOriginY);
        }
    }
    @Redirect(method = "drawIcon(IIZF)V",
            at = @At(value = "INVOKE", target = "Lilluminatus/core/graphics/Texture;drawPart(DDDDDDDD)V"))
    private void weaponfoundry$redirectDrawIcon8(Texture original, double x1, double y1, double x2, double y2,
                                              double xTex1, double yTex1, double xTex2, double yTex2) {
        Item self = (Item)(Object)this;
        if (WeaponFoundryIcons.isCustomIcon(self)) {
            WeaponFoundryIcons.TEXTURE.drawPart(x1, y1, x2, y2,
                    WeaponFoundryIcons.localX(self), WeaponFoundryIcons.localY(self),
                    WeaponFoundryIcons.localX(self) + 32, WeaponFoundryIcons.localY(self) + 32);
        } else {
            original.drawPart(x1, y1, x2, y2, xTex1, yTex1, xTex2, yTex2);
        }
    }

    @Redirect(method = "drawIcon(IIZF)V",
            at = @At(value = "INVOKE", target = "Lilluminatus/core/graphics/Texture;drawPart(DDDDDD)V"))
    private void weaponfoundry$redirectDrawIcon6(Texture original, double x, double y, double width, double height,
                                              double xTex, double yTex) {
        Item self = (Item)(Object)this;
        if (WeaponFoundryIcons.isCustomIcon(self)) {
            WeaponFoundryIcons.TEXTURE.drawPart(x, y, width, height,
                    WeaponFoundryIcons.localX(self), WeaponFoundryIcons.localY(self));
        } else {
            original.drawPart(x, y, width, height, xTex, yTex);
        }
    }

    @Redirect(method = "drawQuickMenuIcon(IIF)V",
            at = @At(value = "INVOKE", target = "Lilluminatus/core/graphics/Texture;drawPart(DDDDDDDD)V"))
    private void weaponfoundry$redirectQuickMenu(Texture original, double x1, double y1, double x2, double y2,
                                              double xTex1, double yTex1, double xTex2, double yTex2) {
        Item self = (Item)(Object)this;
        if (WeaponFoundryIcons.isCustomIcon(self)) {
            WeaponFoundryIcons.TEXTURE.drawPart(x1, y1, x2, y2,
                    WeaponFoundryIcons.localX(self), WeaponFoundryIcons.localY(self),
                    WeaponFoundryIcons.localX(self) + 32, WeaponFoundryIcons.localY(self) + 32);
        } else {
            original.drawPart(x1, y1, x2, y2, xTex1, yTex1, xTex2, yTex2);
        }
    }
}
