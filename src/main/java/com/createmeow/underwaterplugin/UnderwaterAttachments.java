package com.createmeow.underwaterplugin;

import com.mojang.serialization.Codec;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * underwater_plugin 的数据附件注册。
 * 溺水值：0 ~ drowningValueMax（默认 18），跟随玩家存档序列化。
 */
public final class UnderwaterAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, UnderwaterPlugin.MODID);

    public static final Supplier<AttachmentType<Integer>> DROWNING_VALUE =
            ATTACHMENTS.register("drowning_value", () -> AttachmentType.builder(() -> 0)
                    .serialize(Codec.INT)
                    .build());

    private UnderwaterAttachments() {}
}
