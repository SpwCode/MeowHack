package meowhack.modules;

import meowhack.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class VanillaNuker extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgWhitelist = settings.createGroup("Whitelist");
    private final SettingGroup sgBlacklist = settings.createGroup("Blacklist");

    // General

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("The way the blocks are broken.")
        .defaultValue(Mode.Flatten)
        .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("The break range.")
        .defaultValue(4)
        .min(0)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay in ticks between breaking blocks.")
        .defaultValue(0)
        .build()
    );

    private final Setting<Integer> maxBlocksPerTick = sgGeneral.add(new IntSetting.Builder()
        .name("max-blocks-per-tick")
        .description("Maximum blocks to try to break per tick. Useful when insta mining.")
        .defaultValue(1)
        .min(1)
        .sliderRange(1, 6)
        .build()
    );

    private final Setting<SortMode> sortMode = sgGeneral.add(new EnumSetting.Builder<SortMode>()
        .name("sort-mode")
        .description("The blocks you want to mine first.")
        .defaultValue(SortMode.Closest)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Automatically rotates towards the space targeted for filling.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> ignoreNearLava = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-near-lava")
        .description("Prevents mining blocks near lava.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> antiGhostBlock = sgGeneral.add(new BoolSetting.Builder()
        .name("antiGhostBlock")
        .description("Send 1 additional packet to prevent ghost blocks")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> swingHand = sgGeneral.add(new BoolSetting.Builder()
        .name("swing-hand")
        .description("Swing hand client side.")
        .defaultValue(true)
        .build()
    );

    // Whitelist

    private final Setting<Boolean> whitelistEnabled = sgWhitelist.add(new BoolSetting.Builder()
        .name("whitelist-enabled")
        .description("Only mines selected blocks.")
        .defaultValue(false)
        .build()
    );

    private final Setting<List<Block>> whitelist = sgWhitelist.add(new BlockListSetting.Builder()
        .name("whitelist")
        .description("The blocks you want to mine.")
        .visible(whitelistEnabled::get)
        .build()
    );

    // Blacklist

    private final Setting<Boolean> blacklistEnabled = sgBlacklist.add(new BoolSetting.Builder()
        .name("blacklist-enabled")
        .description("Don't mine selected blocks.")
        .defaultValue(false)
        .build()
    );

    private final Setting<List<Block>> blacklist = sgBlacklist.add(new BlockListSetting.Builder()
        .name("blacklist")
        .description("The blocks you don't want to mine.")
        .visible(blacklistEnabled::get)
        .build()
    );

    private final Pool<BlockPos.Mutable> blockPosPool = new Pool<>(BlockPos.Mutable::new);
    private final List<BlockPos.Mutable> blocks = new ArrayList<>();

    private boolean firstBlock;
    private final BlockPos.Mutable lastBlockPos = new BlockPos.Mutable();

    private int timer;
    private int noBlockTimer;

    public VanillaNuker() {
        super(AddonTemplate.CATEGORY, "Vanilla-Nuker", "Breaks blocks around you.");
    }

    @Override
    public void onActivate() {
        firstBlock = true;
        timer = 0;
        noBlockTimer = 0;
    }

    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        // Update timer
        if (timer > 0) {
            timer--;
            return;
        }

        // Calculate some stuff
        double pX = mc.player.getX();
        double pY = mc.player.getY();
        double pZ = mc.player.getZ();

        double rangeSq = Math.pow(range.get(), 2);

        // Find blocks to break
        BlockIterator.register((int) Math.ceil(range.get()), (int) Math.ceil(range.get()), (blockPos, blockState) -> {
            if (!BlockUtils.canBreak(blockPos, blockState) || Utils.squaredDistance(pX, pY, pZ, blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5) > rangeSq) return;

            if (mode.get() == Mode.Flatten && blockPos.getY() < Math.floor(mc.player.getY())) return;

            if (mode.get() == Mode.Smash && blockState.getHardness(mc.world, blockPos) != 0) return;

            if (whitelistEnabled.get() && !whitelist.get().contains(blockState.getBlock())) return;
            if (blacklistEnabled.get() && blacklist.get().contains(blockState.getBlock())) return;

            // Если флаг включён и рядом есть лава, не добавляем блок
            if (ignoreNearLava.get() && isLavaNearby(blockPos)) return;

            blocks.add(blockPosPool.get().set(blockPos));
        });

        // Break block if found
        BlockIterator.after(() -> {
            // Sort blocks
            if (sortMode.get() != SortMode.None) {
                blocks.sort(Comparator.comparingDouble(value -> Utils.squaredDistance(pX, pY, pZ, value.getX() + 0.5, value.getY() + 0.5, value.getZ() + 0.5)
                    * (sortMode.get() == SortMode.Closest ? 1 : -1)));
            }

            // Check if some block was found
            if (blocks.isEmpty()) {
                if (noBlockTimer++ >= delay.get()) firstBlock = true;
                return;
            } else {
                noBlockTimer = 0;
            }

            // Update timer
            if (!firstBlock && !lastBlockPos.equals(blocks.get(0))) {
                timer = delay.get();
                firstBlock = false;
                lastBlockPos.set(blocks.get(0));
                if (timer > 0) return;
            }

            // Break blocks
            int count = 0;
            for (BlockPos block : blocks) {
                if (count >= maxBlocksPerTick.get()) break;

                boolean canInstaMine = BlockUtils.canInstaBreak(block);

                if (rotate.get()) Rotations.rotate(Rotations.getYaw(block), Rotations.getPitch(block));
                if (antiGhostBlock.get())
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, block, Direction.UP));

                if (mc.interactionManager.isBreakingBlock())
                    mc.interactionManager.updateBlockBreakingProgress(block, getDirection(block));
                else
                    mc.interactionManager.attackBlock(block, getDirection(block));

                //BlockUtils.breakBlock(block, swingHand.get());
                lastBlockPos.set(block);

                count++;
                if (!canInstaMine) break;
            }

            firstBlock = false;

            // Clear current block positions
            for (BlockPos.Mutable blockPos : blocks) blockPosPool.free(blockPos);
            blocks.clear();
        });
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

    private Direction getDirection(BlockPos pos) {
        Vec3d eyesPos = new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());
        if ((double) pos.getY() > eyesPos.y) {
            if (mc.world.getBlockState(pos.add(0, -1, 0)).isReplaceable()) return Direction.DOWN;
            else return mc.player.getHorizontalFacing().getOpposite();
        }
        if (!mc.world.getBlockState(pos.add(0, 1, 0)).isReplaceable()) return mc.player.getHorizontalFacing().getOpposite();
        return Direction.UP;
    }

    public enum Mode {
        All,
        Flatten,
        Smash
    }

    public enum SortMode {
        None,
        Closest,
        Furthest
    }
}
