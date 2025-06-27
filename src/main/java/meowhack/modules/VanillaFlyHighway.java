package meowhack.modules;

import meowhack.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import org.lwjgl.glfw.GLFW;

public class VanillaFlyHighway extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Double> mainHeight = sgGeneral.add(new DoubleSetting.Builder()
        .name("fly-height")
        .description("The upper height boundary")
        .defaultValue(123)
        .min(0)
        .sliderMax(320)
        .build()
    );

    public final Setting<Double> fireworkHeight = sgGeneral.add(new DoubleSetting.Builder()
        .name("firework-activate-height")
        .description("Высота отскока феерверком")
        .defaultValue(121)
        .min(0)
        .sliderMax(320)
        .build()
    );

    private final Setting<Boolean> debug  = sgGeneral.add(new BoolSetting.Builder()
        .name("debug-config")
        .description("Не меняйте если не знаете что это.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> changePerspective  = sgGeneral.add(new BoolSetting.Builder()
        .name("change-perspective")
        .description("Меняет перспективу при включении.")
        .visible(debug::get)
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> freeLook  = sgGeneral.add(new BoolSetting.Builder()
        .name("free-look")
        .description("Тест фича")
        .visible(debug::get)
        .defaultValue(true)
        .build()
    );

    public final Setting<Double> minTickBetweenFireworks = sgGeneral.add(new DoubleSetting.Builder()
        .name("ticks")
        .description("Минимальное количество тиков перед повторным использование феерверка.")
        .visible(debug::get)
        .defaultValue(30)
        .sliderRange(0, 120)
        .build()
    );

    private final Setting<Double> horizontalDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("horizontal-distance")
        .visible(debug::get)
        .defaultValue(30)
        .sliderRange(1, 1000)
        .build()
    );

    private final Setting<Integer> fireworkSlot = sgGeneral.add(new IntSetting.Builder()
        .name("firework-slot")
        .visible(debug::get)
        .description("How far away from the player you can place a block (in whole blocks).")
        .defaultValue(8)
        .min(0)
        .sliderMax(8)
        .build()
    );

    private double ticksLeft;
    private Perspective prePers;
    public float cameraPitch;

    public VanillaFlyHighway() {
        super(AddonTemplate.CATEGORY, "Vanilla-Fly-Highway", "Cursed go brrrr..");
    }

    @Override
    public void onActivate() {
        if (changePerspective.get()) {
            prePers = mc.options.getPerspective();
            mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        }
    }

    @Override
    public void onDeactivate() {
        if (changePerspective.get()) mc.options.setPerspective(prePers);
        unpress();
    }

    @EventHandler
    private void onTick(TickEvent.Post e) {
        if (mc.player == null || mc.world == null) return;
        pressFire();
        lookAtHeight(mainHeight.get());
        mc.options.forwardKey.setPressed(true);
    }

    public void lookAtHeight(double targetY) {
        double playerY = mc.player.getY(); // Текущая высота игрока (глаза)
        double deltaY = targetY - playerY;    // Разница по вертикали
        float pitch = (float) -Math.toDegrees(Math.atan2(deltaY, horizontalDistance.get()));        // Рассчитываем pitch
        pitch = MathHelper.clamp(pitch, -90.0F, 90.0F);        // Ограничиваем угол (-90° до +90°)
        mc.player.setPitch(pitch);        // Устанавливаем игроку
    }

    public void pressFire() {
        if (ticksLeft <= 0) {
            if (mc.player.getY() <= fireworkHeight.get()) {
                ticksLeft = minTickBetweenFireworks.get();
                Modules.get().get(RocketRight.class).useFireworkFromInventory(fireworkSlot.get());
            }
        }
        ticksLeft--;
    }

    private void unpress() {
        mc.options.forwardKey.setPressed(false);
    }
}
