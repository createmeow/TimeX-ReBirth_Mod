package io.github.createmeow.timex_rebirth.heat;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/**
 * 供热调试命令（阶段1测试用，需权限 2）：
 * /timex_heat fill <mb>    → 给玩家视线中的热源接收器注入热流
 * /timex_heat clear        → 清空视线中热源接收器的热流
 */
@EventBusSubscriber(modid = TimeX.MODID)
public class HeatCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("timex_heat")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("fill")
                        .then(Commands.argument("mb", IntegerArgumentType.integer(1, 1000000))
                                .executes(ctx -> fill(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "mb")))))
                .then(Commands.literal("clear")
                        .executes(ctx -> clear(ctx.getSource())))
        );
    }

    private static int fill(CommandSourceStack source, int mb) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("§c该命令只能由玩家执行"));
            return 0;
        }
        BlockHitResult hit = raycastBlock(player, 8.0);
        if (hit.getType() != HitResult.Type.BLOCK) {
            source.sendFailure(Component.literal("§c请看向热源接收器"));
            return 0;
        }
        BlockPos pos = hit.getBlockPos();
        if (!(player.level().getBlockEntity(pos) instanceof HeatReceiverBlockEntity be)) {
            source.sendFailure(Component.literal("§c目标不是热源接收器"));
            return 0;
        }
        int filled = be.getTank().fill(new FluidStack(HeatRegistry.HEAT_FLUX.get(), mb),
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        source.sendSuccess(() -> Component.literal("§a注入热流 §e" + filled + " §amb（当前 "
                + be.getTank().getFluidAmount() + "/" + be.getTank().getCapacity() + "）"), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int clear(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("§c该命令只能由玩家执行"));
            return 0;
        }
        BlockHitResult hit = raycastBlock(player, 8.0);
        if (hit.getType() != HitResult.Type.BLOCK) {
            source.sendFailure(Component.literal("§c请看向热源接收器"));
            return 0;
        }
        BlockPos pos = hit.getBlockPos();
        if (!(player.level().getBlockEntity(pos) instanceof HeatReceiverBlockEntity be)) {
            source.sendFailure(Component.literal("§c目标不是热源接收器"));
            return 0;
        }
        be.getTank().drain(Integer.MAX_VALUE, FluidAction.EXECUTE);
        source.sendSuccess(() -> Component.literal("§a已清空该热源接收器的热流"), false);
        return Command.SINGLE_SUCCESS;
    }

    private static BlockHitResult raycastBlock(ServerPlayer player, double range) {
        Vec3 from = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 to = from.add(look.x * range, look.y * range, look.z * range);
        return player.level().clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
    }
}
