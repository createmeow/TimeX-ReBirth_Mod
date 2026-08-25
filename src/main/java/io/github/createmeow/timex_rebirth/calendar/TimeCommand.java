package io.github.createmeow.timex_rebirth.calendar;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * /yeartime 命令：查看/修改世界年份日期。
 * /yeartime                        → 显示当前日期
 * /yeartime <YYYY> <MM> <DD>       → 设置日期（需权限 2）
 */
@EventBusSubscriber(modid = TimeX.MODID)
public class TimeCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("yeartime")
                .executes(ctx -> show(ctx.getSource()))
                .then(Commands.argument("year", IntegerArgumentType.integer(0, 999999))
                        .then(Commands.argument("month", IntegerArgumentType.integer(1, 12))
                                .then(Commands.argument("day", IntegerArgumentType.integer(1, 31))
                                        .requires(src -> src.hasPermission(2))
                                        .executes(ctx -> set(ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "year"),
                                                IntegerArgumentType.getInteger(ctx, "month"),
                                                IntegerArgumentType.getInteger(ctx, "day")))
                                )
                        )
                )
        );
    }

    private static int show(CommandSourceStack source) {
        if (!(source.getLevel() instanceof ServerLevel level)) return 0;
        long dayIndex = level.getDayTime() / 24000L;
        CalendarSystem.DateInfo date = CalendarSystem.fromDayIndex(dayIndex);
        source.sendSuccess(() -> Component.literal("§6[年份] §e" + date.year()
                        + " §7年 §e" + date.month() + " §7月 §e" + date.dayOfMonth() + " §7日"
                        + " §8(第 " + dayIndex + " 天)"),
                false);
        return Command.SINGLE_SUCCESS;
    }

    private static int set(CommandSourceStack source, int year, int month, int day) {
        if (!(source.getLevel() instanceof ServerLevel level)) return 0;
        int maxDay = CalendarSystem.daysInMonth(month);
        if (day > maxDay) {
            source.sendFailure(Component.literal("§c无效日期：第 " + month + " 月最多 " + maxDay + " 天"));
            return 0;
        }
        long dayIndex = CalendarSystem.toDayIndex(year, month, day);
        level.setDayTime(dayIndex * 24000L);
        source.sendSuccess(() -> Component.literal("§a已设置日期为 §e" + year + " 年 " + month + " 月 " + day + " 日"
                        + " §7(第 " + dayIndex + " 天)"),
                true);
        return Command.SINGLE_SUCCESS;
    }
}
