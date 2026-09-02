package offlcersam.weaponfoundry.mixin;

import com.sector.bridge.SSFMLLogger;
import crafting.CraftingTable;
import crafting.CraftingTableNormal;
import offlcersam.weaponfoundry.AmmoDefinition;
import offlcersam.weaponfoundry.AmmoRegistrar;
import offlcersam.weaponfoundry.WeaponDefinition;
import offlcersam.weaponfoundry.WeaponRegistrar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CraftingTableNormal.class, remap = false)
public abstract class CraftingTableMixin extends CraftingTable {

    @Inject(
            method = "init",
            at = @At("TAIL")
    )
    private void weaponfoundry$registerRecipes(CallbackInfo ci) {
        int added = 0;

        for (WeaponDefinition def : WeaponRegistrar.getLoadedWeapons()) {
            WeaponDefinition.Recipe recipe = def.recipe();

            if (recipe == null) {
                continue;
            }

            int productId = WeaponRegistrar.toDatabaseID(def.id());

            WeaponDefinition.Ingredient a = recipe.ingredientA();
            WeaponDefinition.Ingredient b = recipe.ingredientB();
            WeaponDefinition.Ingredient c = recipe.ingredientC();

            if (recipe.plusId() != null) {
                if (recipe.blueprintAmount() != 1) {
                    SSFMLLogger.log(
                            "[WeaponFoundry] Weapon " + def.name() + " (id: " + def.id()
                                    + ") sets recipe.blueprintAmount to " + recipe.blueprintAmount()
                                    + " but also sets plusId - addRecipeAndPlus ignores blueprintAmount and always uses 1."
                    );
                }

                this.addRecipeAndPlus(
                        recipe.label(),
                        productId,
                        WeaponRegistrar.toDatabaseID(recipe.plusId()),
                        recipe.blueprintId(),
                        a.id(), a.amount(),
                        b.id(), b.amount(),
                        c.id(), c.amount()
                );
            } else {
                this.addRecipe(
                        recipe.label(),
                        productId,
                        recipe.blueprintId(), recipe.blueprintAmount(),
                        a.id(), a.amount(),
                        b.id(), b.amount(),
                        c.id(), c.amount()
                );
            }

            added++;
        }

        for (AmmoDefinition def : AmmoRegistrar.getLoadedAmmo()) {
            AmmoDefinition.Recipe recipe = def.recipe();

            if (recipe == null) {
                continue;
            }

            int productId = AmmoRegistrar.toDatabaseID(def.id());

            AmmoDefinition.Ingredient a = recipe.ingredientA();
            AmmoDefinition.Ingredient b = recipe.ingredientB();
            AmmoDefinition.Ingredient c = recipe.ingredientC();

            this.addRecipeStackOutput(
                    recipe.label(),
                    productId, recipe.productAmount(),
                    recipe.blueprintId(), recipe.blueprintAmount(),
                    a.id(), a.amount(),
                    b.id(), b.amount(),
                    c.id(), c.amount()
            );

            added++;
        }

        SSFMLLogger.log("[WeaponFoundry] Added " + added + " weapon/ammo recipe(s) from JSON.");
    }
}