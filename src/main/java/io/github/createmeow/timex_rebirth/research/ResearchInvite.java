package io.github.createmeow.timex_rebirth.research;

import java.util.UUID;

/**
 * 待同意的研究邀请（帮助/协助）：发起者已校验条件并发出邀请，
 * 被邀请者同意后（{@link ResearchStationBlockEntity#agreeInvite}）才扣点数、创建研究会话。
 */
public class ResearchInvite {
    public final UUID inviterId;
    public final String nodeId;
    public final ResearchSession.Mode mode;
    public final long stationPos;

    public ResearchInvite(UUID inviterId, String nodeId, ResearchSession.Mode mode, long stationPos) {
        this.inviterId = inviterId;
        this.nodeId = nodeId;
        this.mode = mode;
        this.stationPos = stationPos;
    }
}
