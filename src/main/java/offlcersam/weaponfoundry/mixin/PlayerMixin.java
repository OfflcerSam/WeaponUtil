package offlcersam.weaponfoundry.mixin;

import offlcersam.weaponfoundry.DebugItemGrant;
import game.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Player.class, remap = false)
public class PlayerMixin {

    @Inject(method = "loadPlayer", at = @At("TAIL"))
    private static void weaponfoundry$grantDebugItems(String name, String saveFileName, CallbackInfo ci) {
        DebugItemGrant.grantItemsToDebugCharacter();
    }
}
