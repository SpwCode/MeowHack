package meowhack.modules;

import meowhack.AddonTemplate;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.HandledScreen;


public class QuickStash extends Module {
    public QuickStash() {
        super(AddonTemplate.CATEGORY, "Quick Stash", "Быстро перемещает предметы между инвентарём и сундуками.");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgDump = settings.createGroup("Складываем");
    private final Setting<Integer> secretNumberX = sgGeneral.add(new IntSetting.Builder()
        .name("Цифра-вора")
        .description("На какую цифру начиются координаты при воровстве")
        .defaultValue(8)
        .min(0)
        .sliderMax(8)
        .build()
    );
    private final Setting<Integer> secretNumberX2 = sgDump.add(new IntSetting.Builder()
        .name("цифра-добра")
        .description("На какую цифру начиются координаты при выкладвании")
        .defaultValue(8)
        .min(0)
        .sliderMax(8)
        .build()
    );

    private int lastContainerHash = 0;

    @EventHandler
    private void onScreenClose(OpenScreenEvent event) {
        lastContainerHash = 0;
    }

     @EventHandler
     private void onTick(TickEvent.Post event) {
         if (mc.currentScreen instanceof HandledScreen<?> handledScreen) {
             int currentHash = handledScreen.getTitle().getString().hashCode();  // Создаем "уникальный идентификатор" контейнера (на основе его заголовка)
             // Если контейнер уже обрабатывался — пропускаем
             if (currentHash == lastContainerHash) return;
             // Запоминаем новый контейнер
             lastContainerHash = currentHash;

             String title = handledScreen.getTitle().getString().toLowerCase();
             if (doesXStartWithNumber(String.valueOf(secretNumberX.get()))) {
                 info("true1");
                 if (title.contains("большой")) {
                     for (int i = 0; i < 54; i++) { // перебираем  слоты
                         if (mc.currentScreen == null) break;
                         if (!mc.player.currentScreenHandler.getSlot(i).hasStack()) continue;
                         InvUtils.shiftClick().slotId(i);
                     }
                 } else if (title.contains("эндер")) {
                     for (int i = 27; i < 54; i++) { // перебираем  слоты
                         if (mc.currentScreen == null) break;
                         if (!mc.player.currentScreenHandler.getSlot(i).hasStack()) continue;
                         InvUtils.shiftClick().slotId(i);
                     }
                 }
             }
             if (doesXStartWithNumber(String.valueOf(secretNumberX2.get()))) {
                 info("true2");
                 if (title.contains("большой")) {
                     for (int i = 54; i < 81; i++) { // перебираем  слоты
                         if (mc.currentScreen == null) break;
                         if (!mc.player.currentScreenHandler.getSlot(i).hasStack()) continue;
                         InvUtils.shiftClick().slotId(i);
                     }
                 } else if (title.contains("эндер")) {
                     for (int i = 0; i < 27; i++) { // перебираем  слоты
                         if (mc.currentScreen == null) break;
                         if (!mc.player.currentScreenHandler.getSlot(i).hasStack()) continue;
                         InvUtils.shiftClick().slotId(i);
                     }
                 }
             }
         }

     }

    public boolean doesXStartWithNumber(String secretNumber) {
        double x = Math.abs(mc.player.getX());
        String xStr = String.valueOf(x);
        char cStr = xStr.charAt(0);
        return !xStr.isEmpty() && secretNumber.equals(String.valueOf(cStr));
    }
}
