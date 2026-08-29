package io.github.createmeow.timex_rebirth.features;

import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import vectorwing.farmersdelight.common.block.AbstractStoveBlock;

/**
 * 火焰方块交互（事件层，避免与原版/FD 的 useItemOn 深度耦合）。
 *
 * <p><b>燃料系统适用范围</b>：仅"自带燃烧状态"的方块——原版篝火（含灵魂）与 FD 炉灶。
 * FD 厨锅/煎锅依赖外部热源（下方 #farmersdelight:heat_sources），无自身燃料，不参与本系统。</p>
 *
 * <ul>
 *   <li><b>熄灭状态</b>：
 *     - 手持可燃物右键 → 填充燃料（消耗 1 个）；已满则拦截提示；</li>
 *     - 手持打火石/火弹 → 仅当剩余燃料 &gt; 0 才放行点燃（否则拦截并提示"无燃料"）；</li>
 *   <li><b>空手右键</b>：查看剩余燃烧时间/状态（参考 FrostedHeart campfire.remaining）。</li>
 * </ul>
 */
@EventBusSubscriber(modid = TimeX.MODID)
public class FireInteractHandler {

    /** 是否为纳入燃料系统的火焰方块（自带 LIT 燃烧状态）。 */
    private static boolean isFuelFireBlock(BlockState state) {
        return state.getBlock() instanceof CampfireBlock
                || state.getBlock() instanceof net.minecraft.world.level.block.AbstractFurnaceBlock
                || (TimeXFdCheck.fdLoaded() && state.getBlock() instanceof AbstractStoveBlock);
    }

    /** FD 兼容判定（类隔离，FD 不存在时不加载其类）。 */
    private static class TimeXFdCheck {
        static boolean fdLoaded() {
            return net.neoforged.fml.ModList.get().isLoaded("farmersdelight");
        }
    }

    /** 检查物品是否为燃烧状态的燃料（仅"点燃"版本的绒毛/涂蜡纸板，且有剩余耐久）。 */
    private static boolean isLitFuel(ItemStack stack) {
        if (stack.isEmpty()) return false;
        // 严格只认"点燃"版本物品：未点燃的纸板(waxed_cardboard)/湿绒毛(wet_fuzz)不视为燃烧
        if (stack.is(FireToolRegistry.LIT_FUZZ.get())) {
            return stack.getMaxDamage() > stack.getDamageValue();
        }
        if (stack.is(FireToolRegistry.LIT_WAXED_CARDBOARD.get())) {
            return stack.getMaxDamage() > stack.getDamageValue();
        }
        return false;
    }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        boolean isFurnace = state.getBlock() instanceof net.minecraft.world.level.block.AbstractFurnaceBlock;
        if (!isFuelFireBlock(state)) return;

        Player player = event.getEntity();
        InteractionHand hand = event.getHand();
        ItemStack held = player.getItemInHand(hand);
        // 熔炉的 LIT 由 BE litTime 驱动，附件燃料是"储备"；篝火/炉灶直接看 LIT
        boolean lit = FireFuelData.isLit(state);

        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return;

        // ── 熔炉类：燃料走 GUI 燃料槽，这里只处理打火与状态查看 ──
        if (isFurnace) {
            // 手持打火石/火弹 + 熄灭状态 → 点燃（燃料槽有可燃物才允许）
            if (!lit && (held.getItem() instanceof FlintAndSteelItem || held.is(Items.FIRE_CHARGE))) {
                if (FireManager.tryIgniteFurnace(level, pos, state, be)) {
                    // 添加打火石火花粒子效果（仅在服务端）
                    if (!level.isClientSide()) {
                        double offsetX = level.getRandom().nextDouble() * 0.6 - 0.3;
                        double offsetZ = level.getRandom().nextDouble() * 0.6 - 0.3;
                        ((net.minecraft.server.level.ServerLevel) level).sendParticles(ParticleTypes.LAVA,
                                pos.getX() + 0.5 + offsetX,
                                pos.getY() + 0.5,
                                pos.getZ() + 0.5 + offsetZ,
                                1, 0, 0.1, 0, 0);
                    }
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                }
                // 未点燃成功（燃料槽无可燃物）：放行原版逻辑
                return;
            }
            // 手持"燃烧中的绒毛/涂蜡纸板" + 熄灭状态 → 视为打火石（消耗 1 点耐久）
            if (!lit && (held.is(FireToolRegistry.LIT_FUZZ.get()) || held.is(FireToolRegistry.LIT_WAXED_CARDBOARD.get()))) {
                if (player.isShiftKeyDown()) {
                    if (be instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity furnace) {
                        // 燃料槽为空 → 直接放入；非空 → 不破坏现有燃料，仅消耗 1 耐久打火
                        ItemStack fuelSlot = furnace.getItem(1);
                        if (fuelSlot.isEmpty()) {
                            // 整组放入燃料槽
                            furnace.setItem(1, held.split(held.getCount()));
                            furnace.setChanged();
                            // 放置后立即授予点火许可（自带燃烧状态）
                            FireManager.tryIgniteFurnace(level, pos, state, be);
                            level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.8F, 1.0F);
                            event.setCanceled(true);
                            event.setCancellationResult(InteractionResult.SUCCESS);
                            return;
                        }
                        // 燃料槽非空：直接当打火石用，扣 1 耐久
                        if (FireManager.tryIgniteFurnace(level, pos, state, be)) {
                            if (!player.getAbilities().instabuild) {
                                held.hurtAndBreak(1, player,
                                        hand == InteractionHand.MAIN_HAND
                                                ? net.minecraft.world.entity.EquipmentSlot.MAINHAND
                                                : net.minecraft.world.entity.EquipmentSlot.OFFHAND);
                            }
                            level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 0.8F, 1.0F);
                            event.setCanceled(true);
                            event.setCancellationResult(InteractionResult.SUCCESS);
                        }
                    }
                    return;
                }
            }
            // 空手右键 → 查看燃烧状态
            if (held.isEmpty() && hand == InteractionHand.MAIN_HAND) {
                FireManager.showStatus(level, pos, state, be, player);
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
            return;
        }

        // ── 篝火/炉灶：任意状态可填充燃料；熄灭时可打火点燃 ──
// 0. 手持"燃烧中的绒毛/涂蜡纸板" + 熄灭状态 + SHIFT → 当打火石（消耗 1 耐久）
//    只有"燃烧状态"的纸板/绒毛（LIT 版本）才能用作打火石；主手或副手持有均可
boolean litMain = isLitFuel(player.getItemInHand(InteractionHand.MAIN_HAND));
boolean litOff = isLitFuel(player.getItemInHand(InteractionHand.OFF_HAND));
if (!lit && player.isShiftKeyDown() && (litMain || litOff)) {
            FireFuelData data = FireFuelData.of(be);
            int fuel = data == null ? 0 : data.getFuelTicks();
            if (fuel > 0 && FireManager.tryIgnite(level, pos, state, be, player)) {
                // 消耗耐久（同时检查主手和副手）
                if (!player.getAbilities().instabuild) {
                    ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
                    ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
                    if (isLitFuel(mainHand)) {
                        mainHand.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                    }
                    if (isLitFuel(offHand)) {
                        offHand.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.OFFHAND);
                    }
                }
                level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 0.8F, 1.0F);
                // 添加火花粒子效果（仅在服务端）
                if (!level.isClientSide()) {
                    double offsetX = level.getRandom().nextDouble() * 0.6 - 0.3;
                    double offsetZ = level.getRandom().nextDouble() * 0.6 - 0.3;
                    ((net.minecraft.server.level.ServerLevel) level).sendParticles(ParticleTypes.LAVA,
                            pos.getX() + 0.5 + offsetX,
                            pos.getY() + 0.5,
                            pos.getZ() + 0.5 + offsetZ,
                            1, 0, 0.1, 0, 0);
                }
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                return;
            }
        }
        // 1. 手持可燃物（非打火石/火弹）→ 填充储备（燃烧中续柴同样有效）
        if (!held.isEmpty() && !(held.getItem() instanceof FlintAndSteelItem) && !held.is(Items.FIRE_CHARGE)) {
            FireManager.AddFuelResult result = FireManager.tryAddFuel(level, pos, be, player, hand);
            if (result == FireManager.AddFuelResult.ADDED || result == FireManager.AddFuelResult.FULL) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                return;
            }
            // NOT_FUEL：交给原版逻辑
            return;
        }
        // 2. 熄灭时手持打火石/火弹 → 需有储备燃料才可点燃；无燃料则拦截并提示
        if (!lit && (held.getItem() instanceof FlintAndSteelItem || held.is(Items.FIRE_CHARGE))) {
            FireFuelData data = FireFuelData.of(be);
            int fuel = data == null ? 0 : data.getFuelTicks();
            if (fuel <= 0) {
                if (!level.isClientSide()) {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable("message.timex_rebirth.no_fuel"), true);
                }
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                return;
            }
            // 有燃料：放行给原版/FD 点燃逻辑
            return;
        }

        // ── 篝火/炉灶：空手右键（任意状态）→ 查看剩余燃烧时间/状态 ──
        if (held.isEmpty() && hand == InteractionHand.MAIN_HAND) {
            FireManager.showStatus(level, pos, state, be, player);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /**
     * 火焰蔓延引燃：当火焰方块放置/变化时，主动引燃其六向邻居。
     * 原版火焰在无可燃物支撑时会很快自然消亡，因此引燃必须在放火瞬间完成。
     */
    @SubscribeEvent
    public static void onNeighborChanged(BlockEvent.NeighborNotifyEvent event) {
        if (event.getLevel().isClientSide()) return;
        Level level = (Level) event.getLevel();
        BlockState state = level.getBlockState(event.getPos());
        // 只有"原版火焰方块"出现时才触发（营火本身不算明火）
        if (!(state.getBlock() instanceof BaseFireBlock)) return;

        igniteNeighbors(level, event.getPos());
    }

    /**
     * 检测 pos 位置的方块是否"可燃"且六向邻居中存在原版火焰：
     * 若是，自动执行对应点燃逻辑（绒毛→烧毁+掉落 lit_fuzz / 未点燃纸板→替换为 lit 版 /
     * 木条→烧毁 / 熔炉/篝火/炉灶→授予点火许可）。
     *
     * <p>由方块 {@code onPlace} 调用，解决"放在已有火焰旁边"场景——此时 {@link #onNeighborChanged}
     * 不会被新火焰触发。</p>
     *
     * @return 是否被成功引燃
     */
    public static boolean igniteSelfIfFlammable(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        // 干燥的木条：被引燃 → 烧毁并放火
        if (state.getBlock() instanceof DryKindlingBlock) {
            level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState());
            return true;
        }
        // 绒毛堆：被引燃 → 变火焰并掉落 lit_fuzz
        if (state.getBlock() instanceof FuzzPileBlock) {
            int fuzzCount = state.getValue(FuzzPileBlock.FUZZ);
            ItemStack litFuzz = new ItemStack(FireToolRegistry.LIT_FUZZ.get());
            LitFuzzItem.initFromFuzzCount(litFuzz, fuzzCount);
            level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState());
            net.minecraft.world.Containers.dropItemStack(level,
                    pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, litFuzz);
            return true;
        }
        // 未点燃的涂蜡纸板：被引燃 → 替换为点燃方块（保留剩余秒数）
        if (state.getBlock() instanceof UnlitWaxedCardboardBlock) {
            if (level.getBlockEntity(pos) instanceof UnlitWaxedCardboardBlockEntity be) {
                int remaining = Math.max(1, be.remainingSeconds());
                level.setBlockAndUpdate(pos,
                        FireToolRegistry.LIT_WAXED_CARDBOARD_BLOCK.get().defaultBlockState());
                if (level.getBlockEntity(pos) instanceof LitWaxedCardboardBlockEntity litBe) {
                    litBe.getData(FireManager.FUEL_DATA).setFuelTicks(remaining);
                    litBe.setChanged();
                }
                return true;
            }
        }
        // 熔炉类：检查燃料槽有可燃物后授予点火许可
        if (state.getBlock() instanceof net.minecraft.world.level.block.AbstractFurnaceBlock) {
            BlockEntity be = level.getBlockEntity(pos);
            return be != null && FireManager.tryIgniteFurnace(level, pos, state, be);
        }
        // 篝火/炉灶：附件有燃料则直接点燃
        if (isFuelFireBlock(state)) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be == null) return false;
            if (FireFuelData.isLit(state)) return false;
            FireFuelData data = FireFuelData.of(be);
            if (data != null && data.hasFuel()) {
                FireFuelData.setLit(level, pos, state, true);
                return true;
            }
        }
        return false;
    }

    /**
     * 检测 pos 的六向邻居中是否存在原版火焰方块。
     * 用于方块放置时主动检测"附近有火"。
     */
    public static boolean hasAdjacentFireSource(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            if (level.getBlockState(pos.relative(dir)).getBlock() instanceof BaseFireBlock) {
                return true;
            }
        }
        return false;
    }

    /**
     * 引燃 pos 的六向邻居中"有燃料且未点燃"的目标：
     * <ul>
     *   <li>干燥的木条 → 烧毁并变火焰（生火中转）；</li>
     *   <li>未点燃的涂蜡纸板 → 替换为点燃方块（保留剩余秒数）；</li>
     *   <li>熔炉类 → 检查 GUI 燃料槽有可燃物后授予点火许可；</li>
     *   <li>篝火/炉灶 → 附件有储备燃料则直接点燃。</li>
     * </ul>
     *
     * @return 是否至少引燃了一个目标
     */
    public static boolean igniteNeighbors(Level level, BlockPos pos) {
        boolean ignited = false;
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            BlockState nb = level.getBlockState(neighbor);

            // 干燥的木条：直接烧毁并放置火焰（作为生火中转物）
            if (nb.getBlock() instanceof DryKindlingBlock) {
                level.removeBlock(neighbor, false);
                level.setBlockAndUpdate(neighbor, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState());
                ignited = true;
                continue;
            }

            // 绒毛堆：被点燃 → 变火焰并掉落"点燃的绒毛"（耐久 = 绒毛数量 × 10）
            if (nb.getBlock() instanceof FuzzPileBlock) {
                int fuzzCount = nb.getValue(FuzzPileBlock.FUZZ);
                ItemStack litFuzz = new ItemStack(FireToolRegistry.LIT_FUZZ.get());
                LitFuzzItem.initFromFuzzCount(litFuzz, fuzzCount);
                level.setBlockAndUpdate(neighbor, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState());
                net.minecraft.world.Containers.dropItemStack(level,
                        neighbor.getX() + 0.5, neighbor.getY() + 0.3, neighbor.getZ() + 0.5,
                        litFuzz);
                ignited = true;
                continue;
            }

            // 未点燃的涂蜡纸板：替换为点燃方块，转移剩余秒数
            if (nb.getBlock() instanceof UnlitWaxedCardboardBlock) {
                if (level.getBlockEntity(neighbor) instanceof UnlitWaxedCardboardBlockEntity be) {
                    int remaining = Math.max(1, be.remainingSeconds());
                    level.setBlockAndUpdate(neighbor,
                            FireToolRegistry.LIT_WAXED_CARDBOARD_BLOCK.get().defaultBlockState());
                    if (level.getBlockEntity(neighbor) instanceof LitWaxedCardboardBlockEntity litBe) {
                        litBe.getData(FireManager.FUEL_DATA).setFuelTicks(remaining);
                        litBe.setChanged();
                    }
                    ignited = true;
                }
                continue;
            }

            if (!isFuelFireBlock(nb)) continue;
            BlockEntity be = level.getBlockEntity(neighbor);
            if (be == null) continue;
            if (FireFuelData.isLit(nb)) continue; // 已点燃

            // 熔炉类：燃料槽有可燃物 → 授予点火许可（原版逻辑自行消耗燃料槽）
            if (nb.getBlock() instanceof net.minecraft.world.level.block.AbstractFurnaceBlock) {
                ignited |= FireManager.tryIgniteFurnace(level, neighbor, nb, be);
                continue;
            }
            // 篝火/炉灶：附件有燃料 → 直接点燃
            FireFuelData data = FireFuelData.of(be);
            if (data != null && data.hasFuel()) {
                FireFuelData.setLit(level, neighbor, nb, true);
                ignited = true;
            }
        }
        return ignited;
    }
}
