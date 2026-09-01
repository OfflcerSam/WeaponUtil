package offlcersam.weaponfoundry.mixin;

import crafting.CraftingTable;
import crafting.CraftingTableNormal;
import mods.ModLogger;
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

            this.addRecipe(
                    recipe.label(),
                    productId,
                    recipe.blueprintId(), recipe.blueprintAmount(),
                    a.id(), a.amount(),
                    b.id(), b.amount(),
                    c.id(), c.amount()
            );

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

        ModLogger.log("[WeaponFoundry] Added " + added + " weapon/ammo recipe(s) from JSON.");
    }
}
