package com.createmeow.underwaterplugin;

import com.simibubi.create.content.equipment.armor.BacktankItem;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorMaterial;

import java.util.function.Supplier;

/**
 * 铝背罐：继承 Create 背罐全部能力（create:backtank_air 空气组件、放置充气、
 * 容量附魔、低气量警告）。
 * - 继承 {@link BacktankItem.Layered}：实现 Create 的 LayeredArmorItem 接口，
 *   第三人称盔甲渲染走与下界合金背罐完全相同的路径（HumanoidArmorLayerMixin 拦截 →
 *   LayeredArmorItem.renderArmorPiece 双层渲染：内层模型 aluminum_diving_layer_2 +
 *   外层模型 aluminum_diving_layer_1）。若继承普通 BacktankItem 则走原版单层链路，
 *   手臂贴图不会渲染（铜背罐同款问题）
 * - 物品加入 create:pressurized_air_sources 标签后，Create 潜水头盔/应力吸收等系统天然识别
 * - 防御力取钻石胸甲（见 {@link UnderwaterCreateRegisters#ALUMINUM_BACKTANK_MATERIAL}），
 *   无耐久限制（物品属性未设置 durability，maxDamage=0 永不损耗）
 */
/**
 * 描述统一由 timex_rebirth 的 ItemDescTooltip（ItemTooltipEvent，按 .desc 语言键存在性）渲染，
 * 不在 appendHoverText 里重复添加。
 */
public class AluminumBacktankItem extends BacktankItem.Layered {

    public AluminumBacktankItem(Holder<ArmorMaterial> material, Properties properties,
                                ResourceLocation textureLoc, Supplier<BacktankBlockItem> placeable) {
        super(material, properties, textureLoc, placeable);
    }
}
