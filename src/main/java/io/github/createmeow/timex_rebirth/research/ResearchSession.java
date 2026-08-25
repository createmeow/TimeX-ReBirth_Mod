package io.github.createmeow.timex_rebirth.research;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 一次研究会话（存储于研究站方块实体）。
 * 模式：
 *  - SOLO   单独研究：研究者独自进行，消耗全部点数，时长 100%
 *  - HELP   单方面帮助：帮助者（已解锁该节点）消耗全部点数，受益者免费，时长 75%
 *  - COLLAB 协助研究：双方各消耗一半点数，双方都解锁，时长 50%（最多 2 人）
 */
public class ResearchSession {
    public enum Mode { SOLO, HELP, COLLAB }

    public String nodeId;
    public Mode mode = Mode.SOLO;
    /** 受益研究者（解锁对象）；COLLAB 时参与者全部解锁 */
    public UUID researcher;
    /** 参与者：SOLO=[研究者]；HELP=[研究者]；COLLAB=[发起者, 协助者] */
    public List<UUID> participants = new ArrayList<>();
    public int progress;
    public int duration;

    public ResearchSession() {
    }

    public ResearchSession(String nodeId, Mode mode, UUID researcher, List<UUID> participants, int duration) {
        this.nodeId = nodeId;
        this.mode = mode;
        this.researcher = researcher;
        this.participants = participants;
        this.duration = duration;
    }

    public boolean isDone() {
        return progress >= duration;
    }

    public float progressPercent() {
        return duration <= 0 ? 0 : Math.min(1.0F, (float) progress / duration);
    }

    public boolean involves(UUID uuid) {
        return participants.contains(uuid);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("node", nodeId == null ? "" : nodeId);
        tag.putString("mode", mode.name());
        tag.putUUID("researcher", researcher);
        tag.putInt("progress", progress);
        tag.putInt("duration", duration);
        ListTag list = new ListTag();
        for (UUID uuid : participants) {
            list.add(StringTag.valueOf(uuid.toString()));
        }
        tag.put("participants", list);
        return tag;
    }

    public void load(CompoundTag tag) {
        this.nodeId = tag.getString("node");
        try {
            this.mode = Mode.valueOf(tag.getString("mode"));
        } catch (IllegalArgumentException e) {
            // 存档数据损坏/旧版本未知模式时回退为单独研究，避免加载时抛异常
            this.mode = Mode.SOLO;
        }
        this.researcher = tag.hasUUID("researcher") ? tag.getUUID("researcher") : new UUID(0, 0);
        this.progress = tag.getInt("progress");
        this.duration = tag.getInt("duration");
        this.participants.clear();
        ListTag list = tag.getList("participants", Tag.TAG_STRING);
        for (Tag t : list) {
            participants.add(UUID.fromString(t.getAsString()));
        }
    }
}
