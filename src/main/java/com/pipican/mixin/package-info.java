/**
 * Mixins: small patches to vanilla code for class abilities that no NeoForge event can reach. Each one only asks
 * {@link com.pipican.ability.Trackless} or the player's synced movement flags whether to change anything, so with no class
 * bonus the game behaves exactly as vanilla.
 */
package com.pipican.mixin;
