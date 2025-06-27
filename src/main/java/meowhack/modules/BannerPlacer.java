package meowhack.modules;

import meowhack.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BannerBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class BannerPlacer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("place-delay")
        .description("The tick delay between placing blocks.")
        .defaultValue(10)
        .min(0)
        .sliderMax(50)
        .build()
    );

    private final Setting<Integer> placeRange = sgGeneral.add(new IntSetting.Builder()
        .name("place-range")
        .description("How far away from the player you can place a block (in whole blocks).")
        .defaultValue(3)
        .min(1)
        .sliderMax(6)
        .build()
    );

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("blocks")
        .description("Block to use for spawn proofing.")
        .defaultValue(Blocks.BLACK_BANNER)
        .filter(this::isBanner)
        .build()
    );

    private final Pool<BlockPos.Mutable> spawnPool = new Pool<>(BlockPos.Mutable::new);
    private final List<BlockPos.Mutable> spawns = new ArrayList<>();
    private int timer;

    public BannerPlacer() {
        super(AddonTemplate.CATEGORY, "Banner-Placer", "Automatically place banners.");
    }

    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        if (timer < placeDelay.get()) return;

        // Find slot
        boolean foundBlock = InvUtils.testInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem())));
        if (!foundBlock) {
            error("Found none of the chosen blocks in hotbar.");
            toggle();
            return;
        }

        // Find spawn locations
        for (BlockPos.Mutable blockPos : spawns) spawnPool.free(blockPos);
        spawns.clear();

        BlockIterator.register(placeRange.get(), 1, (blockPos, blockState) -> {
            // Проверяем, можно ли поставить блок на этом месте
            if (!BlockUtils.canPlace(blockPos)) return;

            // Проверяем, что сверху НЕТ блока из списка blocks (баннеров)
            BlockPos posBelow = blockPos.down(); // Блок над текущей позицией
            Block blockBelow = mc.world.getBlockState(posBelow).getBlock();

            if (blocks.get().contains(blockBelow)) {
                return; // Пропускаем, если сверху уже баннер/блок из списка
            }

            // Если всё ок — добавляем в spawns
            spawns.add(spawnPool.get().set(blockPos));
        });
    }

    @EventHandler
    private void onTickPost(TickEvent.Post event) {
        // Delay
        if (timer++ < placeDelay.get()) return;

        if (spawns.isEmpty()) return;

        // Find slot
        FindItemResult block = InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem())));
        if (!block.found()) {
            error("Found none of the chosen blocks in hotbar.");
            toggle();
            return;
        }

        int placedCount = 0;


        // Place blocks!
        for (BlockPos blockPos : spawns) {
            if (placedCount >= 1) continue;

            if (BlockUtils.place(blockPos, block, false, -50, false)) {
                placedCount++;
            }
        }

        timer = 0;
    }

    private boolean isBanner(Block block) {
        return block instanceof BannerBlock;
    }
}
