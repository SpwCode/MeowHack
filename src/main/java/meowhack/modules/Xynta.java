package meowhack.modules;

import meowhack.AddonTemplate;
import meteordevelopment.meteorclient.events.entity.player.BreakBlockEvent;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class Xynta extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> directionMode  = sgGeneral.add(new BoolSetting.Builder()
        .name("direction-Mode")
        .description("Activate")
        .defaultValue(false)
        .build()
    );

    private final Setting<Xynta.Modes> mode = sgGeneral.add(new EnumSetting.Builder<Xynta.Modes>()
        .name("mode")
        .description("modes")
        .defaultValue(Modes.West_East)
        .visible(directionMode::get)
        .build()
    );

    public Xynta() {
        super(AddonTemplate.CATEGORY, "Xynta", "Obsidian double breaker");
    }
    BlockPos neighborPos = null;


    @EventHandler
    private void onStartBreakingBlock(StartBreakingBlockEvent event) {
        BlockPos breakingPos = event.blockPos;
        if (mc.world.getBlockState(breakingPos).getBlock() != Blocks.OBSIDIAN) return;

        if (directionMode.get()) {
            switch (mode.get()) {
                case West_East -> {
                    if (tryBreakNearby(breakingPos, 1, 0)) return;
                    tryBreakNearby(breakingPos, 2, 0);
                }

                case North_South -> {
                    if (tryBreakNearby(breakingPos, 0, 1)) return;
                    tryBreakNearby(breakingPos, 0, 2);
                }
            }
            return;
        }

        if (tryBreakNearby(breakingPos, 1, 1)) return;
        tryBreakNearby(breakingPos, 2, 2);
    }

    private boolean tryBreakNearby(BlockPos center, int radiusX, int radiusZ) {
        for (int x = -radiusX; x <= radiusX; x++) {
            for (int z = -radiusZ; z <= radiusZ; z++) {
                BlockPos candidatePos = center.add(x, 0, z);
                if (candidatePos.equals(center)) continue;

                BlockState neighborState = mc.world.getBlockState(candidatePos);
                if (neighborState.getBlock() == Blocks.OBSIDIAN && !isPlayerStandingOn(candidatePos)) {
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, candidatePos, Direction.UP));
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, candidatePos, Direction.UP));
                    this.neighborPos = candidatePos;
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isPlayerStandingOn(BlockPos pos) {
            BlockPos playerPos = mc.player.getBlockPos().down();
            if (playerPos.equals(pos)) {
                return true;
            }
        return false;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (neighborPos == null) return;
            event.renderer.box(neighborPos, Color.WHITE, Color.PINK, ShapeMode.Lines, 0);
    }

    public enum Modes{
        West_East,
        North_South
    }
}
