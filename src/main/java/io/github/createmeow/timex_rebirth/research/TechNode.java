package io.github.createmeow.timex_rebirth.research;

import java.util.List;

/**
 * 科技树节点定义（研究项目，细分主题）。
 * @param id            唯一 id（如 "heat_receiver"）
 * @param category      分类分支（如 survival / farming / heat），用于界面分组显示
 * @param cost          所需研究点数
 * @param durationSeconds 基础研究时长（秒）；单独研究 100%、帮助 75%、协助 50%
 * @param dependencies  前置节点 id 列表（全部满足才能研究）
 * @param unlockRecipes 解锁的配方 id（未解锁时工作台无法合成并提示）
 * @param usePermissions 解锁的使用权限键（如 use.enchanting_table），事件层拦截
 */
public record TechNode(
        String id,
        String category,
        int cost,
        int durationSeconds,
        List<String> dependencies,
        List<String> unlockRecipes,
        List<String> usePermissions
) {
    public String getNameTranslationKey() {
        return "research." + "timex_rebirth.node." + id;
    }

    public String getDescTranslationKey() {
        return "research." + "timex_rebirth.node." + id + ".desc";
    }

    public String getCategoryTranslationKey() {
        return "research." + "timex_rebirth.category." + category;
    }

    public int durationTicks() {
        return durationSeconds * 20;
    }
}
