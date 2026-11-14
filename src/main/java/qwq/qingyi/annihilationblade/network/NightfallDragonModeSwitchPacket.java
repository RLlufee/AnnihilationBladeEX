package qwq.qingyi.annihilationblade.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import qwq.qingyi.annihilationblade.AnnihilationBladeex;
import qwq.qingyi.annihilationblade.nightfall_dragon.NightfallDragonDefinitions;
import qwq.qingyi.annihilationblade.nightfall_dragon.item.NightfallDragonItemSupport;

/**
 * 夜临玄龙形态切换请求包（C2S）。
 */
public record NightfallDragonModeSwitchPacket(String requestedForm) implements CustomPacketPayload {
    public static final Type<NightfallDragonModeSwitchPacket> TYPE =
            new Type<>(AnnihilationBladeex.prefix("nightfall_dragon_mode_switch"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NightfallDragonModeSwitchPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, NightfallDragonModeSwitchPacket::requestedForm, NightfallDragonModeSwitchPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handleOnServer(ServerPlayer player) {
        if (NightfallDragonItemSupport.isHoldingNightfallDragon(player)) {
            ItemStack stack = NightfallDragonItemSupport.heldNightfallDragon(player);
            if (!stack.isEmpty()) {
                if (this.requestedForm != null && !this.requestedForm.isEmpty()) {
                    NightfallDragonDefinitions.applyForm(stack, this.requestedForm);
                } else {
                    NightfallDragonDefinitions.toggleForm(stack);
                }
            }
        }
    }
}
