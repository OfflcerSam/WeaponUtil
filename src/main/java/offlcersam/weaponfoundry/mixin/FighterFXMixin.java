package offlcersam.weaponfoundry.mixin;

import game.weapons.FighterFX;
import offlcersam.weaponfoundry.AmmoRegistrar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FighterFX.class, remap = false)
public abstract class FighterFXMixin {

    @Inject(method = "configureFighter", at = @At("HEAD"), cancellable = true)
    private void weaponfoundry$overrideFighterAmmo(int fighterItemBaseId, CallbackInfo ci) {
        AmmoRegistrar.ResolvedFx fx = AmmoRegistrar.getFighterFx(fighterItemBaseId);

        if (fx == null) {
            return;
        }

        FighterFX self = (FighterFX) (Object) this;
        self.fighterGFXIndex = fx.fighterGFXIndex();
        self.SPEED = (float) fx.speed();
        self.scale = fx.scale();
        self.EMDamage = (float) fx.bonusEMDamage();
        self.PHDamage = (float) fx.bonusPHDamage();
        self.glowColor = fx.glowColor();
        self.baseColor = fx.baseColor();
        self.weaponColor = fx.weaponColor();

        ci.cancel();
    }
}