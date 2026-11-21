package QWQ.QingYi.annihilationblade.network;

import QWQ.QingYi.annihilationblade.blood_prison.network.BloodPrisonDomainPacket;
import QWQ.QingYi.annihilationblade.render.network.FxEventPacket;
import QWQ.QingYi.annihilationblade.render.network.FxSustainPacket;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
   /**
    * 2：新增通用特效事件包 FxEventPacket；3：新增持续特效通道 FxSustainPacket；
    * 4：新增特效类型 COSMIC_DESCENT（旧客户端无法识别，且服务端已不再发送对应粒子）；
    * 5：新增血狱特效 BLOOD_DOMAIN / BLOOD_DASH / BLOOD_BURST，魔龙夜陨常驻光环改为客户端绘制；
    * 6：新增 JUDGEMENT_DOMAIN / JUDGEMENT_CUT / SCALE_GUARD / LOLI_JUDGEMENT；
    * 7：迁移湮灭之刃全部 SE 至专属渲染器；
    * 8：移除已废弃的 TERMINUS_MARK（11）与 ABYSSAL_DECREE（17）特效事件。
    * 新旧版本客户端/服务端互相拒绝连接，避免解析错乱或特效缺失。
    */
   private static final String PROTOCOL_VERSION = "8";
   private static int messageId;
   public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
      ResourceLocation.fromNamespaceAndPath("annihilationblade", "main"), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals
   );

   private ModNetwork() {
   }

   public static void register() {
      CHANNEL.registerMessage(
         messageId++,
         BloodPrisonDomainPacket.class,
         BloodPrisonDomainPacket::encode,
         BloodPrisonDomainPacket::decode,
         BloodPrisonDomainPacket::handle,
         Optional.of(NetworkDirection.PLAY_TO_CLIENT)
      );
      CHANNEL.registerMessage(
         messageId++,
         DankongBlinkModePacket.class,
         DankongBlinkModePacket::encode,
         DankongBlinkModePacket::decode,
         DankongBlinkModePacket::handle,
         Optional.of(NetworkDirection.PLAY_TO_SERVER)
      );
      CHANNEL.registerMessage(
         messageId++,
         InfinityStellarisAiErasurePacket.class,
         InfinityStellarisAiErasurePacket::encode,
         InfinityStellarisAiErasurePacket::decode,
         InfinityStellarisAiErasurePacket::handle,
         Optional.of(NetworkDirection.PLAY_TO_SERVER)
      );
      CHANNEL.registerMessage(
         messageId++,
         InfinityStellarisAiRestorePacket.class,
         InfinityStellarisAiRestorePacket::encode,
         InfinityStellarisAiRestorePacket::decode,
         InfinityStellarisAiRestorePacket::handle,
         Optional.of(NetworkDirection.PLAY_TO_SERVER)
      );
      CHANNEL.registerMessage(
         messageId++,
         NightfallDragonModeSwitchPacket.class,
         NightfallDragonModeSwitchPacket::encode,
         NightfallDragonModeSwitchPacket::decode,
         NightfallDragonModeSwitchPacket::handle,
         Optional.of(NetworkDirection.PLAY_TO_SERVER)
      );
      CHANNEL.registerMessage(
         messageId++,
         FxEventPacket.class,
         FxEventPacket::encode,
         FxEventPacket::decode,
         FxEventPacket::handle,
         Optional.of(NetworkDirection.PLAY_TO_CLIENT)
      );
      CHANNEL.registerMessage(
         messageId++,
         FxSustainPacket.class,
         FxSustainPacket::encode,
         FxSustainPacket::decode,
         FxSustainPacket::handle,
         Optional.of(NetworkDirection.PLAY_TO_CLIENT)
      );
   }

   public static void sendBloodPrisonDomain(ServerPlayer player, int durationTicks) {
      CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new BloodPrisonDomainPacket(durationTicks));
   }
}
