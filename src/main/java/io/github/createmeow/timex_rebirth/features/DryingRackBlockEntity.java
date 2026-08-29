package io.github.createmeow.timex_rebirth.features;

import io.github.createmeow.timex_rebirth.advancement.AdvancementTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * 晾晒架方块实体：仿照原版篝火的<b>多槽位放置</b>结构。
 * <ul>
 *   <li>共 <b>4 个槽位</b>，每个槽独立放置一件物品（最多 4 件）；</li>
 *   <li>露天晴天计时，下雨/遮天时暂停（晾晒需要阳光）；</li>
 *   <li>枝条（twig）在槽内晒满 20 秒 → 转成干枝条（dry_twig）并以<b>掉落物</b>形式返还；</li>
 *   <li>空槽位不复位计时。</li>
 * </ul>
 */
public class DryingRackBlockEntity extends BlockEntity {

    /** 槽位数量（仿篝火 4 格）。 */
    public static final int SLOTS = 4;

    /** 4 个槽位的物品（空槽为 ItemStack.EMPTY）。 */
    private final ItemStack[] items = new ItemStack[SLOTS];
    /** 每个槽位的已晾晒 tick（独立计时）。 */
    private final int[] progress = new int[SLOTS];
    /** 每个槽位干燥完成时应返还的产物。 */
    private final ItemStack[] results = new ItemStack[SLOTS];
    /** 每个槽位干燥所需 tick（来自配方 dryingTime）。 */
    private final int[] needTicks = new int[SLOTS];

    public DryingRackBlockEntity(BlockPos pos, BlockState state) {
        super(FireToolRegistry.DRYING_RACK_BE.get(), pos, state);
        for (int i = 0; i < SLOTS; i++) {
            items[i] = ItemStack.EMPTY;
            results[i] = ItemStack.EMPTY;
            needTicks[i] = 1;
        }
    }

    /** 读取某槽位的物品。 */
    public ItemStack getItem(int slot) {
        return items[slot];
    }

    /** 某槽位是否为空。 */
    public boolean isSlotEmpty(int slot) {
        return items[slot].isEmpty();
    }

    /** 已晾晒进度（0~100%），为空槽返回 0。 */
    public float progressOf(int slot) {
        return items[slot].isEmpty() ? 0 : (float) progress[slot] / needTicks[slot];
    }

    /** 找到第一个空槽位索引；全满返回 -1。 */
    public int firstEmptySlot() {
        for (int i = 0; i < SLOTS; i++) {
            if (items[i].isEmpty()) return i;
        }
        return -1;
    }

    /** 是否有任意物品在晾晒。 */
    public boolean hasContent() {
        for (ItemStack s : items) {
            if (!s.isEmpty()) return true;
        }
        return false;
    }

    /** 放入一件物品到首个空槽；无空槽或无可匹配配方则失败。返回实际放置的槽位；-1=失败。 */
    public int setContent(ItemStack stack) {
        if (level == null) return -1;
        DryingRackRecipe recipe = findRecipe(stack);
        if (recipe == null) return -1;
        int slot = firstEmptySlot();
        if (slot < 0) return -1;
        this.items[slot] = stack.copy();
        this.items[slot].setCount(1);
        this.progress[slot] = 0;
        this.results[slot] = recipe.getResult();
        this.needTicks[slot] = recipe.getDryingTime();
        setChanged();
        syncToClient();
        return slot;
    }

    /** 取出某槽位的物品（重置计时）。 */
    public ItemStack takeContent(int slot) {
        ItemStack out = items[slot].copy();
        items[slot] = ItemStack.EMPTY;
        progress[slot] = 0;
        results[slot] = ItemStack.EMPTY;
        needTicks[slot] = 1;
        setChanged();
        syncToClient();
        return out;
    }

    /** 主动把 BE 数据广播给客户端，让渲染器立即读到槽位物品。 */
    private void syncToClient() {
        if (level == null || level.isClientSide()) return;
        BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
    }

    /** 从 RecipeManager 查找某物品的干燥配方（无则返回 null）。 */
    public DryingRackRecipe findRecipe(ItemStack stack) {
        if (level == null) return null;
        Optional<RecipeHolder<DryingRackRecipe>> found = level.getRecipeManager()
                .getRecipeFor(FireToolRegistry.DRYING_RACK_TYPE.get(), new SingleRecipeInput(stack), level);
        return found.map(RecipeHolder::value).orElse(null);
    }

    /** 服务端 ticker：由方块 getTicker 调用。 */
    public static void serverTick(Level level, BlockPos pos, BlockState state, DryingRackBlockEntity be) {
        if (level.isClientSide()) return;

        // 露天晴天才会晾晒；下雨或遮天时暂停
        boolean sky = level.canSeeSky(pos.above());
        boolean raining = level.isRaining() && sky;

        for (int i = 0; i < SLOTS; i++) {
            if (be.items[i].isEmpty()) {
                if (be.progress[i] != 0) {
                    be.progress[i] = 0;
                    be.setChanged();
                }
                continue;
            }
            if (raining) continue;

            be.progress[i]++;
            be.setChanged();
            if (be.progress[i] >= be.needTicks[i]) {
                be.finishDrying(i);
            } else if (be.progress[i] % 40 == 0 && be.progress[i] > 0) {
                // 每 40 tick（2 秒）添加一次白色烟雾粒子（在晾晒物品上方）
                double offsetX = (level.getRandom().nextDouble() - 0.5) * 0.6;
                double offsetY = (level.getRandom().nextDouble() - 0.5) * 0.6;
                double offsetZ = (level.getRandom().nextDouble() - 0.5) * 0.6;
                ((net.minecraft.server.level.ServerLevel) level).sendParticles(ParticleTypes.SMOKE,
                        pos.getX() + 0.5 + offsetX,
                        pos.getY() + 0.8,
                        pos.getZ() + 0.5 + offsetZ,
                        1, 0, 0.1, 0, 0);
            }
        }
    }

    /** 槽位晾晒完成：按放入时解析的配方产物，以掉落物形式返还。 */
    private void finishDrying(int slot) {
        if (level == null) return;
        ItemStack result = results[slot].copy();
        ItemStack input = items[slot];
        // 晒干是"湿→干"转换，须保留输入物品的磨损耐久（湿袜子/破袜子最大耐久相同），
        // 否则低耐久湿袜子晒干后重置为满耐久破袜子，可反复刮绒毛刷物品。
        if (input.isDamageableItem() && result.isDamageableItem()
                && input.getMaxDamage() == result.getMaxDamage()) {
            result.setDamageValue(input.getDamageValue());
        }
        items[slot] = ItemStack.EMPTY;
        progress[slot] = 0;
        results[slot] = ItemStack.EMPTY;
        needTicks[slot] = 1;
        setChanged();
        syncToClient();

        // 触发"它晾干了"成就
        if (level instanceof ServerLevel serverLevel && !serverLevel.isClientSide()) {
            // 查找附近的玩家
            for (net.minecraft.world.entity.player.Player player : serverLevel.players()) {
                if (player.blockPosition().distSqr(worldPosition) < 64 * 64) {
                    AdvancementTriggers.triggerDryingWitness((ServerPlayer) player);
                    // 触发"咋这么臭呢？"隐藏成就（如果是湿破袜子被晾晒）
                    if (input.is(FireToolRegistry.WET_TORN_SOCKS.get())) {
                        AdvancementTriggers.triggerSmellySocks((ServerPlayer) player);
                    }
                }
            }
        }

        net.minecraft.world.Containers.dropItemStack(level,
                worldPosition.getX() + 0.5, worldPosition.getY() + 0.8, worldPosition.getZ() + 0.5,
                result);
        level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS,
                0.7F, 1.1F);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ListTag list = new ListTag();
        for (int i = 0; i < SLOTS; i++) {
            CompoundTag slotTag = new CompoundTag();
            slotTag.put("Item", items[i].saveOptional(provider));
            slotTag.putInt("Progress", progress[i]);
            slotTag.put("Result", results[i].saveOptional(provider));
            slotTag.putInt("Need", needTicks[i]);
            list.add(slotTag);
        }
        tag.put("Slots", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        ListTag list = tag.getList("Slots", Tag.TAG_COMPOUND);
        for (int i = 0; i < SLOTS; i++) {
            if (i < list.size()) {
                CompoundTag slotTag = list.getCompound(i);
                this.items[i] = ItemStack.parseOptional(provider, slotTag.getCompound("Item"));
                this.progress[i] = slotTag.getInt("Progress");
                this.results[i] = ItemStack.parseOptional(provider, slotTag.getCompound("Result"));
                this.needTicks[i] = Math.max(1, slotTag.getInt("Need"));
            } else {
                this.items[i] = ItemStack.EMPTY;
                this.progress[i] = 0;
                this.results[i] = ItemStack.EMPTY;
                this.needTicks[i] = 1;
            }
        }
    }

    // ── 客户端同步（参考 StoneAge DryingRackTileEntity）──
    // 没有这三处，客户端渲染器读到的始终是空槽位，物品不会显示在架子上。

    /** 同步给客户端的完整数据（含全部槽位）。与 Farmer's Delight 一致用 saveWithoutMetadata，
     * 其内部会调用 saveAdditional 写入 "Slots"。 */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }

    /** 每次内容变化：把 BE 数据打包给客户端。 */
    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    /** 收到更新包：默认链路由 NeoForge 调 loadWithComponents → loadAdditional 解析 Slots，
     * 需保证 getUpdateTag 写入的键与 loadAdditional 读取的键一致（此处都在 "Slots"）。
     * 参考 Farmer's Delight AbstractStoveBlockEntity：不覆写 onDataPacket/handleUpdateTag。 */
}
