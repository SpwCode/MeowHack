package meowhack.modules;

import meowhack.AddonTemplate;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;


public class BaritoneHelper extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<String> entryMessage = sgGeneral.add(new StringSetting.Builder()
        .name("entryMessage")
        .description("Какого сообщения ждать")
        .defaultValue("[Baritone] Done building")
        .build()
    );
    private final Setting<String> command = sgGeneral.add(new StringSetting.Builder()
        .name("command")
        .description("Command to execute.")
        .defaultValue("")
        .build());

    private final Setting<String> secondCommand = sgGeneral.add(new StringSetting.Builder()
        .name("second-Command")
        .description("Second command to execute.")
        .defaultValue("")
        .build());

    private final Setting<String> thirdCommand = sgGeneral.add(new StringSetting.Builder()
        .name("third-Command")
        .description("Third command to execute.")
        .defaultValue("")
        .build());

    private final Setting<Integer> errorCommandDelay = sgGeneral.add(new IntSetting.Builder()
        .name("error-command-delay")
        .description("Max speed.")
        .defaultValue(200)
        .sliderMin(0).sliderMax(400)
        .build()
    );

    private final Setting<String> errorCommand1 = sgGeneral.add(new StringSetting.Builder()
        .name("error-Command-1")
        .description("Second command to execute.")
        .defaultValue("")
        .build());

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private int tickCounter = 0;
    private boolean scheduleSecondCommand = false;

    private Vec3d lastPosition = null;
    private int positionCheckCounter = 0;

    public BaritoneHelper() {
        super(AddonTemplate.CATEGORY, "Baritone-helper", "Automatically execute specific commands.");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onMessageRecieve(ReceiveMessageEvent event) {
        if (mc.world == null || mc.player == null) return;
        String msg = event.getMessage().getString();
        if (msg.startsWith(">")) return; // Ignore chat messages

        if (msg.contains(entryMessage.get())) {
            ChatUtils.sendPlayerMsg(command.get());
            scheduleSecondCommand = true;
            tickCounter = 0;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (scheduleSecondCommand) {
            tickCounter++;
            if (tickCounter >= 10) { // 60 тиков = примерно 3 секунды
                if  (!secondCommand.get().equals("")) ChatUtils.sendPlayerMsg(secondCommand.get());
                if  (!thirdCommand.get().equals("")) ChatUtils.sendPlayerMsg(thirdCommand.get());
                scheduleSecondCommand = false;
            }
        }

        positionCheckCounter++;
        if (positionCheckCounter >= errorCommandDelay.get()) { // 1200 тиков = примерно 1 минута
            positionCheckCounter = 0;
            Vec3d currentPosition = mc.player.getPos();
            if (lastPosition != null && currentPosition.equals(lastPosition) && !errorCommand1.get().equals("")) {
                ChatUtils.sendPlayerMsg(errorCommand1.get());
            }
            lastPosition = currentPosition;
        }
    }
}
