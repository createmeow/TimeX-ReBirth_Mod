package com.createmeow.currency_plugin.init;

import com.createmeow.currency_plugin.CurrencyPlugin;
import com.createmeow.currency_plugin.machine.RefinerBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 方块实体类型注册：提炼机。
 */
public class CurrencyBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CurrencyPlugin.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RefinerBlockEntity>> REFINING_MACHINE =
            BLOCK_ENTITY_TYPES.register("refining_machine",
                    () -> BlockEntityType.Builder.of(RefinerBlockEntity::new,
                            CurrencyBlocks.REFINING_MACHINE.get()).build(null));
}
