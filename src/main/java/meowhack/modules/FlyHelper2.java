package meowhack.modules;

import meowhack.AddonTemplate;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.FreeLook;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CarpetBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FlyHelper2 extends Module {
        private final SettingGroup sgGeneral = settings.getDefaultGroup();
        private final Setting<Boolean> freeLook  = sgGeneral.add(new BoolSetting.Builder()
            .name("toggle-free-look")
            .description("Activate FreeLook Module")
            .defaultValue(true)
            .build()
        );

    private final Setting<Boolean> checkBlocksAround = sgGeneral.add(new BoolSetting.Builder()
        .name("check-blocks-around")
        .description("Enable or disable checking for blocks around position.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> placeInWater = sgGeneral.add(new BoolSetting.Builder()
        .name("place in water")
        .description("place blocks inside water or not")
        .defaultValue(false)
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

        private final Setting<Integer> searchRadiusZ = sgGeneral.add(new IntSetting.Builder()
        .name("radius-Z")
        .description("How far blocks to search.")
        .defaultValue(5)
        .min(1)
        .sliderRange(1, 200)
        .build()
        );

        private final Setting<Boolean> inventoryenabled = sgGeneral.add(new BoolSetting.Builder()
        .name("inventory-blocks-only")
        .description("Only place blocks that you have in inventory")
        .defaultValue(false)
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

        private final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder()
        .name("debug-msg")
        .description("Spam")
        .defaultValue(false)
        .build()
        );

    private int delayTimer;

    public FlyHelper2() {
        super(AddonTemplate.CATEGORY, "FlyHelper2", "Prevent the player from losing the respawn point.");
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

    @EventHandler
    private void onTick(TickEvent.Pre e) {
        if (mc.player == null || mc.world == null) return;
        WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();
        if (worldSchematic == null) {
            info("Не загружена схематика, вырубаю");
            toggle();
            return;
        }
        if (delayTimer > 0) {
            delayTimer--;
            return;
        }

        // Reset delay
        delayTimer = delay.get();

        // Список для хранения блоков, отсортированных по расстоянию до игрока
        List<BlockPos> sortedBlocks = new ArrayList<>();

        BlockIterator.register(searchRadiusX.get(), searchRadiusZ.get(), (pos, blockState) -> {
            BlockState required = worldSchematic.getBlockState(pos);
            BlockState blockInPos = mc.world.getBlockState(pos);
            if (debug.get() && blockInPos.getBlock() != required.getBlock()) {
                info(blockInPos.getBlock().toString());
                info(required.getBlock().toString());
                info(pos.toString());
            }
            if (
                blockInPos.isReplaceable()
                // && mc.player.getBlockPos().isWithinDistance(pos, placeRadius.get())
                && !required.isLiquid()
                && !required.isAir()
                && blockInPos.getBlock() != required.getBlock()
                && ((placeInWater.get()) || (!placeInWater.get() && blockState.getFluidState().getLevel() != 8))
                //&& DataManager.getRenderLayerRange().isPositionWithinRange(pos)
                && !mc.player.getBoundingBox().intersects(Vec3d.of(pos), Vec3d.of(pos).add(1, 1, 1))
                && required.canPlaceAt(mc.world, pos)
                && (!checkBlocksAround.get() || haveBlocksAround(pos))
                && (!whitelistenabled.get() || whitelist.get().contains(required.getBlock()))
                && (!inventoryenabled.get() || InvUtils.find(required.getBlock().asItem()).found())
            ) {
                sortedBlocks.add(new BlockPos(pos));
            }
        });

        // Сортировка блоков по расстоянию до игрока
        BlockIterator.after(() -> {
        sortedBlocks.sort(Comparator.comparingDouble(pos -> mc.player != null ? Utils.squaredDistance(mc.player.getX(), mc.player.getY(), mc.player.getZ(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) : 0));

        if (!sortedBlocks.isEmpty()) {
            BlockPos nearestBlock = sortedBlocks.get(0);
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
            if (debug.get()) info("unpessed WAS PRESSED and true");
            unpessed = true;
        }
    });

    }

    private void playerMoveTo(BlockPos blockPos) {
        if (debug.get()) info("pos = " + blockPos.toString());
        // mc.player.setYaw(150+(float)(Math.random()*60));
        // mc.player.setPitch(60);
        // Движение по икс
        // if (mc.player.getX() > blockPos.getX()) {
        //     mc.options.rightKey.setPressed(false);
        //     mc.options.leftKey.setPressed(true);
        // } else{
        //     mc.options.rightKey.setPressed(true);
        //     mc.options.leftKey.setPressed(false);
        // }

        // // Движение по зет
        // if (mc.player.getZ() > blockPos.getZ()) {
        //     mc.options.backKey.setPressed(false);
        //     mc.options.forwardKey.setPressed(true);
        // } else{
        //     mc.options.backKey.setPressed(true);
        //     mc.options.forwardKey.setPressed(false);
        // }


        double playerX = mc.player.getX();
        double playerZ = mc.player.getZ();

        double deltaX = blockPos.getX() + 0.5 - playerX;
        double deltaZ = blockPos.getZ() + 0.5 - playerZ;

        float yaw = (float) (Math.atan2(deltaZ, deltaX) * (180.0 / Math.PI)) - 90.0F; // Расчет yaw

        mc.player.setYaw(yaw); // Установка yaw игрока
        mc.options.forwardKey.setPressed(true);


    }

    private boolean haveBlocksAround(BlockPos blockPos) {
        BlockPos[] positions = {
            blockPos.west(),
            blockPos.east(),
            blockPos.north(),
            blockPos.south(),
            blockPos.down()
        };

        for (BlockPos pos : positions) {
            Block block = mc.world.getBlockState(pos).getBlock();
            if (block.isShapeFullCube(block.getDefaultState().getCollisionShape(mc.world, pos)) || block instanceof CarpetBlock) {
                return true;
            }
        }

        return false;
    }


    private BlockPos getBlocksAround(BlockPos blockPos) {
        for (int x = -1; x < 1; x++) {
                for (int z = -1; z < 1; z++) {
                    BlockPos blockPos2 = blockPos.add(x, 0, z);
                    Block block = mc.world.getBlockState(blockPos2).getBlock();
                    if (block.isShapeFullCube(block.getDefaultState().getCollisionShape(mc.world, blockPos2)) || (block instanceof CarpetBlock)) {
                            return blockPos2;
                    }
                }
        }
        return null;
    }
    private BlockPos getAirBlockPos(BlockPos blockPos) {
        for (int x = -1; x < 2; x++) {
                for (int z = -1; z < 2; z++) {
                    BlockPos blockPos2 = blockPos.add(x, 0, z);
                    BlockState block = mc.world.getBlockState(blockPos2);
                    if (block.isAir()) {
                            return blockPos2;
                    }
                }
        }
        return blockPos;
    }

    private void unpress() {
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
    }
}
