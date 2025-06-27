package meowhack.modules;

import meowhack.AddonTemplate;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.Block;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.text.Text;

import java.util.List;

public class Disconnect extends Module {
    public Disconnect() {
        super(AddonTemplate.CATEGORY, "Disconnect", "Модуль ответа на текстовые команды");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<String> disconnectMessage = sgGeneral.add(new StringSetting.Builder()
        .name("disconnect-Message")
        .description("Сообщение для дисконекта")
        .defaultValue("80vh7l5u5rpn")
        .build()
    );

    private final Setting<String> interactMessage = sgGeneral.add(new StringSetting.Builder()
        .name("interact-Message")
        .description("Сообщение для заимодействия с блоком")
        .defaultValue("q9wuthwkds")
        .build()
    );


    private final Setting<List<Block>> whitelist = sgGeneral.add(new BlockListSetting.Builder()
        .name("whitelist")
        .description("С каким блоком взаимодействовать")
        .description("The blocks you want to click.")
        .build()
    );



    @EventHandler(priority = EventPriority.HIGHEST)
    private void onMessageRecieve(ReceiveMessageEvent event) {
        if (mc.world == null || mc.player == null) return;
        String msg = event.getMessage().getString();
        if (msg.contains(disconnectMessage.get())) {
            mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(Text.literal("Твой друг попросил тебя выйти с сервера.")));
        }
        if (msg.contains(interactMessage.get())) {
            findBlocksFromWhitelist();
        }
    }



    public void findBlocksFromWhitelist() {
        // Получаем список блоков из whitelist
        List<Block> blocksToClick = whitelist.get();

        // Если список пуст — выводим ошибку
        if (blocksToClick.isEmpty()) {
            info("Вайтлист блоков пуст! Добавьте блоки в настройках.");
            return;
        }

        for (Block block : blocksToClick) {
            Modules.get().get(AutoCraftHelper.class).findBlockToClick(block);
        }
    }
}
