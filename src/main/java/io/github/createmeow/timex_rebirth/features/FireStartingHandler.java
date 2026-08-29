package io.github.createmeow.timex_rebirth.features;

import io.github.createmeow.timex_rebirth.TimeX;
import io.github.createmeow.timex_rebirth.TimeXConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * 生火方式（替代被移除的打火石合成）：
 *
 * <p><b>1. 双手打火</b>：主手铁锭/锌锭（或其粒）+ 副手燧石（可互换），对可燃方块/地面右键生火。
 * 每次成功率 {@code fire.steel_ignition_chance}（默认 25%）；
 * 无论成败均有 {@code fire.consume_chance}（默认 10%）概率随机消耗燧石或金属其中之一。
 * 目标是<b>有燃料的熄灭篝火/炉灶</b>时，成功则直接点燃该方块（参考 FrostedHeart lightingFire）。</p>
 *
 * <p><b>2. 木棍钻木取火</b>：手持木棍对准"干燥的木条"（{@link DryKindlingBlock}）右键，
 * 每次成功率 {@code fire.stick_ignition_chance}（默认 20%）且必定消耗 1 个木棍；成功时点燃该木条。</p>
 */
@EventBusSubscriber(modid = TimeX.MODID)
public class FireStartingHandler {

    private static boolean isSteelBar(ItemStack stack) {
        return stack.is(Items.IRON_INGOT) || stack.is(Items.IRON_NUGGET)
                || stack.is(TimeXCompatItems.zincIngot())
                || stack.is(TimeXCompatItems.zincNugget());
    }

    /** Create 锌锭/锌粒的注册名（Create 未安装时返回 AIR 判定 false）。 */
    private static class TimeXCompatItems {
        static net.minecraft.world.item.Item zincIngot() {
            return net.minecraft.core.registries.BuiltInRegistries.ITEM
                    .get(net.minecraft.resources.ResourceLocation.parse("create:zinc_ingot"));
        }

        static net.minecraft.world.item.Item zincNugget() {
            return net.minecraft.core.registries.BuiltInRegistries.ITEM
                    .get(net.minecraft.resources.ResourceLocation.parse("create:zinc_nugget"));
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // 防止客户端和服务端都处理同一个事件（避免木棍被消耗两次）
        if (event.isCanceled()) return;

        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockPos above = pos.above();
        ItemStack main = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack off = player.getItemInHand(InteractionHand.OFF_HAND);
        BlockState target = level.getBlockState(pos);

        // ── 方式 2：木棍 + 干燥的木条 → 钻木取火 ──
        if (target.getBlock() instanceof DryKindlingBlock && main.is(Items.STICK)) {
            if (level.isClientSide()) {
                player.swing(InteractionHand.MAIN_HAND);
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true); // 标记事件已处理
                return;
            }
            main.shrink(1); // 必定消耗
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            // 露天 + 有降水时，钻木取火成功率降为原来的 1/3（木条被淋湿更难引燃）
            if (level.random.nextDouble() < effectiveIgnitionChance(level, pos,
                    TimeXConfig.FIRE_STICK_IGNITION_CHANCE.get())) {
                // 木条被点燃为火焰，同时主动引燃六向邻居（篝火/灶台/熔炉/其它木条）
                level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState());
                FireInteractHandler.igniteNeighbors(level, pos);
                level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.8F, 1.0F);
                player.displayClientMessage(Component.translatable("message.timex_rebirth.fire_success"), true);
                // 添加少量篝火黑烟粒子
                if (!level.isClientSide()) {
                    for (int i = 0; i < 3; i++) {
                        double offsetX = (level.getRandom().nextDouble() - 0.5) * 0.3;
                        double offsetY = (level.getRandom().nextDouble() - 0.5) * 0.3;
                        double offsetZ = (level.getRandom().nextDouble() - 0.5) * 0.3;
                        ((net.minecraft.server.level.ServerLevel) level).sendParticles(ParticleTypes.LARGE_SMOKE,
                                pos.getX() + 0.5 + offsetX,
                                pos.getY() + 0.3,
                                pos.getZ() + 0.5 + offsetZ,
                                1, 0, 0.1, 0, 0);
                    }
                }
            } else {
                level.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.8F, 0.6F);
                player.displayClientMessage(Component.translatable("message.timex_rebirth.fire_fail"), true);
                // 添加少量黑烟粒子
                if (!level.isClientSide()) {
                    for (int i = 0; i < 2; i++) {
                        double offsetX = (level.getRandom().nextDouble() - 0.5) * 0.3;
                        double offsetY = (level.getRandom().nextDouble() - 0.5) * 0.3;
                        double offsetZ = (level.getRandom().nextDouble() - 0.5) * 0.3;
                        ((net.minecraft.server.level.ServerLevel) level).sendParticles(ParticleTypes.LARGE_SMOKE,
                                pos.getX() + 0.5 + offsetX,
                                pos.getY() + 0.3,
                                pos.getZ() + 0.5 + offsetZ,
                                1, 0, 0.1, 0, 0);
                    }
                }
            }
            return;
        }

        // ── 方式 1：主手金属锭/粒 + 副手燧石 → 双手打火（允许互换）──
        boolean steelMain = isSteelBar(main);
        boolean flintOff = off.is(Items.FLINT);
        boolean steelOff = isSteelBar(off);
        boolean flintMain = main.is(Items.FLINT);
        boolean steelAndFlint = (steelMain && flintOff) || (steelOff && flintMain);
        if (!steelAndFlint) return;

        // 对准火焰方块不处理（填充燃料/打火由 FireInteractHandler 处理）；
        // 干燥木条允许作为生火目标（在其上方放火）
        if (target.getBlock() instanceof BaseFireBlock) return;

        event.setCanceled(true);
        if (level.isClientSide()) {
            player.swing(InteractionHand.MAIN_HAND);
            return;
        }
        level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 0.8F,
                (level.random.nextFloat() - level.random.nextFloat()) * 0.2F + 1.0F);

        // 露天 + 有降水时，打火石生火成功率降为原来的 1/3
        boolean success = level.random.nextDouble() < effectiveIgnitionChance(level, pos,
                TimeXConfig.FIRE_STEEL_IGNITION_CHANCE.get());

        // 无论成败均有概率磨损：10% 概率从燧石/金属中随机消耗一个
        if (level.random.nextDouble() < TimeXConfig.FIRE_CONSUME_CHANCE.get()) {
            ItemStack consumeTarget = level.random.nextBoolean() ? main : off;
            consumeTarget.shrink(1);
        }

        // 目标是有燃料的熄灭篝火/炉灶 → 成功时直接点燃该方块；
        // 否则成功时在上方放火并引燃邻居（含干燥木条、贴邻的篝火/灶台/熔炉）
        BlockEntityIgnition.tryIgniteTarget(level, pos, target, success);

        if (success) {
            BlockPos firePos = target.getBlock() instanceof DryKindlingBlock ? pos : above;
            if (!BlockEntityIgnition.wasHandled() && level.getBlockState(firePos).canBeReplaced()) {
                level.setBlockAndUpdate(firePos, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState());
            }
            FireInteractHandler.igniteNeighbors(level, firePos);
            level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            player.displayClientMessage(Component.translatable("message.timex_rebirth.fire_success"), true);
        } else {
            player.displayClientMessage(Component.translatable("message.timex_rebirth.fire_fail"), true);
        }
    }

    /**
     * 生火基础成功率经天气修正后的值：露天且正有降水（雨/雪/雷雨/暴风雪）时降为原来的 1/3。
     * 本模组天气由 WeatherSystem 同步到原版，isRaining 覆盖所有非晴天降水。
     */
    private static double effectiveIgnitionChance(Level level, BlockPos pos, double base) {
        return (level.isRaining() && level.canSeeSky(pos)) ? base / 3.0 : base;
    }

    /**
     * 点燃目标方块（若是带附件数据的火焰类 BE），并记录是否已处理。
     */
    private static class BlockEntityIgnition {
        private static boolean handled;

        static void tryIgniteTarget(Level level, BlockPos pos, BlockState state, boolean success) {
            handled = false;
            if (!success) return;
            if (!(state.getBlock() instanceof CampfireBlock)
                    && !FdStoveCheck.isStove(state)) return;
            var be = level.getBlockEntity(pos);
            if (be == null) return;
            // 直接传 null，tryIgnite 内部会检查 null 并返回 false
            if (FireManager.tryIgnite(level, pos, state, be, null)) {
                handled = true;
            }
        }

        static boolean wasHandled() {
            return handled;
        }
    }

    /** FD 炉灶判定隔离类（厨锅/煎锅依赖外部热源，不在燃料系统内）。 */
    private static class FdStoveCheck {
        static boolean isStove(BlockState state) {
            return net.neoforged.fml.ModList.get().isLoaded("farmersdelight")
                    && state.getBlock() instanceof vectorwing.farmersdelight.common.block.AbstractStoveBlock;
        }
    }
}
