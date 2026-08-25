package io.github.createmeow.timex_rebirth.features;

import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 作物温度 BlockEntity 类型注册。
 * 该类型绑定到所有"作物/树苗"方块（CropBlock/SaplingBlock 子类、带 #minecraft:crops / #minecraft:saplings 标签的方块、竹子），
 * 使其可承载 {@link CropTempBlockEntity} 存储种子温度。
 */
public class CropTempRegistry {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TimeX.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CropTempBlockEntity>> CROP_TEMP_TYPE =
            BLOCK_ENTITIES.register("crop_temp", CropTempRegistry::createType);

    private static BlockEntityType<CropTempBlockEntity> createType() {
        BlockEntityType.BlockEntitySupplier<CropTempBlockEntity> supplier = CropTempBlockEntity::new;
        BlockEntityType.Builder<CropTempBlockEntity> builder =
                BlockEntityType.Builder.of(supplier, collectCropBlocks());
        return builder.build(null);
    }

    private static Block[] collectCropBlocks() {
        return BuiltInRegistries.BLOCK.stream()
                .filter(CropTempRegistry::isCropBlock)
                .toArray(Block[]::new);
    }

    private static boolean isCropBlock(Block block) {
        return block instanceof CropBlock
                || block instanceof SaplingBlock
                || block.defaultBlockState().is(BlockTags.CROPS)
                || block.defaultBlockState().is(BlockTags.SAPLINGS)
                || block == Blocks.BAMBOO_SAPLING;
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
