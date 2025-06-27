package meowhack.modules;

import meowhack.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.pathing.BaritoneUtils;
import meteordevelopment.meteorclient.settings.BlockPosSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.Dimension;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.BlockPos;

public class AreaScanNether extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> isWithinTargetRange = sgGeneral.add(new IntSetting.Builder()
        .name("isWithinTargetRange")
        .description("расстояние до координаты, в блоках, после которой заданная точка будет считаться достигнутой")
        .defaultValue(64)
        .min(1)
        .noSlider()
        .build()
    );

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

    public AreaScanNether() {
        super(AddonTemplate.CATEGORY, "Area-Scan-Nether", "Spiral movement");
    }

    @Override
    public void onActivate() {
        setElytraGoal();
    }

    @Override
    public void onDeactivate() {
        ChatUtils.sendPlayerMsg(BaritoneUtils.getPrefix() + "stop");
    }

    @EventHandler
    private void onTick(TickEvent.Post e) {
        if (mc.player == null || mc.world == null) return;
        if ((mc.player.getBlockPos().isWithinDistance(new BlockPos(firstPoint.get().getX(), mc.player.getBlockY(), firstPoint.get().getZ()), isWithinTargetRange.get()))) {
            updatePoints();
            setElytraGoal();
            return;
        }
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




    private void setElytraGoal() {
        if (PlayerUtils.getDimension() == Dimension.Nether) {
            ChatUtils.sendPlayerMsg(BaritoneUtils.getPrefix() + "stop");
            ChatUtils.sendPlayerMsg(BaritoneUtils.getPrefix() + "goal " + " " + (int)firstPoint.get().getX() +" " + (int)mc.player.getY()  +" " +  (int)firstPoint.get().getZ());
            ChatUtils.sendPlayerMsg(BaritoneUtils.getPrefix() + "elytra");
        }
    }
}
