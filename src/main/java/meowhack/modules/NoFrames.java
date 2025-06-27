package meowhack.modules;

import meowhack.AddonTemplate;
import meteordevelopment.meteorclient.events.entity.player.InteractEntityEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class NoFrames extends Module {
    public NoFrames() {
        super(AddonTemplate.CATEGORY, "no-frames", "Click through item frames onto chests.");
    }

    @EventHandler
    private void onInteractEntity(InteractEntityEvent event) {
        if (!(event.entity instanceof ItemFrameEntity frame)) return;
        if (mc.player.isSneaking()) return;

        // Получаем позицию блока за рамкой
        Direction facing = frame.getHorizontalFacing(); // направление "лицом" от рамки
        BlockPos behind = frame.getBlockPos().offset(facing.getOpposite());

        // Проверяем, сундук ли это
        if (mc.world.getBlockState(behind).getBlock() == Blocks.CHEST || mc.world.getBlockState(behind).getBlock() == Blocks.BARREL){
            event.cancel(); // Отменяем клик по рамке

            // Симулируем клик по сундуку
            Vec3d clickPos = Vec3d.ofCenter(behind);
            BlockHitResult hitResult = new BlockHitResult(clickPos, facing, behind, false);
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
        }
    }
}
