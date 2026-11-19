package QWQ.QingYi.annihilationblade.render.network;

import QWQ.QingYi.annihilationblade.render.client.FxClientDispatcher;

/**
 * 网络包与客户端渲染器之间的隔离层。
 * <p>
 * 只会在 {@code DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)} 内部被调用，
 * 因此专用服务端永远不会解析到 {@link FxClientDispatcher} 及其依赖的客户端类。
 */
final class FxClientBridge {
   private FxClientBridge() {
   }

   static void dispatch(FxEventPacket packet) {
      FxClientDispatcher.handle(packet);
   }
}
