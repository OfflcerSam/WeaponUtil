package offlcersam.weaponfoundry.mixin;

import game.objects.SpaceShip;
import game.weapons.RailGunFX;
import offlcersam.weaponfoundry.AmmoRegistrar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RailGunFX.class, remap = false)
public abstract class RailGunFXMixin {

    @Inject(method = "configureEFXandBonus", at = @At("HEAD"), cancellable = true)
    private static void weaponfoundry$overrideRailAmmo(int railAmmoItemBaseId, SpaceShip user, CallbackInfo ci) {
        AmmoRegistrar.ResolvedFx fx = AmmoRegistrar.getRailFx(railAmmoItemBaseId);

        if (fx == null) {
            return;
        }

        // Mirrors the usingAmmo/subFx bookkeeping the vanilla method would have done before its switch.
        RailGunFX.usingAmmo = true;
        RailGunFX.subFx = 0;
        RailGunFX.bonusEMDamage = (float) fx.bonusEMDamage();
        RailGunFX.bonusPHDamage = (float) fx.bonusPHDamage();
        RailGunFX.glow = fx.glowColor();

        ci.cancel();
    }
}