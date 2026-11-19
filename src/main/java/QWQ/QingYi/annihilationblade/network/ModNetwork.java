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
    * 2：新增通用特效事件包 FxEventPacket；3：新增持续特效通道 FxSustainPacket。
    * 新旧版本客户端/服务端互相拒绝连接以免解析错乱。
    */
   private static final String PROTOCOL_VERSION = "3";
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
