package com.pipican.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.pipican.Pipican;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** Key bindings (rebindable in Controls under the "Pipican" category). */
public final class PipicanKeys {
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Pipican.id("main"));

    /** Opens the trinket screen. */
    public static final KeyMapping TRINKETS = new KeyMapping("key.pipican.trinkets", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);
    /** Opens class selection while you have no class (later: the character sheet). */
    public static final KeyMapping CLASS = new KeyMapping("key.pipican.class", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, CATEGORY);

    /** Use the abilities in slots 1-4 (see {@link com.pipican.ability.AbilitySlots}). */
    public static final KeyMapping[] ABILITIES = {
            new KeyMapping("key.pipican.ability_1", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, CATEGORY),
            new KeyMapping("key.pipican.ability_2", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, CATEGORY),
            new KeyMapping("key.pipican.ability_3", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY),
            new KeyMapping("key.pipican.ability_4", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY),
    };

    private PipicanKeys() {}
}
