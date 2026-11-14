package qwq.qingyi.annihilationblade.common;

import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.capability.slashblade.CapabilitySlashBlade;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.client.renderer.CarryType;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.phys.Vec3;
import qwq.qingyi.annihilationblade.AnnihilationBladeex;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * 拔刀剑状态访问工具：统一读写 SlashBlade-Refabricated 的 {@link CapabilitySlashBlade} 状态组件与物品组件。
 */
public final class SlashBladeStateSupport {
    private SlashBladeStateSupport() {
    }

    public static boolean isSlashBlade(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemSlashBlade;
    }

    public static Optional<ISlashBladeState> state(ItemStack stack) {
        return !isSlashBlade(stack) ? Optional.empty() : CapabilitySlashBlade.getBladeState(stack);
    }

    public static int killCount(ItemStack stack) {
        return state(stack).map(ISlashBladeState::getKillCount).orElse(0);
    }

    public static int proudSoul(ItemStack stack) {
        return state(stack).map(ISlashBladeState::getProudSoulCount).orElse(0);
    }

    public static int refine(ItemStack stack) {
        return state(stack).map(ISlashBladeState::getRefine).orElse(0);
    }

    /**
     * 获取刀剑的逻辑翻译键：优先读取 {@link ISlashBladeState#getTranslationKey()}，否则回退物品默认描述键。
     */
    public static String translationKey(ItemStack stack) {
        if (stack.isEmpty()) {
            return "";
        }
        return state(stack)
                .map(ISlashBladeState::getTranslationKey)
                .filter(key -> key != null && !key.isBlank())
                .orElseGet(() -> stack.getItem().getDescriptionId());
    }

    /**
     * 从注册表查找视图中实例化名刀，未就绪或不存在时返回 {@link ItemStack#EMPTY}。
     */
    public static ItemStack getDefinedStack(
            @Nullable HolderLookup.Provider registries,
            Identifier id,
            @Nullable Item bladeItem
    ) {
        if (registries == null) {
            return ItemStack.EMPTY;
        }
        try {
            var lookup = SlashBlade.getSlashBladeDefinitionRegistry(registries);
            ResourceKey<SlashBladeDefinition> key = ResourceKey.create(SlashBladeDefinition.REGISTRY_KEY, id);
            return lookup.get(key)
                    .map(holder -> bladeItem == null ? holder.value().getBlade() : holder.value().getBlade(bladeItem))
                    .map(ItemStack::copy)
                    .orElse(ItemStack.EMPTY);
        } catch (RuntimeException exception) {
            AnnihilationBladeex.LOGGER.warn("通过注册表读取名刀定义失败: {}", id, exception);
            return ItemStack.EMPTY;
        }
    }

    /**
     * 统一应用运行时拔刀剑状态与不可破坏物品组件，供五大刀剑定义复用。
     */
    public static void applyRuntimeState(
            ItemStack stack,
            String translationKey,
            int maxDamage,
            float baseAttackModifier,
            int proudSoul,
            int killCount,
            int refine,
            Identifier slashArt,
            Identifier comboRoot,
            CarryType carryType,
            int colorCode,
            Identifier texture,
            Identifier model,
            List<Identifier> specialEffects
    ) {
        if (stack.isEmpty()) {
            return;
        }

        stack.set(DataComponents.MAX_DAMAGE, maxDamage);
        stack.setDamageValue(0);
        stack.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
        stack.set(DataComponents.RARITY, Rarity.EPIC);

        state(stack).ifPresent(bladeState -> {
            bladeState.setNonEmpty();
            bladeState.setTranslationKey(translationKey);
            bladeState.setBaseAttackModifier(baseAttackModifier);
            bladeState.setMaxDamage(maxDamage);
            bladeState.setDamage(0);
            bladeState.setProudSoulCount(proudSoul);
            bladeState.setKillCount(killCount);
            bladeState.setRefine(refine);
            bladeState.setBroken(false);
            bladeState.setSealed(false);
            bladeState.setSlashArtsKey(slashArt);
            bladeState.setDefaultBewitched(true);
            bladeState.setComboRoot(comboRoot);
            bladeState.setCarryType(carryType);
            bladeState.setColorCode(colorCode);
            bladeState.setEffectColorInverse(false);
            bladeState.setAdjust(Vec3.ZERO);
            bladeState.setTexture(texture);
            bladeState.setModel(model);

            ListTag effectsTag = new ListTag();
            for (Identifier effectId : specialEffects) {
                effectsTag.add(StringTag.valueOf(effectId.toString()));
            }
            bladeState.setSpecialEffects(effectsTag);
        });
    }
}
