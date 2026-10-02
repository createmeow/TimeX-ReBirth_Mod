package io.github.createmeow.timex_rebirth.compat;

import io.github.createmeow.timex_rebirth.TimeX;
import io.github.createmeow.timex_rebirth.food.SupplyFoodRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Create 搅拌盆右键装瓶（事件层，避免对 Create 内部类的硬依赖/注入）。
 *
 * <p><b>背景</b>：Create 玩家手持玻璃瓶右键盆地时走
 * {@code BasinBlock.useItemOn -> FluidHelper.tryFillItemFromBE -> GenericItemFilling}，
 * 其 {@code canFillGlassBottleInternally} 硬编码白名单仅放行 水/药水/茶（无扩展点），
 * 电解质水不在其中；我们的 create:filling 配方只对封口机（Spout）生效。
 * 故在此拦截 RightClickBlock：手持玻璃瓶 + 盆地存有 ≥250mb 电解质水时
 * 直接装瓶（消耗 250mb 流体 + 1 玻璃瓶，产出瓶装电解质水）。</p>
 *
 * <p><b>兼容性</b>：方块/流体均按注册表对象比较，不 import 任何 Create 类——
 * Create 未加载时盆地方块不存在，事件直接返回；装出的瓶装电解质水
 * 无 ThirstWasTaken 纯净度标记（按默认纯净水处理，效果一致）。</p>
 */
@EventBusSubscriber(modid = TimeX.MODID)
public class BasinBottleFillingHandler {

    /** 一瓶电解质水的流体量（与 create:filling 配方一致）。 */
    private static final int BOTTLE_AMOUNT = 250;

    private static boolean isCreateBasin(BlockState state) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return "create".equals(key.getNamespace()) && "basin".equals(key.getPath());
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        ItemStack held = event.getEntity().getItemInHand(event.getHand());
        if (!held.is(Items.GLASS_BOTTLE)) return;
        if (!isCreateBasin(level.getBlockState(event.getPos()))) return;

        IFluidHandler tank = level.getCapability(Capabilities.FluidHandler.BLOCK, event.getPos(), null);
        if (tank == null) return;

        for (int i = 0; i < tank.getTanks(); i++) {
            FluidStack in = tank.getFluidInTank(i);
            if (!in.is(SupplyFoodRegistry.ELECTROLYTE_WATER_FLUID.get()) || in.getAmount() < BOTTLE_AMOUNT)
                continue;

            // 客户端只取消并返回成功（服务端执行实际装取）；流体数据客户端未同步时以服务端判定为准
            if (level.isClientSide()) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                return;
            }

            FluidStack drained = tank.drain(new FluidStack(in.getFluid(), BOTTLE_AMOUNT),
                    IFluidHandler.FluidAction.EXECUTE);
            if (drained.getAmount() < BOTTLE_AMOUNT)
                return;

            ItemStack bottle = new ItemStack(SupplyFoodRegistry.ELECTROLYTE_WATER_BOTTLE.get());
            Player player = event.getEntity();
            held.shrink(1);
            if (held.isEmpty()) {
                player.setItemInHand(event.getHand(), bottle);
            } else if (!player.getInventory().add(bottle)) {
                player.drop(bottle, false);
            }
            level.playSound(null, event.getPos(), SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
    }
}
