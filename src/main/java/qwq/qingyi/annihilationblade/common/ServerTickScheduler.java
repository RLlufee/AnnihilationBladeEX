package qwq.qingyi.annihilationblade.common;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import qwq.qingyi.annihilationblade.AnnihilationBladeex;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 服务端延迟任务调度器。
 * 在服务端每 tick 末递减剩余刻数并在归零时执行回调，支持迭代过程中安全重入追加新任务。
 */
public final class ServerTickScheduler {
    private static final List<DelayedTask> TASKS = new ArrayList<>();
    private static final List<DelayedTask> PENDING_ADDS = new ArrayList<>();
    private static boolean iterating;

    private ServerTickScheduler() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> onServerTick());
    }

    public static void schedule(int ticks, Runnable action) {
        if (action != null) {
            DelayedTask task = new DelayedTask(Math.max(0, ticks), action);
            if (iterating) {
                PENDING_ADDS.add(task);
            } else {
                TASKS.add(task);
            }
        }
    }

    private static void onServerTick() {
        iterating = true;
        Iterator<DelayedTask> iterator = TASKS.iterator();

        while (iterator.hasNext()) {
            DelayedTask task = iterator.next();
            task.remainingTicks--;
            if (task.remainingTicks <= 0) {
                try {
                    task.action.run();
                } catch (RuntimeException exception) {
                    AnnihilationBladeex.LOGGER.warn("执行延时任务失败", exception);
                }

                iterator.remove();
            }
        }

        iterating = false;
        if (!PENDING_ADDS.isEmpty()) {
            TASKS.addAll(PENDING_ADDS);
            PENDING_ADDS.clear();
        }
    }

    private static final class DelayedTask {
        private int remainingTicks;
        private final Runnable action;

        private DelayedTask(int remainingTicks, Runnable action) {
            this.remainingTicks = remainingTicks;
            this.action = action;
        }
    }
}
