package meowhack.modules;

import meowhack.AddonTemplate;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.entity.player.InteractEntityEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class NoFrames extends Module {
    public NoFrames() {
        super(AddonTemplate.CATEGORY, "no-frames", "Click through item frames and signs onto chests.");
    }

    // Обработка кликов по рамкам (entity)
    @EventHandler
    private void onInteractEntity(InteractEntityEvent event) {
        if (mc.player.isSneaking()) return;
        if (!(event.entity instanceof ItemFrameEntity frame)) return;

        Direction facing = frame.getHorizontalFacing();
        BlockPos behind = frame.getBlockPos().offset(facing.getOpposite());

        if (isContainerBlock(behind)) {
            event.cancel();
            clickContainer(behind, facing);
        }
    }

    // Обработка кликов по табличкам (block)
    @EventHandler
    private void onInteractBlock(InteractBlockEvent event) {
        if (mc.player.isSneaking()) return;
        BlockPos pos = event.result.getBlockPos();
        Block block = mc.world.getBlockState(pos).getBlock();

        // Проверяем, что кликнули на табличку (все виды табличек)
        if (block != Blocks.OAK_SIGN && block != Blocks.OAK_WALL_SIGN &&
            block != Blocks.SPRUCE_SIGN && block != Blocks.SPRUCE_WALL_SIGN &&
            block != Blocks.BIRCH_SIGN && block != Blocks.BIRCH_WALL_SIGN &&
            block != Blocks.JUNGLE_SIGN && block != Blocks.JUNGLE_WALL_SIGN &&
            block != Blocks.ACACIA_SIGN && block != Blocks.ACACIA_WALL_SIGN &&
            block != Blocks.DARK_OAK_SIGN && block != Blocks.DARK_OAK_WALL_SIGN &&
            block != Blocks.CRIMSON_SIGN && block != Blocks.CRIMSON_WALL_SIGN &&
            block != Blocks.WARPED_SIGN && block != Blocks.WARPED_WALL_SIGN) {
            return;
        }
        if (mc.player.isSneaking()) return;

        Direction facing = event.result.getSide(); // Направление клика
        BlockPos behind = pos.offset(facing.getOpposite()); // Блок за табличкой

        if (isContainerBlock(behind)) {
            event.cancel();
            clickContainer(behind, facing);
        }
    }

    // Проверяет, является ли блок контейнером
    private boolean isContainerBlock(BlockPos pos) {
        Block block = mc.world.getBlockState(pos).getBlock();
        return block == Blocks.CHEST ||
            block == Blocks.BARREL ||
            block == Blocks.SHULKER_BOX ||
            block == Blocks.TRAPPED_CHEST ||
            block == Blocks.ENDER_CHEST ||
            block == Blocks.DISPENSER ||
            block == Blocks.DROPPER ||
            block == Blocks.HOPPER ||
            block == Blocks.FURNACE ||
            block == Blocks.BLAST_FURNACE ||
            block == Blocks.SMOKER;
    }

    // Симулирует клик по контейнеру
    private void clickContainer(BlockPos pos, Direction facing) {
        Vec3d clickPos = Vec3d.ofCenter(pos);
        BlockHitResult hitResult = new BlockHitResult(clickPos, facing, pos, false);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
    }
}
