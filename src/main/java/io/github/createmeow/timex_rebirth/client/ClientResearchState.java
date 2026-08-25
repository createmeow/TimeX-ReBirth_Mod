package io.github.createmeow.timex_rebirth.client;

import io.github.createmeow.timex_rebirth.network.ResearchSyncPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 客户端研究数据缓存（由 ResearchSyncPayload 更新），供研究站界面与科技树查看界面使用。
 */
@OnlyIn(Dist.CLIENT)
public class ClientResearchState {
    public record Member(String name, UUID id) {
    }

    private static int points;
    private static final Set<String> unlocked = new HashSet<>();
    /** 玩家全局是否已有进行中的研究会话（含其他研究站） */
    private static boolean globalResearching;
    private static String sessionNode;
    private static String sessionMode;
    private static String researcherName;
    private static final List<String> participantNames = new ArrayList<>();
    private static int sessionProgress;
    private static int sessionDuration;
    private static final List<Member> members = new ArrayList<>();
    /** 当前绑定的研究站坐标（asLong，0 表示附近无研究站），操作包原样带回服务端精确路由 */
    private static long stationPos;

    private ClientResearchState() {
    }

    public static void handlePacket(ResearchSyncPayload packet) {
        points = packet.points();
        unlocked.clear();
        unlocked.addAll(packet.unlocked());
        globalResearching = packet.globalResearching();
        sessionNode = packet.sessionNode();
        sessionMode = packet.sessionMode();
        researcherName = packet.researcherName();
        participantNames.clear();
        participantNames.addAll(packet.participantNames());
        sessionProgress = packet.sessionProgress();
        sessionDuration = packet.sessionDuration();
        stationPos = packet.stationPos();
        members.clear();
        for (String m : packet.members()) {
            String[] parts = m.split("\\|", 2);
            if (parts.length == 2) {
                try {
                    members.add(new Member(parts[0], UUID.fromString(parts[1])));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    public static void openStationScreen() {
        Minecraft.getInstance().setScreen(new ResearchStationScreen());
    }

    public static int getPoints() {
        return points;
    }

    public static boolean isUnlocked(String nodeId) {
        return unlocked.contains(nodeId);
    }

    public static boolean hasActiveSession() {
        return sessionNode != null && !sessionNode.isEmpty();
    }

    /** 玩家是否正在研究（全局，包括其他研究站的会话）。 */
    public static boolean isGlobalResearching() {
        return globalResearching;
    }

    public static String getSessionNode() {
        return sessionNode;
    }

    public static String getSessionMode() {
        return sessionMode;
    }

    public static String getResearcherName() {
        return researcherName;
    }

    public static List<String> getParticipantNames() {
        return participantNames;
    }

    public static int getSessionProgress() {
        return sessionProgress;
    }

    public static int getSessionDuration() {
        return sessionDuration;
    }

    public static List<Member> getMembers() {
        return members;
    }

    /** 当前绑定的研究站坐标（asLong），供操作包精确路由。 */
    public static long getStationPos() {
        return stationPos;
    }
}
