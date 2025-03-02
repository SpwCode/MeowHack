package meowhack.modules;


import meowhack.AddonTemplate;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.FreeLook;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MoveHelper extends Module {
        private final SettingGroup sgGeneral = settings.getDefaultGroup();
        private final Setting<Boolean> freeLook  = sgGeneral.add(new BoolSetting.Builder()
            .name("toggle-free-look")
            .description("Activate FreeLook Module")
            .defaultValue(true)
            .build()
        );

        private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("The amount of delay between interactions.")
        .defaultValue(3)
        .min(0)
        .sliderRange(1, 200)
        .build()
        );

        private final Setting<Integer> searchRadiusX = sgGeneral.add(new IntSetting.Builder()
        .name("radius-X")
        .description("How far blocks to search.")
        .defaultValue(10)
        .min(1)
        .sliderRange(1, 200)
        .build()
        );

        private final Setting<Integer> searchRadiusY = sgGeneral.add(new IntSetting.Builder()
        .name("radius-Y")
        .description("How far blocks to search.")
        .defaultValue(5)
        .min(-1)
        .sliderRange(-1, 200)
        .build()
        );

        private final Setting<Boolean> whitelistenabled = sgGeneral.add(new BoolSetting.Builder()
        .name("whitelist-enabled")
        .description("Only place selected blocks.")
        .defaultValue(false)
        .build()
        );

        private final Setting<List<Block>> whitelist = sgGeneral.add(new BlockListSetting.Builder()
        .name("whitelist")
        .description("Blocks to place.")
        .visible(whitelistenabled::get)
        .build()
        );


    private int delayTimer;
    private BlockPos nearestBlock = null; // Хранит ближайший блок

    public MoveHelper() {
        super(AddonTemplate.CATEGORY, "MoveHelper", "Assists player movement.");
    }
    boolean unpessed;
    boolean blockFound;
    @Override
    public void onActivate() {
        if (freeLook.get() && !Modules.get().isActive(FreeLook.class)) Modules.get().get(FreeLook.class).toggle();
    }
    @Override
    public void onDeactivate() {
        if (freeLook.get() && Modules.get().isActive(FreeLook.class)) Modules.get().get(FreeLook.class).toggle();
        unpress();
    }

    private boolean isLavaNearby(BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos adjacent = pos.offset(direction);
            if (mc.world.getBlockState(adjacent).getBlock() == Blocks.LAVA) {
                return true;
            }
        }
        return false;
    }
    @EventHandler
    private void onTick(TickEvent.Pre e) {
        if (mc.player == null || mc.world == null) return;
        if (delayTimer > 0) {
            delayTimer--;
            return;
        }

        // Reset delay
        delayTimer = delay.get();

        // Список для хранения блоков, отсортированных по расстоянию до игрока
        List<BlockPos> sortedBlocks = new ArrayList<>();

        int chunkX = mc.player.getBlockX() >> 4; // Текущий X-координата чанка
        int chunkZ = mc.player.getBlockZ() >> 4; // Текущий Z-координата чанка

// Определяем границы текущего чанка (16x16 блоков)
        int startX = chunkX << 4;
        int endX = startX + 15;
        int startZ = chunkZ << 4;
        int endZ = startZ + 15;

// Ограничиваем поиск в границах текущего чанка
        BlockIterator.register(16, searchRadiusY.get(), (pos, blockState) -> {
            if (pos.getX() >= startX && pos.getX() <= endX && pos.getZ() >= startZ && pos.getZ() <= endZ) {
                if (whitelistenabled.get() && whitelist.get().contains(blockState.getBlock()) && !isLavaNearby(pos) && !blockState.isAir()) {
                    sortedBlocks.add(new BlockPos(pos));
                }
            }
        });

        // Сортировка блоков по расстоянию до игрока
        BlockIterator.after(() -> {
        sortedBlocks.sort(Comparator.comparingDouble(pos -> mc.player != null ? Utils.squaredDistance(mc.player.getX(), mc.player.getY(), mc.player.getZ(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) : 0));

        if (!sortedBlocks.isEmpty()) {
            BlockPos nearestBlock = sortedBlocks.get(0);
            setNearestBlock(nearestBlock);
            blockFound = true;
            unpessed = false;
            playerMoveTo(nearestBlock);
            return;
        } else {
            blockFound = false;
            // info("unpessed = false");
        }

        if (!unpessed && !blockFound) {
            unpress();
            unpessed = true;
        }
    });

    }

    private void playerMoveTo(BlockPos blockPos) {
        double playerX = mc.player.getX();
        double playerZ = mc.player.getZ();

        double deltaX = blockPos.getX() + 0.5 - playerX;
        double deltaZ = blockPos.getZ() + 0.5 - playerZ;

        float yaw = (float) (Math.atan2(deltaZ, deltaX) * (180.0 / Math.PI)) - 90.0F; // Расчет yaw

        mc.player.setYaw(yaw); // Установка yaw игрока
        mc.options.forwardKey.setPressed(true);


    }


    private void unpress() {
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
    }

    // Сохраняем ближайший блок после сортировки
    private void setNearestBlock(BlockPos pos) {
        this.nearestBlock = pos;
    }

    // Рендерим ближайший блок
    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (nearestBlock == null) return;
        Vec3d pos = new Vec3d(nearestBlock.getX(), nearestBlock.getY(), nearestBlock.getZ());
        event.renderer.box(nearestBlock,meteordevelopment.meteorclient.utils.render.color.Color.WHITE, meteordevelopment.meteorclient.utils.render.color.Color.WHITE, ShapeMode.Lines, 0);
        //box(rpos, RenderColor.get(), null, ShapeMode.Sides, 0);
    }
}
