package offlcersam.weaponfoundry.mixin;

import game.Main;
import offlcersam.weaponfoundry.WeaponFoundryLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Main.class, remap = false)
public class MainSetupMixin {

    @Inject(
            method = "setup",
            at = @At(
                    value = "INVOKE",
                    target = "L_database/ItemDatabase;loadDatabase()V",
                    shift = At.Shift.AFTER
            )
    )
    private void weaponfoundry$loadWeapons(CallbackInfo ci) {
        WeaponFoundryLoader.load();
    }

    @Inject(
            method = "setup",
            at = @At(
                    value = "INVOKE",
                    target = "Lgame/markets/MarketDatabase;loadDatabase()V",
                    shift = At.Shift.AFTER
            )
    )
    private void weaponfoundry$registerMarkets(CallbackInfo ci) {
        offlcersam.weaponfoundry.MarketRegistrar.registerMarkets();
    }
}