package com.pipican.trinket;

import com.pipican.Pipican;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Menu (container) types. */
public final class PipicanMenus {
    public static final DeferredRegister<MenuType<?>> REGISTER = DeferredRegister.create(Registries.MENU, Pipican.MOD_ID);

    public static final Supplier<MenuType<TrinketMenu>> TRINKETS = REGISTER.register("trinkets", () -> IMenuTypeExtension.create(TrinketMenu::fromNetwork));

    private PipicanMenus() {}
}
