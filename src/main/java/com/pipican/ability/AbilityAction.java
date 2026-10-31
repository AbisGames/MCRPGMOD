package com.pipican.ability;

import com.mojang.serialization.Codec;

/**
 * Something an ability does when used (or when its state ends): heal, push, shoot a bolt...
 * Chosen in JSON by {@code "type"}; the built-in types live in {@link AbilityActionTypes}.
 */
public interface AbilityAction {

    Codec<AbilityAction> CODEC = AbilityActionTypes.TYPE_CODEC.dispatch("type", AbilityAction::type, AbilityActionTypes.Type::codec);

    AbilityActionTypes.Type type();

    void run(AbilityContext ctx);
}
