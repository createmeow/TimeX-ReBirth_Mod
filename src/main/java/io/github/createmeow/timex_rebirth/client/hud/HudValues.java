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
 *
 * <p><b>性能</b>：反射调用昂贵，因此所有跨模组数据采用「快照缓存」——
 * 每 0.5 秒刷新一轮（{@link #REFRESH_INTERVAL_MS}），渲染帧直接读取缓存值，
 * 不再逐帧触发 {@code getMethod/invoke}。为避免"一次刷新在单帧内集中执行 12 次反射"
 * 造成卡顿，将一轮刷新拆成 {@link #PHASES} 个时间相位，每个相位只更新一小撮数据，
 * 使查询在 0.5s 窗口内摊开执行（任意时刻最多 2 次反射）。
 */
public final class HudValues {

    /** 跨模组数据快照一轮刷新周期（毫秒）。 */
    private static final long REFRESH_INTERVAL_MS = 500;
    /** 一轮被拆成的相位数量：每个相位只刷新 2 项数据，摊开反射开销。 */
    private static final int PHASES = 6;
    /** 单个相位时长（毫秒）。 */
    private static final long PHASE_LEN_MS = REFRESH_INTERVAL_MS / PHASES;

    private HudValues() {
    }

    /** 数值四舍五入保留 0.1 精度（HUD 显示统一精度）。 */
    public static float round1(float v) {
        return Math.round(v * 10f) / 10f;
    }

    public static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    // ─────────────────────────── 原版数据（廉价，直读） ───────────────────────────

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

    // ─────────────────────── 快照缓存（0.5s 一轮，按相位摊开刷新） ───────────────────────

    private static long lastRefresh;
    private static boolean refreshedOnce;
    /** 上次刷新的相位编号（用于相邻帧去重）。 */
    private static int lastRefreshedPhase = -1;

    /** 快照字段：所有跨模组数据的时序缓存。 */
    private static float snapRvSanity;
    private static float snapRvHealth;
    private static float snapThirst;
    private static float snapThirstQuenched;
    private static double snapCsCore;
    private static double snapCsWorld;
    private static double snapCsWorldCelsius;
    private static long snapCurrencyRaw;
    private static boolean snapInAircraft;
    private static float snapAircraftDurability;
    private static float snapAircraftEngine;
    private static int snapFps;

    /**
     * 按时间片轮询刷新：把每轮 REFRESH_INTERVAL_MS 拆成 PHASES 个相位，
     * 相邻相位之间依次刷新不同的一组数据，从而把反射摊开在 0.5s 窗口内，
     * 单帧最多执行 2 次反射查询，避免集中卡顿。
     */
    private static void ensureSnapshot() {
        long now = System.currentTimeMillis();
        // 兜底整轮：若长时间未进入新相位（如切屏），刷新间隔兜底仍推进一轮
        if (!refreshedOnce) {
            lastRefresh = now;
            refreshedOnce = true;
        }
        int phase = (int) ((now / PHASE_LEN_MS) % PHASES);
        if (phase == lastRefreshedPhase && now - lastRefresh < REFRESH_INTERVAL_MS) return;
        lastRefreshedPhase = phase;
        lastRefresh = now;
        refreshPhase(phase);
    }

    /** 刷新指定相位对应的一组数据（每相位 ≤2 项）。 */
    private static void refreshPhase(int phase) {
        switch (phase) {
            case 0 -> {
                snapRvSanity = computeRvSanity();
                snapRvHealth = computeRvHealth();
            }
            case 1 -> {
                snapThirst = computeThirst();
                snapThirstQuenched = computeThirstQuenched();
            }
            case 2 -> {
                snapCsCore = computeCsCore();
                snapCsWorld = computeCsWorld();
            }
            case 3 -> {
                snapCsWorldCelsius = computeCsWorldCelsius(snapCsWorld);
                snapCurrencyRaw = computeCurrencyRaw();
            }
            case 4 -> {
                snapInAircraft = computeInAircraft();
                snapAircraftDurability = computeAircraftDurability();
            }
            case 5 -> {
                snapAircraftEngine = computeAircraftEngine();
                snapFps = computeFps();
            }
            default -> {
            }
        }
    }

    // ─────────────────────── 反射元数据（类加载时解析一次） ───────────────────────

    private static volatile boolean rvChecked = false;
    private static boolean rvLoaded = false;
    private static Class<?> rvClientDataClass;
    private static Method rvGetSanityMethod;
    private static Method rvGetHealthMethod;
    private static Object rvDefaultMaxHealth;

    private static boolean rvLoaded() {
        if (!rvChecked) {
            rvChecked = true;
            try {
                if (ModList.get().isLoaded("reality_value")) {
                    rvClientDataClass = Class.forName("dev.anye.mc.reality_value.cap.ClientPlayerExData");
                    rvGetSanityMethod = rvClientDataClass.getMethod("getSanity");
                    rvGetHealthMethod = rvClientDataClass.getMethod("getHealth");
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

    private static volatile boolean thirstChecked = false;
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

    private static volatile boolean csChecked = false;
    private static boolean csLoaded = false;
    private static Class<?> csTempClass;
    private static Class<?> csTraitClass;
    private static Object csCoreTrait;
    private static Object csWorldTrait;
    private static Object csUnitC;
    private static Object csUnitMc;
    private static Method csGetMethod;
    private static Method csConvertMethod;

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
                    Class<?> csUnitsClass = Class.forName("com.momosoftworks.coldsweat.api.util.Temperature$Units");
                    for (Object u : (Object[]) csUnitsClass.getMethod("values").invoke(null)) {
                        String n = ((Enum<?>) u).name();
                        if ("C".equals(n)) csUnitC = u;
                        if ("MC".equals(n)) csUnitMc = u;
                    }
                    csGetMethod = csTempClass.getMethod("get", LivingEntity.class, csTraitClass);
                    csConvertMethod = csTempClass.getMethod("convert", double.class,
                            (Class<?>) csUnitsClass, (Class<?>) csUnitsClass, boolean.class);
                    csLoaded = true;
                }
            } catch (Exception e) {
                csLoaded = false;
            }
        }
        return csLoaded;
    }

    private static volatile boolean numChecked = false;
    private static boolean numLoaded = false;
    private static Method numGetValueMethod;

    private static boolean numLoaded() {
        if (!numChecked) {
            numChecked = true;
            try {
                if (ModList.get().isLoaded("numismaticoverhaul")) {
                    Class<?> currencyHolderClass = Class.forName("tallestred.numismaticoverhaul.cap.CurrencyHolder");
                    numGetValueMethod = currencyHolderClass.getMethod("getValue",
                            net.minecraft.world.entity.player.Player.class);
                    numLoaded = true;
                }
            } catch (Exception e) {
                numLoaded = false;
            }
        }
        return numLoaded;
    }

    private static volatile boolean iaChecked = false;
    private static boolean iaLoaded = false;
    private static Class<?> aircraftClass;
    private static Method aircraftGetHealth;
    private static Method aircraftGetEnginePower;

    private static boolean iaLoaded() {
        if (!iaChecked) {
            iaChecked = true;
            try {
                if (ModList.get().isLoaded("immersive_aircraft")) {
                    aircraftClass = Class.forName("immersive_aircraft.entity.AircraftEntity");
                    aircraftGetHealth = aircraftClass.getMethod("getHealth");
                    aircraftGetEnginePower = aircraftClass.getMethod("getEnginePower");
                    iaLoaded = true;
                }
            } catch (Exception e) {
                iaLoaded = false;
            }
        }
        return iaLoaded;
    }

    private static Field fpsField;

    private static int computeFps() {
        try {
            if (fpsField == null) {
                fpsField = Minecraft.class.getDeclaredField("fps");
                fpsField.setAccessible(true);
            }
            return fpsField.getInt(Minecraft.getInstance());
        } catch (Exception e) {
            return 0;
        }
    }

    // ─────────────────────── 各模组数据的实际计算（仅快照刷新时执行） ───────────────────────

    private static float computeRvSanity() {
        if (!rvLoaded()) return 0;
        try {
            return round1(((Number) rvGetSanityMethod.invoke(null)).floatValue());
        } catch (Exception e) {
            return 0;
        }
    }

    private static float computeRvHealth() {
        if (!rvLoaded()) return 0;
        try {
            return round1(((Number) rvGetHealthMethod.invoke(null)).floatValue());
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

    /** 反射读取玩家 ThirstWasTaken 数据对象（IThirst 实现），缺失时返回 null。 */
    private static Object thirstData() {
        LocalPlayer p = player();
        if (!thirstLoaded() || p == null) return null;
        try {
            // PLAYER_THIRST 是 Supplier<AttachmentType<?>>，需先 get() 拿到 AttachmentType
            Object supplier = thirstAttachmentField.get(null);
            Object attachment = ((java.util.function.Supplier<?>) supplier).get();
            if (attachment == null) return null;
            java.lang.reflect.Method getData = p.getClass().getMethod("getData",
                    net.neoforged.neoforge.attachment.AttachmentType.class);
            return getData.invoke(p, attachment);
        } catch (Exception e) {
            return null;
        }
    }

    private static float computeThirst() {
        Object thirst = thirstData();
        if (thirst == null) return 0;
        try {
            Method m = thirst.getClass().getMethod("getThirst");
            return round1(((Number) m.invoke(thirst)).floatValue());
        } catch (Exception e) {
            return 0;
        }
    }

    private static float computeThirstQuenched() {
        Object thirst = thirstData();
        if (thirst == null) return 0;
        try {
            Method m = thirst.getClass().getMethod("getQuenched");
            return round1(((Number) m.invoke(thirst)).floatValue());
        } catch (Exception e) {
            return 0;
        }
    }

    private static double computeCsCore() {
        LocalPlayer p = player();
        if (!csLoaded() || p == null) return 0;
        try {
            return round1(((Number) csGetMethod.invoke(null, p, csCoreTrait)).doubleValue());
        } catch (Exception e) {
            return 0;
        }
    }

    private static double computeCsWorld() {
        LocalPlayer p = player();
        if (!csLoaded() || p == null) return 0;
        try {
            return round1(((Number) csGetMethod.invoke(null, p, csWorldTrait)).doubleValue());
        } catch (Exception e) {
            return 0;
        }
    }

    private static double computeCsWorldCelsius(double world) {
        if (!csLoaded()) return 0;
        try {
            return round1(((Number) csConvertMethod.invoke(null, world, csUnitMc, csUnitC, true)).doubleValue());
        } catch (Exception e) {
            return 0;
        }
    }

    private static long computeCurrencyRaw() {
        LocalPlayer p = player();
        if (!numLoaded() || p == null) return 0;
        try {
            return ((Number) numGetValueMethod.invoke(null, p)).longValue();
        } catch (Exception e) {
            return 0;
        }
    }

    private static boolean computeInAircraft() {
        LocalPlayer p = player();
        if (!iaLoaded() || p == null) return false;
        return aircraftClass.isInstance(p.getVehicle());
    }

    private static float computeAircraftDurability() {
        LocalPlayer p = player();
        if (!iaLoaded() || p == null) return 0;
        try {
            Entity v = p.getVehicle();
            if (v != null && aircraftClass.isInstance(v)) {
                return ((Number) aircraftGetHealth.invoke(v)).floatValue() * 100f;
            }
        } catch (Exception e) {
            // fall through
        }
        return 0;
    }

    private static float computeAircraftEngine() {
        LocalPlayer p = player();
        if (!iaLoaded() || p == null) return 0;
        try {
            Entity v = p.getVehicle();
            if (v != null && aircraftClass.isInstance(v)) {
                return ((Number) aircraftGetEnginePower.invoke(v)).floatValue();
            }
        } catch (Exception e) {
            // fall through
        }
        return 0;
    }

    // ─────────────────────── 对外 API：读取 1Hz 快照 ───────────────────────

    /** RealityValue 理智值（1 秒快照）。 */
    public static float realityValueSanity() {
        ensureSnapshot();
        return snapRvSanity;
    }

    /** RealityValue 健康值（1 秒快照）。 */
    public static float realityValueHealth() {
        ensureSnapshot();
        return snapRvHealth;
    }

    /** ThirstWasTaken 口渴值（0~20，1 秒快照）。 */
    public static float thirst() {
        ensureSnapshot();
        return snapThirst;
    }

    /** ThirstWasTaken 解渴度（1 秒快照）。 */
    public static float thirstQuenched() {
        ensureSnapshot();
        return snapThirstQuenched;
    }

    /** ColdSweat 核心体温（MC 单位，1 秒快照）。 */
    public static double coldSweatCore() {
        ensureSnapshot();
        return snapCsCore;
    }

    /** ColdSweat 环境温度（MC 单位，1 秒快照）。 */
    public static double coldSweatWorld() {
        ensureSnapshot();
        return snapCsWorld;
    }

    /** ColdSweat 环境温度（摄氏度，1 秒快照）。 */
    public static double coldSweatWorldCelsius() {
        ensureSnapshot();
        return snapCsWorldCelsius;
    }

    /** NumismaticOverhaul 原始货币值（1 秒快照）。 */
    public static long numismaticRaw() {
        ensureSnapshot();
        return snapCurrencyRaw;
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

    /** 是否正在驾驶沉浸式飞机（1 秒快照）。 */
    public static boolean inAircraft() {
        ensureSnapshot();
        return snapInAircraft;
    }

    /** 飞机耐久（0~100，1 秒快照）。 */
    public static float aircraftDurability() {
        ensureSnapshot();
        return snapAircraftDurability;
    }

    /** 飞机引擎功率（0~1，1 秒快照）。 */
    public static float aircraftEngine() {
        ensureSnapshot();
        return snapAircraftEngine;
    }

    /** FPS（1 秒快照）。 */
    public static int fps() {
        ensureSnapshot();
        return snapFps;
    }
}
