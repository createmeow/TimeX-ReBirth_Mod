package com.createmeow.cm_plugins;

import com.mrcrayfish.guns.event.GunFireEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.List;
import java.util.Set;

/**
 * 枪声吸引怪物（MrCrayfish Gun Mod 集成）：玩家开枪后，
 * 将周围 {@link #ATTRACT_RADIUS} 格内的僵尸（含尸壳/溺尸等变种）
 * 与 mutanter 怪物（7 种）的目标设为开火玩家，使其向枪声来源追击。
 * <p>该 handler 仅在 cgm 已加载时注册（见 {@code createmeowsplugins} 构造器），
 * 引用的 GunFireEvent 类不存在时不会加载。</p>
 */
public class GunfireAttractionHandler {

    /** 枪声吸引半径（格） */
    public static final double ATTRACT_RADIUS = 96.0;

    /** mutanter 模组的怪物实体注册名路径（7 种，不含投射物） */
    private static final Set<String> MUTANTER_MOBS = Set.of(
            "mini_slime", "slime", "raider", "devourer", "destroyer", "lurker", "amalgamation");

    /**
     * 枪械开火（cgm {@link GunFireEvent.Post}）：服务端将吸引范围内
     * 僵尸与 mutanter 怪物的攻击目标设为开火玩家。
     */
    @SubscribeEvent
    public static void onGunFire(GunFireEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;
        ServerLevel level = (ServerLevel) player.level();

        AABB box = player.getBoundingBox().inflate(ATTRACT_RADIUS);
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, box,
                e -> e.isAlive() && (e instanceof Zombie || isMutanterMob(e)));
        for (Mob mob : mobs) {
            mob.setTarget(player);
        }
        if (!mobs.isEmpty()) {
            createmeowsplugins.LOGGER.debug("[GunfireAttraction] 玩家 {} 开枪，吸引了 {} 只怪物",
                    player.getName().getString(), mobs.size());
        }
    }

    /** 是否为 mutanter 怪物（按注册表 id 判定，mutanter 缺席时自然为 false）。 */
    public static boolean isMutanterMob(Mob mob) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        return "mutanter".equals(id.getNamespace()) && MUTANTER_MOBS.contains(id.getPath());
    }
}
