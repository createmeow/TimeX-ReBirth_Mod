package io.github.createmeow.timex_rebirth.research;

import dev.anye.mc.basecore.basecore.BasecoreServerHelper;
import io.github.createmeow.timex_rebirth.advancement.AdvancementTriggers;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 研究站方块实体：承载并推进研究会话。
 * - 会话进度仅在所有参与者在线时推进（研究者缺席则暂停）。
 * - 会话完成时为参与者解锁对应科技节点（协助=双方，帮助=受益者）。
 * - 通过静态列表注册自身，供网络处理器按玩家位置查找（仿 basecore ServerHelper 模式）。
 * - 规则：每个玩家全局同时只能进行 1 个研究会话（跨站判定，防止摆放多站并行研究）。
 */
public class ResearchStationBlockEntity extends BlockEntity {
    /** 当前加载的研究站（服务端） */
    public static final List<ResearchStationBlockEntity> STATIONS = new ArrayList<>();
    /** 待同意的帮助/协助邀请（被邀请者 UUID → 邀请） */
    public static final Map<UUID, ResearchInvite> PENDING_INVITES = new HashMap<>();
    /** 玩家查找研究站的最大距离（方块，平方距离 64 = 8 格） */
    public static final double FIND_RANGE_SQ = 64.0;

    private ResearchSession session;

    public ResearchStationBlockEntity(BlockPos pos, BlockState state) {
        super(ResearchRegistry.RESEARCH_STATION_BE.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            STATIONS.add(this);
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) {
            STATIONS.remove(this);
        }
        super.setRemoved();
    }

    public ResearchSession getSession() {
        return session;
    }

    public void tick() {
        if (level == null || level.isClientSide || session == null) return;
        // 所有参与者在线才推进研究
        for (UUID uuid : session.participants) {
            if (getServerPlayer(uuid) == null) return;
        }
        session.progress++;
        // 每 1 秒标脏一次：平衡"进度尽快落盘"与"避免每 tick 持续标记区块脏导致频繁落盘"
        if (session.progress % 20 == 0) {
            setChanged();
        }
        if (session.progress % 100 == 0) {
            // 每 5 秒：向参与者同步进度（刷新打开的研究站界面）并备份快照到全部参与者存档
            // （此前只备份研究者，协助伙伴的快照进度停留在 0，站被拆后从伙伴快照恢复会丢全部进度）
            syncAll();
            CompoundTag sessionTag = session.save();
            for (UUID uuid : session.participants) {
                ServerPlayer p = getServerPlayer(uuid);
                if (p != null) {
                    ResearchData.saveActiveSession(p, sessionTag, worldPosition.asLong());
                }
            }
        }
        if (session.isDone()) {
            complete();
        }
    }

    private void complete() {
        ResearchSession done = this.session;
        this.session = null;
        if (done == null) return;
        TechNode node = TechTree.getNode(done.nodeId);
        for (UUID uuid : done.participants) {
            ServerPlayer p = getServerPlayer(uuid);
            if (p == null) continue;
            // 先清快照再解锁：unlock 触发的同步才能携带"已清空"的会话状态，
            // 否则客户端残留 globalResearching=true + 满进度条（旧顺序先 unlock 后清快照且不再同步）
            ResearchData.clearActiveSession(p);
            if (node != null) {
                ResearchData.unlock(p, node.id());
                // 触发"我学会了！"成就
                AdvancementTriggers.triggerResearcher(p);
                p.sendSystemMessage(Component.translatable("research.timex_rebirth.unlocked",
                        Component.translatable(node.getNameTranslationKey())));
            } else {
                // 节点定义已失效：只释放会话，不解锁，主动同步一次避免客户端残留
                ResearchNetwork.syncPlayer(p);
            }
        }
        setChanged();
    }

    /** 调试：立即完成当前研究（无论进度如何），走与正常完成相同的解锁流程。 */
    public void forceComplete() {
        if (session == null || level == null || level.isClientSide) return;
        session.progress = session.duration;
        complete();
    }

    /**
     * 从研究者玩家持久数据中的会话快照恢复进度（研究站区块数据丢失时的兜底）。
     * 仅当本站当前无会话且快照记录的研究站坐标与本站一致时生效。
     */
    public void restoreFromSnapshot(ServerPlayer player) {
        if (session != null || level == null) return;
        CompoundTag snap = ResearchData.getActiveSession(player);
        if (snap == null) return;
        if (snap.getLong("station") != worldPosition.asLong()) return;
        ResearchSession restored = new ResearchSession();
        restored.load(snap.getCompound("session"));
        if (restored.nodeId == null || restored.nodeId.isEmpty()) {
            ResearchData.clearActiveSession(player);
            return;
        }
        TechNode node = TechTree.getNode(restored.nodeId);
        if (node == null) {
            // 节点定义已变更，旧快照失效
            ResearchData.clearActiveSession(player);
            return;
        }
        // 校验所有在线参与者仍持有指向本站的活动会话快照：
        // 若任一参与者已放弃（快照被清除/被重置），说明本会话已终止，不应恢复，直接清除本玩家的无效快照
        for (UUID uuid : restored.participants) {
            ServerPlayer p = getServerPlayer(uuid);
            if (p == null) continue;
            CompoundTag ps = ResearchData.getActiveSession(p);
            if (ps == null || ps.getLong("station") != worldPosition.asLong()) {
                ResearchData.clearActiveSession(player);
                return;
            }
        }
        this.session = restored;
        setChanged();
        syncAll();
        player.sendSystemMessage(Component.translatable("research.timex_rebirth.resumed",
                Component.translatable(node.getNameTranslationKey())));
    }

    // ── 研究操作（由网络处理器调用）──

    /** 单独研究：研究者消耗全部点数，时长 100%。 */
    public boolean startSolo(ServerPlayer player, String nodeId) {
        TechNode node = TechTree.getNode(nodeId);
        if (node == null) return false;
        if (session != null) {
            player.sendSystemMessage(Component.translatable("research.timex_rebirth.busy"));
            return false;
        }
        if (hasActiveResearch(player)) {
            player.sendSystemMessage(Component.translatable("research.timex_rebirth.already_researching"));
            return false;
        }
        if (ResearchData.isUnlocked(player, nodeId)) {
            player.sendSystemMessage(Component.translatable("research.timex_rebirth.already"));
            return false;
        }
        if (!TechTree.hasDependencies(player, node)) {
            player.sendSystemMessage(Component.translatable("research.timex_rebirth.no_deps"));
            return false;
        }
        if (!ResearchData.spendPoints(player, node.cost())) {
            player.sendSystemMessage(Component.translatable("research.timex_rebirth.no_points"));
            return false;
        }
        this.session = new ResearchSession(nodeId, ResearchSession.Mode.SOLO, player.getUUID(),
                List.of(player.getUUID()), node.durationTicks());
        ResearchData.saveActiveSession(player, session.save(), worldPosition.asLong());
        setChanged();
        syncAll();
        player.sendSystemMessage(Component.translatable("research.timex_rebirth.started",
                Component.translatable(node.getNameTranslationKey())));
        return true;
    }

    /**
     * 帮助研究：向受益者发送帮助邀请（不扣点数、不建会话），
     * 对方点击同意（{@link #agreeInvite}）后才真正开始研究，时长 75%。
     */
    public boolean startHelp(ServerPlayer helper, String nodeId, UUID researcherId) {
        TechNode node = TechTree.getNode(nodeId);
        if (node == null || researcherId == null) return false;
        if (session != null) {
            helper.sendSystemMessage(Component.translatable("research.timex_rebirth.busy"));
            return false;
        }
        ServerPlayer researcher = getServerPlayer(researcherId);
        if (researcher == null) {
            helper.sendSystemMessage(Component.translatable("research.timex_rebirth.partner_offline"));
            return false;
        }
        if (helper.getUUID().equals(researcherId)) return false;
        if (!ResearchData.isUnlocked(helper, nodeId)) {
            helper.sendSystemMessage(Component.translatable("research.timex_rebirth.help_need_knowledge"));
            return false;
        }
        if (ResearchData.isUnlocked(researcher, nodeId)) {
            // 对方（受益者）已掌握：提示"B 已掌握该技术"，而非"你已掌握该技术"
            helper.sendSystemMessage(Component.translatable("research.timex_rebirth.already_by", researcher.getDisplayName()));
            return false;
        }
        if (!sameBase(helper, researcher)) {
            helper.sendSystemMessage(Component.translatable("research.timex_rebirth.not_same_base"));
            return false;
        }
        if (!TechTree.hasDependencies(researcher, node)) {
            helper.sendSystemMessage(Component.translatable("research.timex_rebirth.no_deps"));
            return false;
        }
        if (!ResearchData.hasEnoughPoints(helper, node.cost())) {
            helper.sendSystemMessage(Component.translatable("research.timex_rebirth.no_points"));
            return false;
        }
        PENDING_INVITES.put(researcherId, new ResearchInvite(helper.getUUID(), nodeId,
                ResearchSession.Mode.HELP, worldPosition.asLong()));
        helper.sendSystemMessage(Component.translatable("research.timex_rebirth.help_invite_sent", researcher.getDisplayName()));
        researcher.sendSystemMessage(buildInviteMessage(
                "research.timex_rebirth.invite_received_help", helper.getDisplayName(), nodeId));
        return true;
    }

    /**
     * 协助研究：向伙伴发送协助邀请（不扣点数、不建会话），
     * 对方点击同意后才真正开始研究，双方各消耗一半点数、双方解锁，时长 50%（最多 2 人）。
     */
    public boolean startCollab(ServerPlayer inviter, String nodeId, UUID partnerId) {
        TechNode node = TechTree.getNode(nodeId);
        if (node == null || partnerId == null) return false;
        if (session != null) {
            inviter.sendSystemMessage(Component.translatable("research.timex_rebirth.busy"));
            return false;
        }
        if (hasActiveResearch(inviter)) {
            inviter.sendSystemMessage(Component.translatable("research.timex_rebirth.already_researching"));
            return false;
        }
        ServerPlayer partner = getServerPlayer(partnerId);
        if (partner == null) {
            inviter.sendSystemMessage(Component.translatable("research.timex_rebirth.partner_offline"));
            return false;
        }
        if (inviter.getUUID().equals(partnerId)) return false;
        if (ResearchData.isUnlocked(inviter, nodeId)) {
            inviter.sendSystemMessage(Component.translatable("research.timex_rebirth.already"));
            return false;
        }
        if (ResearchData.isUnlocked(partner, nodeId)) {
            // 对方已掌握：提示"B 已掌握该技术"
            inviter.sendSystemMessage(Component.translatable("research.timex_rebirth.already_by", partner.getDisplayName()));
            return false;
        }
        if (!sameBase(inviter, partner)) {
            inviter.sendSystemMessage(Component.translatable("research.timex_rebirth.not_same_base"));
            return false;
        }
        if (!TechTree.hasDependencies(inviter, node) || !TechTree.hasDependencies(partner, node)) {
            inviter.sendSystemMessage(Component.translatable("research.timex_rebirth.no_deps"));
            return false;
        }
        int half = node.cost() / 2;
        if (!ResearchData.hasEnoughPoints(inviter, half) || !ResearchData.hasEnoughPoints(partner, half)) {
            inviter.sendSystemMessage(Component.translatable("research.timex_rebirth.no_points"));
            return false;
        }
        PENDING_INVITES.put(partnerId, new ResearchInvite(inviter.getUUID(), nodeId,
                ResearchSession.Mode.COLLAB, worldPosition.asLong()));
        inviter.sendSystemMessage(Component.translatable("research.timex_rebirth.collab_invite_sent", partner.getDisplayName()));
        partner.sendSystemMessage(buildInviteMessage(
                "research.timex_rebirth.invite_received_collab", inviter.getDisplayName(), nodeId));
        return true;
    }

    /**
     * 同意当前待处理的帮助/协助邀请（点击聊天邀请消息的 [同意] 按钮触发）。
     * 同意时才扣除点数并创建研究会话；条件已不满足或研究站被占用则邀请失效。
     */
    public static void agreeInvite(ServerPlayer invitee) {
        ResearchInvite invite = PENDING_INVITES.remove(invitee.getUUID());
        if (invite == null) {
            invitee.sendSystemMessage(Component.translatable("research.timex_rebirth.invite_none"));
            return;
        }
        ResearchStationBlockEntity station = findStationByPos(invitee, invite.stationPos);
        if (station == null || station.session != null) {
            invitee.sendSystemMessage(Component.translatable("research.timex_rebirth.invite_expired"));
            return;
        }
        ServerPlayer inviter = invitee.server.getPlayerList().getPlayer(invite.inviterId);
        if (inviter == null) {
            invitee.sendSystemMessage(Component.translatable("research.timex_rebirth.invite_expired"));
            return;
        }
        TechNode node = TechTree.getNode(invite.nodeId);
        if (node == null) {
            invitee.sendSystemMessage(Component.translatable("research.timex_rebirth.invite_expired"));
            return;
        }
        if (invite.mode == ResearchSession.Mode.HELP) {
            applyHelp(station, inviter, invitee, node);
        } else {
            applyCollab(station, inviter, invitee, node);
        }
    }

    /** 同意帮助邀请后真正开始：受益者免费，帮助者（已解锁）消耗全部点数，时长 75%。 */
    private static void applyHelp(ResearchStationBlockEntity station, ServerPlayer helper,
                                  ServerPlayer researcher, TechNode node) {
        String nodeId = node.id();
        if (!ResearchData.isUnlocked(helper, nodeId) || ResearchData.isUnlocked(researcher, nodeId)
                || !station.sameBase(helper, researcher) || !TechTree.hasDependencies(researcher, node)) {
            researcher.sendSystemMessage(Component.translatable("research.timex_rebirth.invite_expired"));
            return;
        }
        if (hasActiveResearch(researcher)) {
            researcher.sendSystemMessage(Component.translatable("research.timex_rebirth.already_researching"));
            helper.sendSystemMessage(Component.translatable("research.timex_rebirth.invite_expired"));
            return;
        }
        if (!ResearchData.spendPoints(helper, node.cost())) {
            helper.sendSystemMessage(Component.translatable("research.timex_rebirth.no_points"));
            researcher.sendSystemMessage(Component.translatable("research.timex_rebirth.invite_cancelled"));
            return;
        }
        station.session = new ResearchSession(nodeId, ResearchSession.Mode.HELP, researcher.getUUID(),
                List.of(researcher.getUUID()), (int) (node.durationTicks() * 0.75));
        ResearchData.saveActiveSession(researcher, station.session.save(), station.worldPosition.asLong());
        station.setChanged();
        station.syncAll();
        helper.sendSystemMessage(Component.translatable("research.timex_rebirth.helped",
                Component.translatable(node.getNameTranslationKey())));
        researcher.sendSystemMessage(Component.translatable("research.timex_rebirth.helped_to",
                helper.getDisplayName(), Component.translatable(node.getNameTranslationKey())));
    }

    /** 同意协助邀请后真正开始：双方各消耗一半点数、双方解锁，时长 50%。 */
    private static void applyCollab(ResearchStationBlockEntity station, ServerPlayer inviter,
                                    ServerPlayer partner, TechNode node) {
        String nodeId = node.id();
        if (ResearchData.isUnlocked(inviter, nodeId) || ResearchData.isUnlocked(partner, nodeId)
                || !station.sameBase(inviter, partner)
                || !TechTree.hasDependencies(inviter, node) || !TechTree.hasDependencies(partner, node)) {
            partner.sendSystemMessage(Component.translatable("research.timex_rebirth.invite_expired"));
            return;
        }
        if (hasActiveResearch(inviter)) {
            inviter.sendSystemMessage(Component.translatable("research.timex_rebirth.already_researching"));
            partner.sendSystemMessage(Component.translatable("research.timex_rebirth.invite_expired"));
            return;
        }
        if (hasActiveResearch(partner)) {
            partner.sendSystemMessage(Component.translatable("research.timex_rebirth.already_researching"));
            inviter.sendSystemMessage(Component.translatable("research.timex_rebirth.invite_expired"));
            return;
        }
        int half = node.cost() / 2;
        if (!ResearchData.spendPoints(inviter, half)) {
            inviter.sendSystemMessage(Component.translatable("research.timex_rebirth.no_points"));
            partner.sendSystemMessage(Component.translatable("research.timex_rebirth.invite_cancelled"));
            return;
        }
        if (!ResearchData.spendPoints(partner, half)) {
            ResearchData.addPoints(inviter, half);
            inviter.sendSystemMessage(Component.translatable("research.timex_rebirth.no_points"));
            partner.sendSystemMessage(Component.translatable("research.timex_rebirth.invite_cancelled"));
            return;
        }
        station.session = new ResearchSession(nodeId, ResearchSession.Mode.COLLAB, inviter.getUUID(),
                List.of(inviter.getUUID(), partner.getUUID()), (int) (node.durationTicks() * 0.5));
        // 双方都写快照：全局"同时只能研究 1 项"判定需要覆盖协助伙伴（其存档中必须有活动会话记录）
        ResearchData.saveActiveSession(inviter, station.session.save(), station.worldPosition.asLong());
        ResearchData.saveActiveSession(partner, station.session.save(), station.worldPosition.asLong());
        station.setChanged();
        station.syncAll();
        inviter.sendSystemMessage(Component.translatable("research.timex_rebirth.collab_started",
                Component.translatable(node.getNameTranslationKey())));
        partner.sendSystemMessage(Component.translatable("research.timex_rebirth.collab_invited",
                inviter.getDisplayName(), Component.translatable(node.getNameTranslationKey())));
    }

    /**
     * 该玩家是否已有进行中的研究会话（全局跨站判定）。
     * 判定来源双保险：
     * - 玩家存档中的会话快照（单独研究/帮助的受益者/协助的发起者必有快照；协助伙伴也已补写快照）
     * - 已加载研究站上涉及该玩家的会话（兜底，覆盖快照尚未写出的瞬间）
     */
    public static boolean hasActiveResearch(ServerPlayer player) {
        if (ResearchData.hasActiveSession(player)) return true;
        UUID id = player.getUUID();
        for (ResearchStationBlockEntity be : STATIONS) {
            ResearchSession s = be.session;
            if (s != null && s.involves(id)) return true;
        }
        return false;
    }

    /**
     * 放弃当前研究：清除玩家会话快照，并终止所有已加载研究站上涉及该玩家的会话。
     * 不返还已消耗的研究点数；用于研究站被拆/会话停滞等情况下主动解除"同时只能研究 1 项"的限制。
     */
    public static void abandonSession(ServerPlayer player) {
        boolean hadAny = ResearchData.hasActiveSession(player);
        terminateSessions(player);
        ResearchData.clearActiveSession(player);
        ResearchNetwork.syncPlayer(player);
        if (!hadAny) {
            player.sendSystemMessage(Component.translatable("research.timex_rebirth.no_session"));
            return;
        }
        player.sendSystemMessage(Component.translatable("research.timex_rebirth.abandoned"));
    }

    /**
     * 终止所有已加载研究站上涉及该玩家的会话（不清玩家快照），并通知其他参与者。
     * 供放弃研究/调试重置共用。
     */
    static void terminateSessions(ServerPlayer player) {
        UUID id = player.getUUID();
        for (ResearchStationBlockEntity be : STATIONS) {
            ResearchSession s = be.session;
            if (s == null || !s.involves(id)) continue;
            for (UUID uuid : s.participants) {
                if (uuid.equals(id)) continue;
                ServerPlayer p = be.level != null && be.level.getServer() != null
                        ? be.level.getServer().getPlayerList().getPlayer(uuid) : null;
                if (p != null) {
                    p.sendSystemMessage(Component.translatable("research.timex_rebirth.abandoned_by", player.getDisplayName()));
                    // 一并清除其他参与者的会话快照：否则其存档残留快照会令 hasActiveResearch 恒真
                    // （无法开始新研究），且重开研究站时 restoreFromSnapshot 会恢复出已废弃的会话
                    ResearchData.clearActiveSession(p);
                    ResearchNetwork.syncPlayer(p);
                }
            }
            be.session = null;
            be.setChanged();
        }
    }

    /** 邀请聊天消息（含可点击的 [同意] 按钮）。 */
    private static Component buildInviteMessage(String key, Component who, String nodeId) {
        TechNode node = TechTree.getNode(nodeId);
        Component nodeName = node != null
                ? Component.translatable(node.getNameTranslationKey()) : Component.literal(nodeId);
        return Component.translatable(key, who, nodeName)
                .append(Component.literal(" "))
                .append(Component.translatable("research.timex_rebirth.invite_agree_hint")
                        .withStyle(style -> style.withColor(ChatFormatting.GREEN)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/timexresearch agree"))));
    }

    /** 按坐标查找指定研究站（邀请同意时使用，跨方块实体直接引用查找）。 */
    static ResearchStationBlockEntity findStationByPos(ServerPlayer player, long pos) {
        for (ResearchStationBlockEntity be : STATIONS) {
            if (be.level == player.level() && be.worldPosition.asLong() == pos) {
                return be;
            }
        }
        return null;
    }

    /**
     * 玩家是否可使用本站：位于基地核心范围内时按基地权限判定，独立研究站全员放行。
     * 用于右键打开研究站界面的权限校验（此前 openStation 无权限校验，任意玩家可打开基地内研究站界面）。
     */
    public boolean canUse(ServerPlayer player) {
        return level != null && hasStationPermission(level, worldPosition, player.getUUID());
    }

    /**
     * 登录时校验玩家存档中的会话快照（清理失效快照，防止玩家被永久锁在"研究中"状态）：
     * - 快照指向的研究站未加载/已被拆 → 保留快照（待重放后由 restoreFromSnapshot 恢复）
     * - 研究站已加载但当前无会话 → 尝试恢复（restoreFromSnapshot 内部校验参与者快照，无效则清除）
     * - 研究站上正在进行的是他人的会话 → 本玩家快照失效，直接清除
     */
    public static void cleanupStaleSnapshot(ServerPlayer player) {
        CompoundTag snap = ResearchData.getActiveSession(player);
        if (snap == null) return;
        ResearchStationBlockEntity station = findStationByPos(player, snap.getLong("station"));
        if (station == null) return;
        ResearchSession s = station.getSession();
        if (s == null) {
            station.restoreFromSnapshot(player);
        } else if (!s.involves(player.getUUID())) {
            ResearchData.clearActiveSession(player);
        }
    }

    // ── 辅助 ──

    private boolean sameBase(ServerPlayer a, ServerPlayer b) {
        // 协助/帮助仅限基地核心范围内的研究站
        if (level == null || !hasBasecoreAt(level, worldPosition)) return false;
        Vec3 pos = Vec3.atCenterOf(worldPosition);
        return BasecoreServerHelper.hasPermission(level, pos, a.getUUID())
                && BasecoreServerHelper.hasPermission(level, pos, b.getUUID());
    }

    /** 该位置是否存在基地核心（协助/帮助仅限基地内研究站）。 */
    private static boolean hasBasecoreAt(net.minecraft.world.level.Level level, BlockPos pos) {
        return level != null && BasecoreServerHelper.getBasecore(level, Vec3.atCenterOf(pos)) != null;
    }

    /**
     * 研究站使用权限判定：独立研究站（附近无基地核心）全员放行（仅限单独研究）；
     * 位于基地核心范围内时按基地权限判定。
     */
    private static boolean hasStationPermission(net.minecraft.world.level.Level level, BlockPos pos, UUID uuid) {
        if (!hasBasecoreAt(level, pos)) {
            return true;
        }
        return BasecoreServerHelper.hasPermission(level, Vec3.atCenterOf(pos), uuid);
    }

    private ServerPlayer getServerPlayer(UUID uuid) {
        if (level == null || level.getServer() == null) return null;
        return level.getServer().getPlayerList().getPlayer(uuid);
    }

    /** 该玩家是否被此站会话涉及（用于向其同步会话进度）。 */
    public boolean isInvolved(UUID uuid) {
        return session != null && session.involves(uuid);
    }

    /** 与查看者同基地的在线成员（用于帮助/协助目标选择；独立研究站无基地成员）。 */
    public List<ServerPlayer> getBaseMembers(ServerPlayer viewer) {
        List<ServerPlayer> list = new ArrayList<>();
        if (level == null || level.getServer() == null || !hasBasecoreAt(level, worldPosition)) return list;
        Vec3 pos = Vec3.atCenterOf(worldPosition);
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            if (p != viewer && BasecoreServerHelper.hasPermission(level, pos, p.getUUID())) {
                list.add(p);
            }
        }
        return list;
    }

    /** 查找玩家附近（8 格内、同基地有权限）的研究站。 */
    public static ResearchStationBlockEntity findStationFor(ServerPlayer player) {
        ResearchStationBlockEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (ResearchStationBlockEntity be : STATIONS) {
            if (be.level != player.level()) continue;
            double dist = be.worldPosition.distSqr(player.blockPosition());
            if (dist <= FIND_RANGE_SQ && dist < best
                    && hasStationPermission(be.level, be.worldPosition, player.getUUID())) {
                nearest = be;
                best = dist;
            }
        }
        return nearest;
    }

    public void syncAll() {
        if (level == null || level.getServer() == null) return;
        if (session == null) return;
        for (UUID uuid : session.participants) {
            ServerPlayer p = getServerPlayer(uuid);
            if (p != null) {
                ResearchNetwork.syncPlayer(p);
            }
        }
    }

    // ── 数据持久化 ──

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        if (session != null) {
            tag.put("session", session.save());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains("session")) {
            session = new ResearchSession();
            session.load(tag.getCompound("session"));
        } else {
            session = null;
        }
    }
}
