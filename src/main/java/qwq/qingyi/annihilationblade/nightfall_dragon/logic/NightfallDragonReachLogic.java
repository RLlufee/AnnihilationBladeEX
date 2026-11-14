package qwq.qingyi.annihilationblade.nightfall_dragon.logic;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import qwq.qingyi.annihilationblade.AnnihilationBladeex;
import qwq.qingyi.annihilationblade.nightfall_dragon.NightfallDragonDefinitions;
import qwq.qingyi.annihilationblade.nightfall_dragon.item.NightfallDragonItemSupport;

/**
 * 夜临玄龙触及扩展与状态维持：手持期间提供额外实体交互距离，并周期同步刀身运行时属性。
 */
public final class NightfallDragonReachLogic {
    private static final Identifier REACH_MODIFIER_ID = AnnihilationBladeex.prefix("nightfall_dragon_entity_reach");
    private static final double ENTITY_REACH_BONUS = 3.0D;

    private NightfallDragonReachLogic() {
    }

    public static void onPlayerTick(Player player) {
        if (player.level().isClientSide()) {
            return;
        }

        updateReach(player);
    }

    public static void onPlayerLogout(Player player) {
        removeReach(player);
    }

    public static void onPlayerChangedDimension(Player player) {
        removeReach(player);
    }

    private static void updateReach(Player player) {
        if (player.tickCount % 20 == 0) {
            refreshHeldBlade(player);
        }

        AttributeInstance attribute = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (attribute == null) {
            return;
        }

        boolean hasModifier = attribute.getModifier(REACH_MODIFIER_ID) != null;
        if (NightfallDragonItemSupport.isHoldingNightfallDragon(player)) {
            if (!hasModifier) {
                attribute.addTransientModifier(new AttributeModifier(REACH_MODIFIER_ID, ENTITY_REACH_BONUS, Operation.ADD_VALUE));
            }
        } else if (hasModifier) {
            attribute.removeModifier(REACH_MODIFIER_ID);
        }
    }

    private static void refreshHeldBlade(Player player) {
        if (NightfallDragonItemSupport.isNightfallDragon(player.getMainHandItem())) {
            NightfallDragonDefinitions.ensureStats(player.getMainHandItem(), player.level());
        }

        if (NightfallDragonItemSupport.isNightfallDragon(player.getOffhandItem())) {
            NightfallDragonDefinitions.ensureStats(player.getOffhandItem(), player.level());
        }
    }

    private static void removeReach(Player player) {
        AttributeInstance attribute = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (attribute != null && attribute.getModifier(REACH_MODIFIER_ID) != null) {
            attribute.removeModifier(REACH_MODIFIER_ID);
        }
    }
}
