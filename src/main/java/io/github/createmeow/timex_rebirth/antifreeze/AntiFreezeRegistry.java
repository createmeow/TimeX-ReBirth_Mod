package io.github.createmeow.timex_rebirth.antifreeze;

import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 防冻系统注册：
 * - 抗冻土壤（anti_freeze_dirt）：受防冻剂处理过的泥土，低温下不会冻结，可被锄头耕成抗冻耕地
 * - 抗冻耕地（anti_freeze_farmland）：受防冻剂处理过的耕地，低温下不会冻结，作物可正常种植
 * - 防冻剂（anti_freeze）：右键土壤/耕地将其转换为抗冻版本，每次消耗 1 点耐久
 * 抗冻方块天然不在 FrozenSoilHandler 的冻土转换目标列表中，无需额外豁免。
 */
public class AntiFreezeRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TimeX.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TimeX.MODID);

    public static final DeferredBlock<AntiFreezeDirtBlock> ANTI_FREEZE_DIRT =
            BLOCKS.register("anti_freeze_dirt", () -> new AntiFreezeDirtBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.DIRT)
                            .strength(0.5F)
                            .sound(SoundType.GRAVEL)));
    public static final DeferredBlock<AntiFreezeFarmlandBlock> ANTI_FREEZE_FARMLAND =
            BLOCKS.register("anti_freeze_farmland", () -> new AntiFreezeFarmlandBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.DIRT)
                            .strength(0.6F)
                            .sound(SoundType.GRAVEL)
                            .randomTicks()
                            .pushReaction(PushReaction.DESTROY)));

    public static final DeferredItem<BlockItem> ANTI_FREEZE_DIRT_ITEM =
            ITEMS.register("anti_freeze_dirt", () -> new BlockItem(ANTI_FREEZE_DIRT.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> ANTI_FREEZE_FARMLAND_ITEM =
            ITEMS.register("anti_freeze_farmland", () -> new BlockItem(ANTI_FREEZE_FARMLAND.get(), new Item.Properties()));
    /** 防冻剂：最大 16 次使用（每次右键消耗 1 点耐久）。 */
    public static final DeferredItem<AntiFreezeItem> ANTI_FREEZE =
            ITEMS.register("anti_freeze", () -> new AntiFreezeItem(new Item.Properties().durability(16).rarity(Rarity.UNCOMMON)));
    /** 抗冻凝胶：低温凝胶材料，与铁板合成防冻剂（配方 anti_freeze.json）。 */
    public static final DeferredItem<Item> ANTI_FREEZE_GEL =
            ITEMS.register("anti_freeze_gel", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    // 自定义成就触发器：防冻剂成功转换土壤时触发（区分 dirt/farmland/permafrost）
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, TimeX.MODID);
    public static final DeferredHolder<CriterionTrigger<?>, AntiFreezeUsedTrigger> ANTI_FREEZE_USED =
            TRIGGERS.register("anti_freeze_used", AntiFreezeUsedTrigger::new);

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        TRIGGERS.register(modEventBus);
    }
}
