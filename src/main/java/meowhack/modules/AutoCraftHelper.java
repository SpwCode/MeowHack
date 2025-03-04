package meowhack.modules;

import meowhack.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.item.Item;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;


import java.util.Arrays;
import java.util.List;

public class AutoCraftHelper extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> AutoShutdown = sgGeneral.add(new BoolSetting.Builder()
    .name("auto-shutdown")
    .description("Turn off the module if no stalkers found near player")
    .defaultValue(true)
    .build()
    );

    private final Setting<List<Item>> itemsForSteal = sgGeneral.add(new ItemListSetting.Builder()
        .name("items-for-steal")
        .description("Items you want to get crafted.")
        .defaultValue(Arrays.asList())
        .build()
    );

    private final Setting<List<Item>> itemsForDump = sgGeneral.add(new ItemListSetting.Builder()
        .name("items-for-dump")
        .description("Items result.")
        .defaultValue(Arrays.asList())
        .build()
    );

    private final Setting<List<Block>> shulkersToPitIn = sgGeneral.add(new BlockListSetting.Builder()
        .name("colored-shulkers")
        .description("Color of a shulker for craft result")
        .defaultValue(Blocks.BLACK_SHULKER_BOX)
        .filter(this::filterBlocks)
        .build()
    );


    private final Setting<Integer> scanDelay = sgGeneral.add(new IntSetting.Builder()
        .name("scan-delay ")
        .description("Delay between interactions.")
        .defaultValue(50)
        .range(1, 200)
        .sliderMax(200)
        .build()
    );

    public AutoCraftHelper() {
        super(AddonTemplate.CATEGORY, "auto-craft-helper", "Helps you to automatically crafts items.");
    }

    int wait;
    int currentMethodIndex = 0;
    private Runnable[] methods = { this::findShulkerToClick, this::searchAndMoveItems, this::findWorkbenchToClick, this::findColoredShulkerToClick, this::searchAndMoveItems };

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (wait > 0) {
            wait--;
            return;
          }
          executeNextMethod();

        wait = scanDelay.get();
    }


    private void executeNextMethod() {
        methods[currentMethodIndex % methods.length].run();
        currentMethodIndex++;
    }

    private void findWorkbenchToClick() {
        findBlockToClick(Blocks.CRAFTING_TABLE);
    }

    private void findShulkerToClick() {
        findBlockToClick(Blocks.SHULKER_BOX);
    }

    private void findColoredShulkerToClick() {
        findBlockToClick(shulkersToPitIn.get().get(0));
    }

    private void findBlockToClick(Block blockToClick) {
        int px = mc.player.getBlockPos().getX();
        int py = mc.player.getBlockPos().getY();
        int pz = mc.player.getBlockPos().getZ();
        boolean blockFound = false;

        for (int x = px - 4; x <= px + 4; x++) {
            for (int z = pz - 4; z <= pz + 4; z++) {
                for (int y = py - 4; y <= py + 5; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = mc.world.getBlockState(pos);
                    if (state.getBlock() == blockToClick) {
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Utils.vec3d(pos), Direction.UP, pos, false));
                        blockFound = true;
                    }
                }
            }
        }
        if (!blockFound && AutoShutdown.get()) {
            error("Блок " + blockToClick.getName().getString() + " не найден в радиусе 4 блоков от игрока.");
            this.toggle();
        }
    }

    private void searchAndMoveItems() {
        if (!(mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler)) return;

        boolean itemsMoved = false; // флаг для прекращения выполнения цикла после перемещения предметов

        for (int i = 0; i < 27; i++) { // перебираем только слоты до 27 включительно
            if (!mc.player.currentScreenHandler.getSlot(i).hasStack()) continue;
            Item item = ((ShulkerBoxScreenHandler) mc.player.currentScreenHandler).slots.get(i).getStack().getItem();

            if (mc.currentScreen == null) break;

            if (itemsForSteal.get().contains(item)) {
                InvUtils.shiftClick().slotId(i);
                itemsMoved = true; // устанавливаем флаг после перемещения предметов
            }
        }

        if (itemsMoved) return; // прекращаем выполнение цикла после перемещения предметов

        for (int i = 27; i < 63; i++) {
            if (!mc.player.currentScreenHandler.getSlot(i).hasStack()) continue;
            Item item = ((ShulkerBoxScreenHandler) mc.player.currentScreenHandler).slots.get(i).getStack().getItem();

            if (mc.currentScreen == null) break;

            if (itemsForDump.get().contains(item)) {
                InvUtils.shiftClick().slotId(i);
            }
        }
    }

    private boolean filterBlocks(Block block) {
        return isShulker(block);
    }

    private boolean isShulker(Block block) {
        return block instanceof ShulkerBoxBlock shulkerBlock;
    }


}
