package com.createmeow.currency_plugin.init;

import com.mojang.serialization.Codec;
import com.createmeow.currency_plugin.CurrencyPlugin;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * 玩家货币余额的数据附件（NeoForge AttachmentType）。
 * <p>两种货币各自独立存储余额，<b>禁止任何形式的互相转换</b>：
 * 腐空朽余额存腐空朽，归霜升余额存归霜升。</p>
 */
public class CurrencyAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, CurrencyPlugin.MODID);

    /** 腐空朽余额（枚数）。 */
    public static final Supplier<AttachmentType<Long>> COMMON_VALUE =
            ATTACHMENT_TYPES.register("common_value", () ->
                    AttachmentType.builder(() -> 0L)
                            .serialize(Codec.LONG)
                            .copyOnDeath()
                            .build());

    /** 归霜升余额（枚数）。 */
    public static final Supplier<AttachmentType<Long>> RARE_VALUE =
            ATTACHMENT_TYPES.register("rare_value", () ->
                    AttachmentType.builder(() -> 0L)
                            .serialize(Codec.LONG)
                            .copyOnDeath()
                            .build());
}
