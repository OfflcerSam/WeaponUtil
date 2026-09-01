package offlcersam.weaponfoundry.mixin;

import game.weapons.RailGunFX;
import offlcersam.weaponfoundry.AmmoRegistrar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RailGunFX.class, remap = false)
public abstract class RailGunFXMixin {

    @Inject(method = "configureEFXandBonus", at = @At("HEAD"), cancellable = true)
    private void weaponfoundry$overrideRailAmmo(int railAmmoItemBaseId, CallbackInfo ci) {
        AmmoRegistrar.ResolvedFx fx = AmmoRegistrar.getRailFx(railAmmoItemBaseId);

        if (fx == null) {
            return;
        }

        RailGunFX self = (RailGunFX) (Object) this;
        self.bonusEMDamage = fx.bonusEMDamage();
        self.bonusPHDamage = fx.bonusPHDamage();
        self.glow = fx.glowColor();

        ci.cancel();
    }
}
