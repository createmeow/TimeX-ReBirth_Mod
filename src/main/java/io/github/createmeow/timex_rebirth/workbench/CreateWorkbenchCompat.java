package io.github.createmeow.timex_rebirth.workbench;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * Create 序列组装组件桥接器：
 * 将 create:sequenced_assembly 组件与 BlockEntity 的 shears/hammer/saw 状态互相转换。
 * <p>
 * 步骤映射：
 * <ul>
 *   <li>step 0 → 无工具</li>
 *   <li>step 1 → shears</li>
 *   <li>step 2 → shears + hammer</li>
 *   <li>step 3 → shears + hammer + saw（完成）</li>
 * </ul>
 */
public final class CreateWorkbenchCompat {

    private CreateWorkbenchCompat() {}

    public static boolean isCreateLoaded() {
        return ModList.get().isLoaded("create");
    }

    /** 从 ItemStack 的 create:sequenced_assembly 组件读取已完成的步骤数（-1 表示无组件）。 */
    public static int getCreateStep(ItemStack stack) {
        if (!isCreateLoaded()) return -1;
        return CreateBridge.getStep(stack);
    }

    /** 将 BlockEntity 的已安装工具数写入 ItemStack 的 create:sequenced_assembly 组件。 */
    public static void setCreateStep(ItemStack stack, int installedCount) {
        if (!isCreateLoaded()) return;
        if (installedCount <= 0) return;
        float progress = installedCount / 3.0f;
        CreateBridge.setStep(stack, installedCount, progress);
    }

    /** 内部桥接类：直接引用 Create 的类，仅在 Create 已加载时被调用。 */
    private static final class CreateBridge {
        private static int getStep(ItemStack stack) {
            com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe.SequencedAssembly sa =
                    stack.get(com.simibubi.create.AllDataComponents.SEQUENCED_ASSEMBLY);
            return sa != null ? sa.step() : -1;
        }

        private static void setStep(ItemStack stack, int step, float progress) {
            com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe.SequencedAssembly sa =
                    new com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe.SequencedAssembly(
                            ResourceLocation.parse("timex_rebirth:workbench_assembly"),
                            step, progress);
            stack.set(com.simibubi.create.AllDataComponents.SEQUENCED_ASSEMBLY, sa);
        }
    }
}
