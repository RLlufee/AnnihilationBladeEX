package qwq.qingyi.annihilationblade.client.render;

/**
 * 用于向原版 {@code GuiGraphicsExtractor} 注入自定义延迟 Tooltip 渲染逻辑的桥接接口。
 */
public interface GuiGraphicsExtractorAccess {
    void annihilationbladeex$setDeferredTooltip(Runnable runnable);
}
