package meowhack.modules;

import fi.dy.masa.itemscroller.util.InventoryUtils;
import meowhack.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.Rotation;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Item;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;


public class AutoSteal extends Module {

    public AutoSteal() {
        super(AddonTemplate.CATEGORY, "Auto-Steal", "Вор какашек");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> turnRotation = sgGeneral.add(new BoolSetting.Builder()
        .name("turn-rotation-module")
        .description("Turn on rotation module on activate")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> maxMovedItems = sgGeneral.add(new IntSetting.Builder()
        .name("number-of-moved-items ")
        .description("Delay between interactions.")
        .defaultValue(6)
        .range(1, 30)
        .sliderMax(30)
        .build()
    );

    private final Setting<Integer> delayBetweenClicks = sgGeneral.add(new IntSetting.Builder()
        .name("delay-between-clicks")
        .description("Delay in ticks between item moves.")
        .defaultValue(1) // Задержка в тиках (1 тик = 50 мс)
        .range(1, 20)
        .sliderMax(20)
        .build()
    );

    private final Setting<List<Item>> itemsForSteal = sgGeneral.add(new ItemListSetting.Builder()
        .name("items-for-steal")
        .description("Items you want to get crafted.")
        .build()
    );

    private final Setting<List<Item>> itemsForDump = sgGeneral.add(new ItemListSetting.Builder()
        .name("items-for-dump")
        .description("Items result.")
        .build()
    );
    private int clickDelay = 0; // Таймер для задержки

    @Override
    public void onActivate() {
        if (turnRotation.get() && !Modules.get().isActive(Rotation.class)) Modules.get().get(Rotation.class).toggle();
    }

    @Override
    public void onDeactivate() {
        if (turnRotation.get() && Modules.get().isActive(Rotation.class)) Modules.get().get(Rotation.class).toggle();
    }


    @EventHandler
    private void onTick(TickEvent.Post event) {


            int px = mc.player.getBlockPos().getX();
            int py = mc.player.getBlockPos().getY();
            int pz = mc.player.getBlockPos().getZ();


            for (int x = px - 3; x <= px + 3; x++) {
                for (int z = pz - 3; z <= pz + 3; z++) {
                    for (int y = py - 1; y <= py + 2; y++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState state = mc.world.getBlockState(pos);
                        BlockState triggerBlockState = mc.world.getBlockState(new BlockPos(x, y, z + 1));
                        if (triggerBlockState.getBlock() == Blocks.COMPARATOR && state.getBlock() == Blocks.SHULKER_BOX) {
                            if (mc.currentScreen == null) {
                                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Utils.vec3d(pos), Direction.UP, pos, false));
                            }
                            if (mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler) {
                                dumpItems(); // Dump

                            }
                        }
                        if (triggerBlockState.getBlock() == Blocks.DISPENSER && state.getBlock() == Blocks.SHULKER_BOX) {
                            if (mc.currentScreen == null) {
                                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Utils.vec3d(pos), Direction.UP, pos, false));
                            }
                            if (mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler) {
                                stealItems(); // Steal
                            }
                        }
                    }
                }
            }
        }

    private int getEmptySlot(int fromSlot, int toSlot) {
        for (int i = fromSlot; i < toSlot; i++) {
            if ((mc.player.currentScreenHandler).slots.get(i).getStack().isEmpty()) return i;
        }
        return -1;
    }

    private void stealItems() {
        if (clickDelay > 0) {
            clickDelay--;
            return;
        }

        List<Item> targetItems = itemsForSteal.get();
        int fromSlot = 0;
        int toSlot = 26;
        int emptySlot = getEmptySlot(27, 54); // Проверяем пустой слот в инвентаре

        if (emptySlot == -1) return; // Нет свободного места

        for (int i = fromSlot; i <= toSlot; i++) {
            if (mc.player.currentScreenHandler.getSlot(i).getStack().isEmpty()) continue;

            Item item = mc.player.currentScreenHandler.getSlot(i).getStack().getItem();

            if (targetItems.contains(item)) {
                InvUtils.move().fromId(i).toId(emptySlot);
                clickDelay = delayBetweenClicks.get();
                return; // Перемещаем один предмет за раз
            }
        }
    }

    private void dumpItems() {
        if (clickDelay > 0) {
            clickDelay--;
            return;
        }

        List<Item> targetItems = itemsForDump.get();
        int fromSlot = 27;
        int toSlot = 62;

        for (int i = fromSlot; i <= toSlot; i++) {
            if (mc.player.currentScreenHandler.getSlot(i).getStack().isEmpty()) continue;

            Item item = mc.player.currentScreenHandler.getSlot(i).getStack().getItem().asItem();

            if (targetItems.contains(item)) {
                if (!(mc.currentScreen instanceof HandledScreen<?>)) return;
                HandledScreen<?> screen = (HandledScreen<?>) mc.currentScreen; // Преобразование

                InventoryUtils.shiftClickSlot(screen,i);
                clickDelay = delayBetweenClicks.get();
                return; // Перемещаем один предмет за раз
            }
        }
    }
}
