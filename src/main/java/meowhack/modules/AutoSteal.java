package meowhack.modules;

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
                                processItems(false); // Dump

                            }
                        }
                        if (triggerBlockState.getBlock() == Blocks.DISPENSER && state.getBlock() == Blocks.SHULKER_BOX) {
                            if (mc.currentScreen == null) {
                                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Utils.vec3d(pos), Direction.UP, pos, false));
                            }
                            if (mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler) {
                                processItems(true); // Steal
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

    private void processItems(boolean isSteal) {
        if (clickDelay > 0) {
            clickDelay--;
            return; // Ждём завершения задержки
        }

        // Определяем параметры в зависимости от режима (красть или сбрасывать)
        List<Item> targetItems = isSteal ? itemsForSteal.get() : itemsForDump.get();
        int fromSlot = isSteal ? 0 : 27;
        int toSlot = isSteal ? 26 : 62;
        int emptySlot = getEmptySlot(isSteal ? 27 : 0, isSteal ? 54 : 27); // Ищем пустой слот заранее

        if (emptySlot == -1) return; // Нет свободного места

        for (int i = fromSlot; i <= toSlot; i++) {
            if (mc.player.currentScreenHandler.getSlot(i).getStack().isEmpty()) continue;

            Item item = mc.player.currentScreenHandler.getSlot(i).getStack().getItem();

            if (targetItems.contains(item)) {
                InvUtils.move().fromId(i).toId(emptySlot);
               //info((isSteal ? "Steal" : "Dump") + " Двигаем слот " + i + " в слот " + emptySlot);
                clickDelay = delayBetweenClicks.get();
                return; // Двигаем только один предмет за раз
            }
        }
    }
}
