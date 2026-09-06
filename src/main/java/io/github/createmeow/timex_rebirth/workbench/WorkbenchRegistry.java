package io.github.createmeow.timex_rebirth.workbench;

import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 工作台组装系统注册：
 * - 工作剪/工作锤/工作锯：3种工具物品，用于在工作台上安装
 * - 工作台(半成品)：木板被工具加工后的中间态方块，安装全部3种工具后变为原版工作台
 * - 手动物品组装配方：数据驱动，供 JEI/EMI 展示组装流程
 */
public final class WorkbenchRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TimeX.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TimeX.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TimeX.MODID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, TimeX.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, TimeX.MODID);

    // ── 工具物品 ──
    public static final DeferredItem<Item> WORK_SHEARS =
            ITEMS.register("work_shears", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> WORK_HAMMER =
            ITEMS.register("work_hammer", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> WORK_SAW =
            ITEMS.register("work_saw", () -> new Item(new Item.Properties()));

    // ── 工作台(半成品)方块 ──
    public static final DeferredBlock<Block> WORKBENCH_HALF =
            BLOCKS.register("workbench_half", () -> new WorkbenchHalfBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .strength(2.0F, 3.0F)
                            .sound(SoundType.WOOD)));
    public static final DeferredItem<WorkbenchHalfItem> WORKBENCH_HALF_ITEM =
            ITEMS.register("workbench_half", () -> new WorkbenchHalfItem(
                    WORKBENCH_HALF.get(), new Item.Properties()));

    // ── 方块实体 ──
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WorkbenchHalfBlockEntity>> WORKBENCH_HALF_BE =
            BLOCK_ENTITIES.register("workbench_half",
                    () -> BlockEntityType.Builder.of(WorkbenchHalfBlockEntity::new, WORKBENCH_HALF.get()).build(null));

    // ── 手动物品组装配方 ──
    public static final DeferredHolder<RecipeType<?>, RecipeType<ManualAssemblyRecipe>> MANUAL_ASSEMBLY_TYPE =
            RECIPE_TYPES.register("manual_assembly", () -> RecipeType.simple(TimeX.rl("manual_assembly")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ManualAssemblyRecipe>> MANUAL_ASSEMBLY_SERIALIZER =
            RECIPE_SERIALIZERS.register("manual_assembly", ManualAssemblyRecipe.Serializer::new);

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        RECIPE_TYPES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
    }
}
