package io.github.createmeow.timex_rebirth.features.flint;

import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 可疑的积雪：分层积雪（layers 1-8），内含可刷刮的战利品。
 * <p>
 * 继承原版 {@link BrushableBlock}，从而复用 {@link net.minecraft.world.item.BrushItem} 的长按刷刮动画
 * （挥动粒子 + 刷子音效）；但改用自定义 {@link SuspiciousSnowBlockEntity} 控制清刷节奏：
 * 每持续清刷 2 秒提升一级 DUSTED（0→1→2），进度满时 100% 掉落考古战利品并变回普通积雪（保持层数）。
 * <p>
 * 挖掘走方块自身战利品表 {@code loot_table/blocks/suspicious_snow.json}：50% 掉落。
 */
public class SuspiciousSnowBlock extends BrushableBlock {

    public static final IntegerProperty LAYERS = BlockStateProperties.LAYERS;

    private static final TagKey<Item> SCRAP_TAG = TagKey.create(BuiltInRegistries.ITEM.key(), TimeX.rl("scrap"));

    public SuspiciousSnowBlock(BlockBehaviour.Properties properties, SoundEvent brushSound, SoundEvent completedSound) {
        this(Blocks.SNOW, brushSound, completedSound, properties);
    }

    public SuspiciousSnowBlock(Block turnsInto, SoundEvent brushSound, SoundEvent completedSound, BlockBehaviour.Properties properties) {
        super(turnsInto, brushSound, completedSound, properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(BlockStateProperties.DUSTED, 0)
                .setValue(LAYERS, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.DUSTED, LAYERS);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SuspiciousSnowBlockEntity(pos, state);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof BrushableBlockEntity brushable) {
            brushable.checkReset();
        }
        // 积雪层不像沙砾/沙块那样会下落
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShapeForLayers(state.getValue(LAYERS));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShapeForLayers(Math.max(0, state.getValue(LAYERS) - 1));
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(LAYERS) == 8 ? 0.2F : 1.0F;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return !below.isEmpty();
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        ItemStack tool = builder.getOptionalParameter(LootContextParams.TOOL);
        boolean hasSilkTouch = tool != null && tool.get(net.minecraft.core.component.DataComponents.ENCHANTMENTS) != null
                && tool.get(net.minecraft.core.component.DataComponents.ENCHANTMENTS).entrySet().stream()
                        .anyMatch(e -> e.getKey().is(Enchantments.SILK_TOUCH));
        if (hasSilkTouch) {
            // 精准采集：掉落对应层数的雪层 + 随机废品
            List<ItemStack> drops = new ArrayList<>();
            int layers = state.getValue(LAYERS);
            drops.add(new ItemStack(Items.SNOW, layers));
            // 随机废品
            var scrapTag = BuiltInRegistries.ITEM.getTag(SCRAP_TAG);
            if (scrapTag.isPresent()) {
                var items = scrapTag.get().stream().toList();
                if (!items.isEmpty()) {
                    Item scrapItem = items.get(RandomSource.create().nextInt(items.size())).value();
                    drops.add(new ItemStack(scrapItem));
                }
            }
            return drops;
        }
        return super.getDrops(state, builder);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        int layers = 1;
        BlockState above = context.getLevel().getBlockState(context.getClickedPos().above());
        if (above.is(this)) {
            layers = above.getValue(LAYERS) + 1;
            layers = Math.min(layers, 8);
        }
        return this.defaultBlockState()
                .setValue(LAYERS, layers)
                .setValue(BlockStateProperties.DUSTED, 0);
    }

    private static VoxelShape getShapeForLayers(int layers) {
        if (layers < 1) return Shapes.empty();
        if (layers > 8) layers = 8;
        return switch (layers) {
            case 1 -> Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0);
            case 2 -> Block.box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0);
            case 3 -> Block.box(0.0, 0.0, 0.0, 16.0, 6.0, 16.0);
            case 4 -> Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);
            case 5 -> Block.box(0.0, 0.0, 0.0, 16.0, 10.0, 16.0);
            case 6 -> Block.box(0.0, 0.0, 0.0, 16.0, 12.0, 16.0);
            case 7 -> Block.box(0.0, 0.0, 0.0, 16.0, 14.0, 16.0);
            default -> Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
        };
    }
}