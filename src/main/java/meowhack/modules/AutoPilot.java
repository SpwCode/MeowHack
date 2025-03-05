package meowhack.modules;

import meowhack.AddonTemplate;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BlockPosSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;

import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class AutoPilot extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<BlockPos> coords = sgGeneral.add(new BlockPosSetting.Builder()
        .name("Coords")
        .description("Center of the sphere")
        .build()
    );

    private final Setting<Boolean> disconnect = sgGeneral.add(new BoolSetting.Builder()
        .name("disconnect")
        .description("Disconnect.")
        .defaultValue(true)
        .build()
    );

    public AutoPilot() {
        super(AddonTemplate.CATEGORY, "Auto-Pilot", "Cursed go brrrr..");
    }

    float yaw = 180;

    @Override
    public void onDeactivate() {
        unpress();
    }

    @EventHandler
    private void onTick(TickEvent.Post e) {
        if (mc.player == null || mc.world == null) return;
        if ((mc.player.getBlockPos().isWithinDistance(new BlockPos(coords.get().getX(), mc.player.getBlockY(), coords.get().getZ()), 2))) {
            if (disconnect.get()) {
                mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(Text.literal("Координаты игрока достигли заданых")));
                this.toggle();
            } else {
                unpress();
                toggle();
                return;
            }
        }

        BlockPos blockPos = new BlockPos(coords.get()); // Замените значениями координат
        double playerX = mc.player.getX();
        double playerZ = mc.player.getZ();

        double deltaX = blockPos.getX() + 0.5 - playerX;
        double deltaZ = blockPos.getZ() + 0.5 - playerZ;

        float yaw = (float) (Math.atan2(deltaZ, deltaX) * (180.0 / Math.PI)) - 90.0F; // Расчет yaw

        mc.player.setYaw(yaw); // Установка yaw игрока
        mc.options.forwardKey.setPressed(true);

    }


    private void unpress() {
        mc.options.forwardKey.setPressed(false);
    }
}
