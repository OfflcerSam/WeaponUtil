package offlcersam.weaponfoundry.mixin;

import items.lists.WeaponList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;


@Mixin(WeaponList.class)
public interface WeaponListAccessorMixin {

    @Invoker("setBaseAttributes")
    static void invokeSetBaseAttributes(
            int weaponType,
            float volume,
            long creditValue,
            float damage,
            int range,
            float damageEnergyRatio) {
        throw new AssertionError();
    }
}
