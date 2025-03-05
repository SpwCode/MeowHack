package meowhack.modules;

import meowhack.AddonTemplate;
import meteordevelopment.orbit.EventHandler;

import java.util.List;

import meteordevelopment.meteorclient.events.packets.InventoryEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.InventoryTweaks;
import meteordevelopment.meteorclient.systems.modules.player.Rotation;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.Generic3x3ContainerScreenHandler;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;


public class AutoSteal extends Module {

    public AutoSteal() {
        super(AddonTemplate.CATEGORY, "AutoSteal", "");
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

    @Override
    public void onActivate() {
        if (turnRotation.get() && !Modules.get().isActive(Rotation.class)) Modules.get().get(Rotation.class).toggle();
    }

    @Override
    public void onDeactivate() {
      if (turnRotation.get() && Modules.get().isActive(Rotation.class)) Modules.get().get(Rotation.class).toggle();
    }

    Boolean allowSteal = false;
    Boolean doneOnce = false;
    public int movedItems = 0; // переменная для подсчета перемещенных предметов
    int wait = 0;
    private int timer = 0;

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.currentScreen == null) {
            doneOnce = false;

            int px = mc.player.getBlockPos().getX();
            int py = mc.player.getBlockPos().getY();
            int pz = mc.player.getBlockPos().getZ();


            for (int x = px - 4; x <= px + 4; x++) {
                for (int z = pz - 4; z <= pz + 4; z++) {
                    for (int y = py - 4; y <= py + 5; y++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState state = mc.world.getBlockState(pos);
                        BlockState triggerBlockState = mc.world.getBlockState(new BlockPos(x, y, z+1));
                        if(triggerBlockState.getBlock() == Blocks.DISPENSER && state.getBlock() == Blocks.SHULKER_BOX) {
                            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Utils.vec3d(pos), Direction.UP, pos, false));
                            allowSteal = false;
                        }
                        if(triggerBlockState.getBlock() == Blocks.PISTON && state.getBlock() == Blocks.SHULKER_BOX) {
                            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Utils.vec3d(pos), Direction.UP, pos, false));
                            allowSteal = true;
                        }
                    }
                }
            }
        }
        if (mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler) {
            timer++;
        }
        if (timer >= 20) {
           // info("getTtems return to openshulker");
            if (!doneOnce) searchAndMoveItems();
            timer = 0;
            return;
        }

    }


    private void searchAndMoveItems() {
        if (!(mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler)) return;

        boolean itemsMoved = false; // флаг для прекращения выполнения цикла после перемещения предметов
        doneOnce = true;


        for (int i = 0; i <= 26; i++) { // перебираем только слоты до 27 включительно
            if (!mc.player.currentScreenHandler.getSlot(i).hasStack()) continue;
            Item item = ((ShulkerBoxScreenHandler) mc.player.currentScreenHandler).slots.get(i).getStack().getItem();

            if (mc.currentScreen == null) break;

            if (movedItems >= maxMovedItems.get()) {
                movedItems = 0;
               // error("Переменная сбросилась");
                break; // выходим из цикла после перемещения 6 предметов
            }

           // error(String.valueOf(movedItems));

            if (itemsForSteal.get().contains(item) && allowSteal) {
                InvUtils.shiftClick().slotId(i);
               // error("Переместили предмет +1");
                itemsMoved = true; // устанавливаем флаг после перемещения предметов
                movedItems++;
            }
        }

        if (itemsMoved) return; // прекращаем выполнение цикла после перемещения предметов

        for (int i = 27; i < 63; i++) {
            if (!mc.player.currentScreenHandler.getSlot(i).hasStack()) continue;
            Item item = ((ShulkerBoxScreenHandler) mc.player.currentScreenHandler).slots.get(i).getStack().getItem();

            if (mc.currentScreen == null) break;

            if (itemsForDump.get().contains(item) && !allowSteal) {
                InvUtils.shiftClick().slotId(i);
            }
        }
    }
}
