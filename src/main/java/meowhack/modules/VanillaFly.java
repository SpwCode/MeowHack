package meowhack.modules;

import meowhack.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.FreeLook;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class VanillaFly extends Module {
    private boolean pitchingDown = true;
    private Double pitch;

    public VanillaFly() {
      super(AddonTemplate.CATEGORY, "vanilla-fly", "Vanilla Elytra Fly.");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Boolean> replace = sgGeneral.add(new BoolSetting.Builder()
        .name("elytra-replace")
        .description("Replaces broken elytra with a new elytra.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Integer> replaceDurability = sgGeneral.add(new IntSetting.Builder()
        .name("replace-durability")
        .description("The durability threshold your elytra will be replaced at.")
        .defaultValue(2)
        .range(1, 100)
        .sliderRange(1, 100)
        .visible(replace::get)
        .build()
    );


    private final Setting<Boolean> freeLook  = sgGeneral.add(new BoolSetting.Builder()
    .name("free-look-toggle")
    .description("Activate FreeLook Module")
    .defaultValue(true)
    .build()
    );

    private final Setting<Boolean> useFireworks  = sgGeneral.add(new BoolSetting.Builder()
    .name("use-fireworks")
    .description("Activate")
    .defaultValue(true)
    .build()
    );

    private final Setting<Double> velocitySpeed = sgGeneral.add(new DoubleSetting.Builder()
    .name("Falling-speed")
    .description("Falling speed.")
    .visible(useFireworks::get)
    .defaultValue(-0.170)
    .sliderRange(-0.2, 0)
    .build()
    );


    public final Setting<Double> pitch40upperBounds = sgGeneral.add(new DoubleSetting.Builder()
        .name("pitch-upper-bounds")
        .description("The upper height boundary for pitch40.")
        .defaultValue(120)
        .min(0)
        .sliderMax(260)
        .build()
    );

    public final Setting<Double> pitch40lowerBounds = sgGeneral.add(new DoubleSetting.Builder()
    .name("pitch-lower-bounds")
    .description("The bottom height boundary for pitch40.")
    .defaultValue(80)
    .min(0)
    .sliderMax(260)
    .build()
    );

    public final Setting<Double> pitchRotationSpeed = sgGeneral.add(new DoubleSetting.Builder()
    .name("pitch-rotate-speed")
    .description("The speed for pitch rotation (degrees/tick)")
    .defaultValue(10)
    .min(0)
    .sliderMax(20)
    .build()
    );

    public final Setting<Double> pitchMinus = sgGeneral.add(new DoubleSetting.Builder()
    .name("pitch-Minus")
    .defaultValue(-40)
    .sliderRange(-40, 0)
    .build()
    );

    public final Setting<Double> pitchPlus = sgGeneral.add(new DoubleSetting.Builder()
    .name("pitch-Plus")
    .defaultValue(10)
    .sliderRange(0, 40)
    .build()
    );

    private double ticksLeft;

    @Override
    public void onActivate() {
        if (freeLook.get() && !Modules.get().isActive(FreeLook.class)) Modules.get().get(FreeLook.class).toggle();

        pitch = pitchPlus.get();
    }

    @Override
    public void onDeactivate() {
      if (freeLook.get() && Modules.get().isActive(FreeLook.class)) Modules.get().get(FreeLook.class).toggle();
    }



    @EventHandler
    private void onPreTick(TickEvent.Pre event) {

      if (replace.get()) {
        ItemStack chestStack = mc.player.getInventory().getArmorStack(2);

        if (chestStack.getItem() == Items.ELYTRA) {
            if (chestStack.getMaxDamage() - chestStack.getDamage() <= replaceDurability.get()) {
                FindItemResult elytra = InvUtils.find(stack -> stack.getMaxDamage() - stack.getDamage() > replaceDurability.get() && stack.getItem() == Items.ELYTRA);

                InvUtils.move().from(elytra.slot()).toArmor(2);
            }
        }
    }

      //  debug msg
      //  info(String.valueOf(mc.player.getVelocity().getY()));
        if (pitchingDown && mc.player.getY() <= pitch40lowerBounds.get()) {
            pitchingDown = false;
        }
        else if (!pitchingDown && mc.player.getVelocity().getY() <= 0 && mc.player.getY() >= pitch40upperBounds.get()) {
            pitchingDown = true;
        }

        // Pitch upwards
        if (!pitchingDown && mc.player.getPitch() > pitchMinus.get()) {
            pitch -= pitchRotationSpeed.get();

            if (pitch < pitchMinus.get()) pitch = pitchMinus.get();
        // Pitch downwards
        } else if (pitchingDown && mc.player.getPitch() < pitchPlus.get()) {
            pitch += pitchRotationSpeed.get();

            if (pitch > pitchPlus.get()) pitch = pitchPlus.get();
        }

        if (!pitchingDown  && useFireworks.get()) {
          if (ticksLeft <= 0) {
            ticksLeft = 120;
          FindItemResult itemResult = InvUtils.findInHotbar(Items.FIREWORK_ROCKET);
            if (!itemResult.found()) return;

            if (itemResult.isOffhand()) {
                mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
                mc.player.swingHand(Hand.OFF_HAND);
            } else {
                InvUtils.swap(itemResult.slot(), true);

                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                mc.player.swingHand(Hand.MAIN_HAND);

                InvUtils.swapBack();
            }
          }
            ticksLeft--;
          }

        mc.player.setPitch(pitch.floatValue());
    }
}
