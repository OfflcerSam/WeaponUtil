package offlcersam.weaponfoundry.mixin;

import game.weapons.MissileFX;
import offlcersam.weaponfoundry.AmmoRegistrar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MissileFX.class, remap = false)
public abstract class MissileFXMixin {

    @Inject(method = "configureEFXandBonus", at = @At("HEAD"), cancellable = true)
    private void weaponfoundry$overrideMissileAmmo(int missileItemBaseId, CallbackInfo ci) {
        AmmoRegistrar.ResolvedFx fx = AmmoRegistrar.getMissileFx(missileItemBaseId);

        if (fx == null) {
            return;
        }

        MissileFX self = (MissileFX) (Object) this;
        self.SPEED = (float) fx.speed();
        self.scale = fx.scale();
        self.bonusEMDamage = (float) fx.bonusEMDamage();
        self.bonusPHDamage = (float) fx.bonusPHDamage();
        self.glowColor = fx.glowColor();
        self.baseColor = fx.baseColor();

        ci.cancel();
    }
}