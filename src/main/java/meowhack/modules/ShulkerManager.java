package meowhack.modules;

import meowhack.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.screen.ShulkerBoxScreenHandler;

import java.util.Arrays;
import java.util.List;


public class ShulkerManager extends Module {

    public enum ShulkerMode {
        Наполнение_шалкера,
        Выгрузка_из_шалкера
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<ShulkerMode> mode = sgGeneral.add(new EnumSetting.Builder<ShulkerMode>()
        .name("mode")
        .description("Choose whether to load or unload items")
        .defaultValue(ShulkerMode.Наполнение_шалкера)
        .build()
    );

    private final Setting<List<Item>> itemsList = sgGeneral.add(new ItemListSetting.Builder()
        .name("items")
        .description("Items to move")
        .defaultValue(Arrays.asList())
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay between interactions.")
        .defaultValue(50)
        .range(1, 200)
        .sliderMax(200)
        .build()
    );


    public ShulkerManager() {
        super(AddonTemplate.CATEGORY, "shulker-manager", "Loads or unloads shulker boxes with control.");
    }

    int wait;
    int currentMethodIndex = 0;
    private Runnable[] methods = { this::findShulkerToClick, this::setupSteps };


    @Override
    public void onActivate() {
        wait = 0;
        currentMethodIndex = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (wait > 0) {
            wait--;
            return;
          }

          executeNextMethod();

        wait = delay.get();
    }


    private void executeNextMethod() {
        methods[currentMethodIndex % methods.length].run();
        currentMethodIndex++;
    }

    private void findShulkerToClick() {
        if (itemsList.get().isEmpty()) return;

        for (Item item : itemsList.get()) {
            FindItemResult result = InvUtils.find(item);
            boolean condition = (mode.get() == ShulkerMode.Наполнение_шалкера && result.found()) ||
                (mode.get() == ShulkerMode.Выгрузка_из_шалкера && !result.found());

            if (condition) {
                Modules.get().get(AutoCraftHelper.class).findBlockToClick(Blocks.SHULKER_BOX);
            }
        }
    }

    private void setupSteps() {
            if (mode.get() == ShulkerMode.Выгрузка_из_шалкера) {
                searchAndMoveItems(0, 26); // from container to inventory
            } else if (mode.get() == ShulkerMode.Наполнение_шалкера) {
                searchAndMoveItems(27, 62); // from inventory to container
            }
    }

    private void searchAndMoveItems(int startSlot, int endSlot) {
        if (!(mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler)) return;

        for (int i = startSlot; i <= endSlot; i++) { // перебираем только слоты до 27 включительно
            if (!mc.player.currentScreenHandler.getSlot(i).hasStack()) continue;
            Item item = ((ShulkerBoxScreenHandler) mc.player.currentScreenHandler).slots.get(i).getStack().getItem();

            if (mc.currentScreen == null) break;

            if (itemsList.get().contains(item)) {
                InvUtils.shiftClick().slotId(i);
            }
        }
    }
}
