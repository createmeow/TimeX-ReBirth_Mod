package io.github.createmeow.timex_rebirth.features;

import io.github.createmeow.timex_rebirth.TimeX;
import io.github.createmeow.timex_rebirth.TimeXConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * 火焰管理：篝火 / 农夫乐事炉灶·厨锅的"燃料时长 + 打火点燃"机制
 * （设计参考 FrostedHeart 的篝火限时燃烧）。
 *
 * <p>规则：
 * <ul>
 *   <li><b>不可虚空燃烧</b>：放置时无燃料 → 强制熄灭（见各 Mixin）。</li>
 *   <li><b>填充燃料</b>：可燃物由熔炉燃料表判定；每次消耗 1 个，
 *       累加燃烧 tick = 燃烧值 × 倍率（{@code fire.fuel_multiplier}，默认 3），
 *       总量上限 {@code fire.fuel_cap_ticks}（默认 19200 ≈ 16 分钟）。</li>
 *   <li><b>丢弃添柴</b>：将可燃物品实体丢到熄灭/燃烧的火焰方块上自动吸收（见 CampfireStepOnMixin）。</li>
 *   <li><b>打火点燃</b>：仅当剩余燃料 &gt; 0 时才允许点燃；空燃时打火无效并提示。</li>
 *   <li><b>燃尽熄灭</b>：每 tick 扣 1，归零自动熄灭并播放熄灭音效。</li>
 * </ul>
 */
public class FireManager {

    /** 方块实体附件：篝火/炉灶/厨锅的剩余燃烧 tick。 */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<FireFuelData>> FUEL_DATA =
            DeferredHolder.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, TimeX.rl("fire_fuel"));

    /** 注册附件类型。 */
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(FireManager::onRegisterAttachments);
    }

    private static void onRegisterAttachments(RegisterEvent event) {
        if (event.getRegistryKey().equals(NeoForgeRegistries.Keys.ATTACHMENT_TYPES)) {
            event.register(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, TimeX.rl("fire_fuel"),
                    () -> AttachmentType.serializable(FireFuelData::new).build());
        }
    }

    // ─────────────────────────── 燃料数值 ───────────────────────────

    /** 单个物品可提供的燃烧 tick = 熔炉燃烧值 × 倍率。非可燃物返回 0。 */
    public static int burnTicksOf(ItemStack stack) {
        int burnTime = stack.getItem().getBurnTime(stack, null);
        if (burnTime <= 0) return 0;
        return burnTime * TimeXConfig.FIRE_FUEL_MULTIPLIER.get();
    }

    private static int cap() {
        return TimeXConfig.FIRE_FUEL_CAP_TICKS.get();
    }

    // ─────────────────────────── 填充燃料 ───────────────────────────

    /** 右键填充结果。 */
    public enum AddFuelResult {
        /** 物品不可燃，未消费交互。 */
        NOT_FUEL,
        /** 已满，无法再添加（已提示）。 */
        FULL,
        /** 添加成功。 */
        ADDED
    }

    /**
     * 手持可燃物右键火焰方块：消耗 1 个并累加燃烧时长。
     */
    public static AddFuelResult tryAddFuel(Level level, BlockPos pos, BlockEntity be, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) return AddFuelResult.NOT_FUEL;

        int burnTicks = burnTicksOf(held);
        if (burnTicks <= 0) return AddFuelResult.NOT_FUEL; // 不可燃：交给原版逻辑

        FireFuelData data = FireFuelData.of(be);
        if (data == null) return AddFuelResult.NOT_FUEL;

        int room = cap() - data.getFuelTicks();
        if (room <= 0) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.timex_rebirth.fuel_full"), true);
            }
            return AddFuelResult.FULL;
        }

        data.addFuel(Math.min(burnTicks, room));
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        if (!level.isClientSide()) {
            level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.6F, 1.2F);
            // 先有火、后加燃料：若六向邻居已有明火，填充后立即点燃
            BlockState curState = level.getBlockState(pos);
            if (!FireFuelData.isLit(curState)) {
                for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                    if (level.getBlockState(pos.relative(dir)).getBlock() instanceof net.minecraft.world.level.block.BaseFireBlock) {
                        FireFuelData.setLit(level, pos, curState, true);
                        break;
                    }
                }
            }
        }
        player.displayClientMessage(Component.translatable("message.timex_rebirth.fuel_added",
                data.getFuelTicks() / 20), true);
        return AddFuelResult.ADDED;
    }

    /**
     * 吸收丢到火焰方块上的物品实体（丢弃添柴，参考 FrostedHeart stepOn）。
     *
     * @return true 表示该实体被完全或部分吸收（应继续保留/更新实体）
     */
    public static boolean absorbItemEntity(Level level, BlockPos pos, BlockState state,
                                           net.minecraft.world.entity.item.ItemEntity entity) {
        ItemStack stack = entity.getItem();
        int perItem = burnTicksOf(stack);
        if (perItem <= 0) return false; // 非可燃物不吸收

        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return false;
        FireFuelData data = FireFuelData.of(be);
        if (data == null) return false;

        boolean lit = FireFuelData.isLit(state);
        // FrostedHeart 行为：只吸收"已点燃"或"已有燃料"的火焰；
        // 完全空燃且熄灭的火不允许靠丢物复燃（需手动打火）
        if (!lit && !data.hasFuel()) return false;

        int room = cap() - data.getFuelTicks();
        if (room <= 0) return false;

        int count = Math.min(room / perItem, stack.getCount());
        if (count <= 0) return false;

        data.addFuel(perItem * count);
        // 退还容器（如桶装燃料等）
        ItemStack container = stack.getCraftingRemainingItem();
        stack.shrink(count);
        if (stack.getCount() <= 0) {
            entity.discard();
        }
        if (!container.isEmpty()) {
            net.minecraft.world.Containers.dropItemStack(level,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, container);
        }
        level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.6F, 1.2F);
        return true;
    }

    // ─────────────────────────── 点燃与消耗 ───────────────────────────

    /**
     * 尝试点燃（打火）：仅当剩余燃料 &gt; 0 时允许。
     *
     * @return true=点燃成功；false=燃料不足（已向玩家提示）
     */
    public static boolean tryIgnite(Level level, BlockPos pos, BlockState state, BlockEntity be, Player player) {
        FireFuelData data = FireFuelData.of(be);
        int fuel = data == null ? 0 : data.getFuelTicks();
        if (fuel <= 0) {
            if (!level.isClientSide() && player != null) {
                player.displayClientMessage(Component.translatable("message.timex_rebirth.no_fuel"), true);
            }
            return false;
        }
        return FireFuelData.setLit(level, pos, state, true);
    }

    /**
     * 空手右键查询火焰状态（参考 FrostedHeart campfire.remaining 提示）。
     */
    public static void showStatus(Level level, BlockPos pos, BlockState state, BlockEntity be, Player player) {
        if (level.isClientSide()) return;

        // 熔炉体系：剩余时间 = 当前剩余燃烧 + 燃料槽储备
        if (be instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity furnace) {
            boolean lit = FireFuelData.isLit(state);
            int litTime = ((io.github.createmeow.timex_rebirth.mixin.AbstractFurnaceBlockEntityAccessor) furnace)
                    .timex_rebirth$getLitTime();
            ItemStack fuelSlot = furnace.getItem(1);
            // 燃料槽储备秒数（按原版燃烧值计算，不含倍率——熔炉走原版消耗逻辑）
            int reserveTicks = fuelSlot.isEmpty() ? 0
                    : fuelSlot.getCount() * fuelSlot.getItem().getBurnTime(fuelSlot, null);
            if (lit) {
                player.displayClientMessage(Component.translatable("message.timex_rebirth.furnace_remaining",
                        litTime / 20, reserveTicks / 20), true);
            } else if (reserveTicks > 0) {
                player.displayClientMessage(Component.translatable("message.timex_rebirth.fire_ready"), true);
            } else {
                player.displayClientMessage(Component.translatable("message.timex_rebirth.fire_no_fuel"), true);
            }
            return;
        }

        FireFuelData data = FireFuelData.of(be);
        int fuel = data == null ? 0 : data.getFuelTicks();
        boolean lit = FireFuelData.isLit(state);
        if (lit) {
            player.displayClientMessage(Component.translatable("message.timex_rebirth.fire_remaining", fuel / 20), true);
        } else if (fuel > 0) {
            player.displayClientMessage(Component.translatable("message.timex_rebirth.fire_ready"), true);
        } else {
            player.displayClientMessage(Component.translatable("message.timex_rebirth.fire_no_fuel"), true);
        }
    }

    /**
     * 在 {@code pos.relative(face)} 处放置一个<b>正常的火焰</b>（仿原版打火石）。
     * 用于"点燃的绒毛 / 涂蜡燃烧纸板 SHIFT+右键"在地面/任意方块上生火。
     *
     * @return 是否成功放置
     */
    public static boolean placeFire(Level level, BlockPos pos, Direction face) {
        if (level.isClientSide()) return false;
        BlockPos target = pos.relative(face);
        if (!level.getBlockState(target).canBeReplaced()) return false;
        // 必须有可被火点燃的下部方块（BaseFireBlock.getState 内部已校验）
        BlockState fire = BaseFireBlock.getState(level, target);
        if (fire == null) return false;
        level.setBlock(target, fire, Block.UPDATE_ALL);
        level.playSound(null, target, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.8F, 1.0F);
        return true;
    }

    /**
     * 打火石点燃熔炉（授予一次"点火许可"）：
     * 前提是 GUI 燃料槽中已有可燃物；附件 fuelTicks 作为许可标志，
     * FurnaceFuelMixin 的 Redirect 检测到许可后放行原版自动点火逻辑；
     * 燃尽熄灭后需重新打火。
     *
     * @return true = 许可已授予
     */
    public static boolean tryIgniteFurnace(Level level, BlockPos pos, BlockState state, BlockEntity be) {
        if (level.isClientSide()) return false;
        if (!(be instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity furnace)) return false;

        // 燃料槽必须有可燃物才允许点火
        ItemStack fuelSlot = furnace.getItem(1);
        if (fuelSlot.isEmpty() || burnTicksOf(fuelSlot) <= 0) {
            return false;
        }

        FireFuelData data = FireFuelData.of(be);
        if (data == null) return false;
        data.setFuelTicks(1); // 点火许可标志
        be.setChanged();
        level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    /**
     * 每 tick 消耗：点燃状态下扣减燃料，归零自动熄灭（播放熄灭音效）。由篝火/炉灶/厨锅 tick Mixin 调用。
     *
     * @return true 表示本次因燃料耗尽执行了熄灭
     */
    public static boolean consumeTick(Level level, BlockPos pos, BlockState state, BlockEntity be) {
        if (!FireFuelData.isLit(state)) return false;
        FireFuelData data = FireFuelData.of(be);
        if (data == null) return false;
        data.tickDown();
        if (!data.hasFuel()) {
            FireFuelData.setLit(level, pos, state, false);
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.0F);
            }
            return true;
        }
        be.setChanged();
        return false;
    }
}
