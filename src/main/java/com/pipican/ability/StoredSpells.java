package com.pipican.ability;

import com.pipican.Pipican;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * Potion effects loaded into gear (Store Spell). The effects and the charges left are saved on the item itself. A weapon
 * casts them with each hit: harmful effects on the mob hit, beneficial ones on its wielder. Armor casts them when its
 * wearer is hurt by a mob: harmful ones on the attacker, beneficial ones on the wearer. Each cast uses one charge.
 */
@EventBusSubscriber(modid = Pipican.MOD_ID)
public final class StoredSpells {

    private static final String KEY = "pipican_stored_spell";
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private StoredSpells() {}

    public static void store(ItemStack gear, List<MobEffectInstance> effects, int charges) {
        ListTag list = new ListTag();
        for (MobEffectInstance effect : effects) {
            MobEffectInstance.CODEC.encodeStart(NbtOps.INSTANCE, effect).result().ifPresent(list::add);
        }
        CustomData.update(DataComponents.CUSTOM_DATA, gear, tag -> {
            CompoundTag spell = new CompoundTag();
            spell.put("effects", list);
            spell.putInt("charges", charges);
            tag.put(KEY, spell);
        });
        List<Component> lines = new ArrayList<>();
        for (MobEffectInstance effect : effects) {
            lines.add(Component.translatable("pipican.stored_spell.line", Component.translatable(effect.getDescriptionId()), charges)
                    .withStyle(ChatFormatting.DARK_PURPLE));
        }
        gear.set(DataComponents.LORE, new ItemLore(lines));
    }

    private static List<MobEffectInstance> effects(CompoundTag spell) {
        List<MobEffectInstance> out = new ArrayList<>();
        for (Tag t : spell.getListOrEmpty("effects")) {
            MobEffectInstance.CODEC.parse(NbtOps.INSTANCE, t).result().ifPresent(out::add);
        }
        return out;
    }

    /** Casts the gear's stored spell (harmful on {@code foe}, beneficial on {@code owner}) and uses a charge. */
    private static void cast(ItemStack gear, LivingEntity owner, LivingEntity foe) {
        CustomData data = gear.get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.copyTag().contains(KEY)) {
            return;
        }
        CompoundTag spell = data.copyTag().getCompoundOrEmpty(KEY);
        List<MobEffectInstance> effects = effects(spell);
        for (MobEffectInstance effect : effects) {
            LivingEntity to = effect.getEffect().value().isBeneficial() ? owner : foe;
            if (to != null && to.isAlive()) {
                to.addEffect(new MobEffectInstance(effect), owner);
            }
        }
        int left = spell.getIntOr("charges", 0) - 1;
        if (left <= 0) {
            CustomData.update(DataComponents.CUSTOM_DATA, gear, tag -> tag.remove(KEY));
            gear.remove(DataComponents.LORE);
        } else {
            store(gear, effects, left);
        }
    }

    public static boolean has(ItemStack gear) {
        CustomData data = gear.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().contains(KEY);
    }

    @SubscribeEvent
    static void onDamage(LivingDamageEvent.Post event) {
        if (event.getInflictedDamage() <= 0) {
            return;
        }
        LivingEntity victim = event.getEntity();
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && attacker != victim && event.getSource().getDirectEntity() == attacker) {
            cast(attacker.getMainHandItem(), attacker, victim);
        }
        if (victim instanceof ServerPlayer wearer && event.getSource().getEntity() instanceof LivingEntity attacker && attacker != wearer) {
            for (EquipmentSlot slot : ARMOR) {
                cast(wearer.getItemBySlot(slot), wearer, attacker);
            }
        }
    }
}
