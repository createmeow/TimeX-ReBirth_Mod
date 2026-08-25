package io.github.createmeow.timex_rebirth.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * HUD 数据提供层：以反射方式读取各联动模组的玩家数据，缺失时优雅降级。
 *
 * <p>数据源对照 commandwithplaceholder 模组的 placeholder 实现：
 * <ul>
 *   <li>理智/健康（RealityValue）：{@code ClientPlayerExData.getSanity()/getHealth()}</li>
 *   <li>口渴（ThirstWasTaken）：{@code ModAttachment.PLAYER_THIRST} 附件的 {@code getThirst()}</li>
 *   <li>体温（ColdSweat）：{@code Temperature.get(player, Trait.CORE/WORLD)}</li>
 *   <li>金币（NumismaticOverhaul）：{@code CurrencyHolder.getValue(player)}</li>
 *   <li>飞机耐久/引擎（Immersive Aircraft）：{@code AircraftEntity.getHealth()/getEnginePower()}</li>
 * </ul>
 */
public final class HudValues {

    private HudValues() {
    }

    /** 数值四舍五入保留 0.1 精度（HUD 显示统一精度）。 */
    public static float round1(float v) {
        return Math.round(v * 10f) / 10f;
    }

    public static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    // ─────────────────────────── 原版数据 ───────────────────────────

    public static LocalPlayer player() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player;
    }

    public static float health() {
        LocalPlayer p = player();
        return p == null ? 0 : round1(p.getHealth());
    }

    public static float maxHealth() {
        LocalPlayer p = player();
        return p == null ? 20 : round1(p.getMaxHealth());
    }

    public static float absorption() {
        LocalPlayer p = player();
        return p == null ? 0 : round1(p.getAbsorptionAmount());
    }

    public static int armor() {
        LocalPlayer p = player();
        return p == null ? 0 : p.getArmorValue();
    }

    public static int hunger() {
        LocalPlayer p = player();
        return p == null ? 0 : p.getFoodData().getFoodLevel();
    }

    public static int airSupply() {
        LocalPlayer p = player();
        return p == null ? 300 : p.getAirSupply();
    }

    public static int maxAirSupply() {
        LocalPlayer p = player();
        return p == null ? 300 : p.getMaxAirSupply();
    }

    public static float expProgress() {
        LocalPlayer p = player();
        return p == null ? 0 : p.experienceProgress;
    }

    public static int level() {
        LocalPlayer p = player();
        return p == null ? 0 : p.experienceLevel;
    }

    /** 骑乘的有生命实体（坐骑/飞机等），未骑乘返回 null。 */
    public static LivingEntity mount() {
        LocalPlayer p = player();
        if (p == null) return null;
        Entity v = p.getVehicle();
        return v instanceof LivingEntity le ? le : null;
    }

    // ─────────────────────── 反射缓存（惰性） ───────────────────────

    private static boolean rvChecked = false;
    private static boolean rvLoaded = false;
    private static Class<?> rvClientDataClass;
    private static Object rvDefaultMaxHealth;

    private static boolean rvLoaded() {
        if (!rvChecked) {
            rvChecked = true;
            try {
                if (ModList.get().isLoaded("reality_value")) {
                    rvClientDataClass = Class.forName("dev.anye.mc.reality_value.cap.ClientPlayerExData");
                    Class<?> capClass = Class.forName("dev.anye.mc.reality_value.cap.PlayerExCap");
                    Field f = capClass.getField("DefaultMaxHealth");
                    rvDefaultMaxHealth = f.get(null);
                    rvLoaded = true;
                }
            } catch (Exception e) {
                rvLoaded = false;
            }
        }
        return rvLoaded;
    }

    /** RealityValue 理智值。 */
    public static float realityValueSanity() {
        if (!rvLoaded()) return 0;
        try {
            Method m = rvClientDataClass.getMethod("getSanity");
            return round1(((Number) m.invoke(null)).floatValue());
        } catch (Exception e) {
            return 0;
        }
    }

    /** RealityValue 健康值。 */
    public static float realityValueHealth() {
        if (!rvLoaded()) return 0;
        try {
            Method m = rvClientDataClass.getMethod("getHealth");
            return round1(((Number) m.invoke(null)).floatValue());
        } catch (Exception e) {
            return 0;
        }
    }

    /** RealityValue 健康最大值。 */
    public static float realityValueMaxHealth() {
        if (!rvLoaded()) return 20;
        try {
            return ((Number) rvDefaultMaxHealth).floatValue();
        } catch (Exception e) {
            return 20;
        }
    }

    private static boolean thirstChecked = false;
    private static boolean thirstLoaded = false;
    private static Field thirstAttachmentField;

    private static boolean thirstLoaded() {
        if (!thirstChecked) {
            thirstChecked = true;
            try {
                if (ModList.get().isLoaded("thirst")) {
                    Class<?> modAtt = Class.forName("dev.ghen.thirst.foundation.common.capability.ModAttachment");
                    thirstAttachmentField = modAtt.getField("PLAYER_THIRST");
                    thirstLoaded = true;
                }
            } catch (Exception e) {
                thirstLoaded = false;
            }
        }
        return thirstLoaded;
    }

    /** ThirstWasTaken 口渴值（0~20）。 */
    public static float thirst() {
        Object thirst = thirstData();
        if (thirst == null) return 0;
        try {
            Method m = thirst.getClass().getMethod("getThirst");
            return round1(((Number) m.invoke(thirst)).floatValue());
        } catch (Exception e) {
            return 0;
        }
    }

    /** ThirstWasTaken 解渴度（口渴饱和度，0~20，对应 AppleSkin 风格的水分条饱和度覆盖）。 */
    public static float thirstQuenched() {
        Object thirst = thirstData();
        if (thirst == null) return 0;
        try {
            Method m = thirst.getClass().getMethod("getQuenched");
            return round1(((Number) m.invoke(thirst)).floatValue());
        } catch (Exception e) {
            return 0;
        }
    }

    /** 反射读取玩家 ThirstWasTaken 数据对象（IThirst 实现），缺失时返回 null。 */
    private static Object thirstData() {
        LocalPlayer p = player();
        if (!thirstLoaded() || p == null) return null;
        try {
            // PLAYER_THIRST 是 Supplier<AttachmentType<?>>，需先 get() 拿到 AttachmentType
            Object supplier = thirstAttachmentField.get(null);
            Object attachment = ((java.util.function.Supplier<?>) supplier).get();
            if (attachment == null) return null;
            // player.getData(AttachmentType<?>)：反射调用以避开编译期依赖
            java.lang.reflect.Method getData = p.getClass().getMethod("getData",
                    net.neoforged.neoforge.attachment.AttachmentType.class);
            return getData.invoke(p, attachment);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean csChecked = false;
    private static boolean csLoaded = false;
    private static Class<?> csTempClass;
    private static Class<?> csTraitClass;
    private static Object csCoreTrait;
    private static Object csWorldTrait;
    private static Object csUnitsClass;
    private static Object csUnitC;

    private static boolean csLoaded() {
        if (!csChecked) {
            csChecked = true;
            try {
                if (ModList.get().isLoaded("cold_sweat")) {
                    csTempClass = Class.forName("com.momosoftworks.coldsweat.api.util.Temperature");
                    csTraitClass = Class.forName("com.momosoftworks.coldsweat.api.util.Temperature$Trait");
                    for (Object t : (Object[]) csTraitClass.getMethod("values").invoke(null)) {
                        String n = ((Enum<?>) t).name();
                        if ("CORE".equals(n)) csCoreTrait = t;
                        if ("WORLD".equals(n)) csWorldTrait = t;
                    }
                    csUnitsClass = Class.forName("com.momosoftworks.coldsweat.api.util.Temperature$Units");
                    for (Object u : (Object[]) ((Class<?>) csUnitsClass).getMethod("values").invoke(null)) {
                        if ("C".equals(((Enum<?>) u).name())) csUnitC = u;
                    }
                    csLoaded = true;
                }
            } catch (Exception e) {
                csLoaded = false;
            }
        }
        return csLoaded;
    }

    /** ColdSweat 核心体温（MC 单位）。 */
    public static double coldSweatCore() {
        LocalPlayer p = player();
        if (!csLoaded() || p == null) return 0;
        try {
            return round1(((Number) csTempClass.getMethod("get", LivingEntity.class, csTraitClass)
                    .invoke(null, p, csCoreTrait)).doubleValue());
        } catch (Exception e) {
            return 0;
        }
    }

    /** ColdSweat 环境温度（MC 单位）。 */
    public static double coldSweatWorld() {
        LocalPlayer p = player();
        if (!csLoaded() || p == null) return 0;
        try {
            return round1(((Number) csTempClass.getMethod("get", LivingEntity.class, csTraitClass)
                    .invoke(null, p, csWorldTrait)).doubleValue());
        } catch (Exception e) {
            return 0;
        }
    }

    /** ColdSweat 环境温度（摄氏度）。 */
    public static double coldSweatWorldCelsius() {
        LocalPlayer p = player();
        if (!csLoaded() || p == null) return 0;
        try {
            Object mcUnits = null;
            for (Object u : (Object[]) ((Class<?>) csUnitsClass).getMethod("values").invoke(null)) {
                if ("MC".equals(((Enum<?>) u).name())) mcUnits = u;
            }
            Method convert = csTempClass.getMethod("convert", double.class, (Class<?>) csUnitsClass,
                    (Class<?>) csUnitsClass, boolean.class);
            return round1(((Number) convert.invoke(null, coldSweatWorld(), mcUnits, csUnitC, true)).doubleValue());
        } catch (Exception e) {
            return 0;
        }
    }

    private static boolean numChecked = false;
    private static boolean numLoaded = false;
    private static Class<?> currencyHolderClass;

    private static boolean numLoaded() {
        if (!numChecked) {
            numChecked = true;
            try {
                if (ModList.get().isLoaded("numismaticoverhaul")) {
                    currencyHolderClass = Class.forName("tallestred.numismaticoverhaul.cap.CurrencyHolder");
                    numLoaded = true;
                }
            } catch (Exception e) {
                numLoaded = false;
            }
        }
        return numLoaded;
    }

    /** NumismaticOverhaul 原始货币值。 */
    public static long numismaticRaw() {
        LocalPlayer p = player();
        if (!numLoaded() || p == null) return 0;
        try {
            // getValue(Player) —— 签名是 Player 而非 Entity
            return ((Number) currencyHolderClass.getMethod("getValue", net.minecraft.world.entity.player.Player.class)
                    .invoke(null, p)).longValue();
        } catch (Exception e) {
            return 0;
        }
    }

    public static long numismaticBronze() {
        return numismaticRaw() % 100;
    }

    public static long numismaticSilver() {
        return (numismaticRaw() % 10000) / 100;
    }

    public static long numismaticGold() {
        return numismaticRaw() / 10000;
    }

    private static boolean iaChecked = false;
    private static boolean iaLoaded = false;
    private static Class<?> aircraftClass;

    private static boolean iaLoaded() {
        if (!iaChecked) {
            iaChecked = true;
            try {
                if (ModList.get().isLoaded("immersive_aircraft")) {
                    aircraftClass = Class.forName("immersive_aircraft.entity.AircraftEntity");
                    iaLoaded = true;
                }
            } catch (Exception e) {
                iaLoaded = false;
            }
        }
        return iaLoaded;
    }

    /** 是否正在驾驶沉浸式飞机。 */
    public static boolean inAircraft() {
        LocalPlayer p = player();
        if (!iaLoaded() || p == null) return false;
        return aircraftClass.isInstance(p.getVehicle());
    }

    /** 飞机耐久（0~100，由 getHealth()*100 得来，与 FancyMenu placeholder 一致）。 */
    public static float aircraftDurability() {
        LocalPlayer p = player();
        if (!iaLoaded() || p == null) return 0;
        try {
            Entity v = p.getVehicle();
            if (v != null && aircraftClass.isInstance(v)) {
                Method m = aircraftClass.getMethod("getHealth");
                return ((Number) m.invoke(v)).floatValue() * 100f;
            }
        } catch (Exception e) {
            // fall through
        }
        return 0;
    }

    /** 飞机引擎功率（0~1）。 */
    public static float aircraftEngine() {
        LocalPlayer p = player();
        if (!iaLoaded() || p == null) return 0;
        try {
            Entity v = p.getVehicle();
            if (v != null && aircraftClass.isInstance(v)) {
                Method m = aircraftClass.getMethod("getEnginePower");
                return ((Number) m.invoke(v)).floatValue();
            }
        } catch (Exception e) {
            // fall through
        }
        return 0;
    }

    /** FPS（Minecraft 内部帧率计数）。 */
    public static int fps() {
        try {
            java.lang.reflect.Field f = Minecraft.class.getDeclaredField("fps");
            f.setAccessible(true);
            return f.getInt(Minecraft.getInstance());
        } catch (Exception e) {
            return 0;
        }
    }
}
