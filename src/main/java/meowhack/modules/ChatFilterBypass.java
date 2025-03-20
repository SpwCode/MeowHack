package meowhack.modules;

import meowhack.AddonTemplate;
import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class ChatFilterBypass extends Module {
    public ChatFilterBypass() {
        super(AddonTemplate.CATEGORY, "Chat Filter Bypass", "Bypasses Chat Filters");
    }
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    private final Setting<Modes> mode = sgGeneral.add(new EnumSetting.Builder<Modes>()
        .name("mode")
        .description("modes")
        .defaultValue(Modes.zeroSpace)
        .build()
    );

    @EventHandler
    private void OnMessage(SendMessageEvent event){
        String message = event.message;
        switch (mode.get()) {
            case zeroSpace -> {
                message = message.replaceAll("а", "a\u200C")
                    .replaceAll("е", "e\u200C")
                    .replaceAll("у", "y\u200C")
                    .replaceAll("o", "o\u200C")
                    .replaceAll("с", "c\u200C")
                    .replaceAll("х", "x\u200C")
                    .replaceAll("и", "и\u200C")
                    .replaceAll("з", "з\u200C")
                    .replaceAll("й", "й\u200C")
                    .replaceAll("к", "к\u200C");
            }

            case RTLO -> {
                message = message.replaceAll("<", ">").replaceAll(">", "<");
                message = new StringBuilder(message).reverse().toString();
                message = "\u202E" + message;

            }
        }
        event.message = message;
    }

    public enum Modes{
        zeroSpace,
        RTLO
    }
}
