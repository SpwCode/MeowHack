package meowhack.modules;

import meowhack.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BlockPosSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.BlockPos;

public class AreaScan extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<BlockPos> firstPoint = sgGeneral.add(new BlockPosSetting.Builder()
    .name("First point")
    .description("Vector point 1")
    .build()
    );

    private final Setting<Integer> interval = sgGeneral.add(new IntSetting.Builder()
    .name("interval")
    .description("Interval between points")
    .defaultValue(120)
    .min(1)
    .noSlider()
    .build()
    );

    private final Setting<Integer> multiplier = sgGeneral.add(new IntSetting.Builder()
    .name("multiplier")
    .description("")
    .defaultValue(1)
    .min(1)
    .noSlider()
    .build()
    );

    private final Setting<Integer> direction = sgGeneral.add(new IntSetting.Builder()
    .name("direction")
    .description("")
    .defaultValue(0)
    .max(4)
    .noSlider()
    .build()
    );

    public AreaScan() {
        super(AddonTemplate.CATEGORY, "Area-Scan", "Spiral movement");
    }

    float yaw = 180;
    int step, addCoord = 0;

    @Override
    public void onDeactivate() {
        unpress();
    }

    @EventHandler
    private void onTick(TickEvent.Post e) {
        if (mc.player == null || mc.world == null) return;
        if ((mc.player.getBlockPos().isWithinDistance(new BlockPos(firstPoint.get().getX(), mc.player.getBlockY(), firstPoint.get().getZ()), 2))) {

            updatePoints();
            return;
        }
        BlockPos blockPos = new BlockPos(firstPoint.get()); // Замените значениями координат
        double playerX = mc.player.getX();
        double playerZ = mc.player.getZ();

        double deltaX = blockPos.getX() + 0.5 - playerX;
        double deltaZ = blockPos.getZ() + 0.5 - playerZ;

        float yaw = (float) (Math.atan2(deltaZ, deltaX) * (180.0 / Math.PI)) - 90.0F; // Расчет yaw

        mc.player.setYaw(yaw); // Установка yaw игрока
        mc.options.forwardKey.setPressed(true);


    }

    public void updatePoints() {
        if (direction.get() >= 4) {
            direction.set(0);
            }
        if (direction.get() == 0) {
            firstPoint.set(new BlockPos((int)firstPoint.get().getX(), (int)mc.player.getY(), (int)firstPoint.get().getZ() - interval.get()*multiplier.get()));
            direction.set(direction.get()+1);
            return;
        }
        if (direction.get() == 1) {
            firstPoint.set(new BlockPos((int)firstPoint.get().getX() + interval.get()*multiplier.get(), (int)mc.player.getY(), (int)firstPoint.get().getZ()));
            multiplier.set(multiplier.get()+1);
            direction.set(direction.get()+1);
            return;
        }
        if (direction.get() == 2) {
            firstPoint.set(new BlockPos((int)firstPoint.get().getX(), (int)mc.player.getY(), (int)firstPoint.get().getZ() + interval.get()*multiplier.get()));
            direction.set(direction.get()+1);
            return;
        }
        if (direction.get() == 3) {
            firstPoint.set(new BlockPos((int)firstPoint.get().getX() - interval.get()*multiplier.get(), (int)mc.player.getY(), (int)firstPoint.get().getZ()));
            multiplier.set(multiplier.get()+1);
            direction.set(direction.get()+1);
            return;
        }

    }




    private void unpress() {
        mc.options.forwardKey.setPressed(false);
    }
}
