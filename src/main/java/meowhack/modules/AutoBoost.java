package meowhack.modules;

import meowhack.AddonTemplate;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AutoBoost extends Module {
  public AutoBoost() {
    super(AddonTemplate.CATEGORY, "auto-boost", "BleachHack Elytra Fly.");
  }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> noFall = sgGeneral.add(new BoolSetting.Builder()
    .name("no-fall")
    .description("Boost speed.")
    .defaultValue(false)
    .build()
    );

    private final Setting<Double> settingBoost = sgGeneral.add(new DoubleSetting.Builder()
        .name("boost")
        .description("Boost speed.")
        .defaultValue(0.1D)
        .sliderMin(0.0D).sliderMax(0.5D)
        .build()
        );

    private final Setting<Double> settingBoost2 = sgGeneral.add(new DoubleSetting.Builder()
        .name("boost2test")
        .description("Boost speed.")
        .defaultValue(-0.100)
        .sliderRange(-0.5, 0)
        .build()
        );

    private final Setting<Double> settingMaxBoost = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-boost")
        .description("Max boost speed.")
        .defaultValue(4.1D)
        .sliderMin(0.0D).sliderMax(5.0D)
        .build()
    );

    private final Setting<Integer> maximumSpeed = sgGeneral.add(new IntSetting.Builder()
    .name("max-speed")
    .description("Max speed.")
    .defaultValue(35)
    .sliderMin(0).sliderMax(100)
    .build()
    );



  @EventHandler
  public void onClientMove(PlayerMoveEvent event) {
    if (mc.options.sneakKey.isPressed() && !mc.player.isOnGround()) {
        mc.player.setVelocity(new Vec3d(0, mc.player.getVelocity().y, 0));
    }
  }

  @EventHandler
  public void event_ZPlayerMove(PlayerMoveEvent e) {
    if (noFall.get() && e.movement.y > 0) ((IVec3d)e.movement).meteor$setY(0);
    // if (mc.player.isGliding()) ((IVec3d)e.movement).setY(0);
}

  @EventHandler
  private void TickEvent(TickEvent.Pre e) {

      double currentVel = Math.abs(mc.player.getVelocity().x) + Math.abs(mc.player.getVelocity().y) + Math.abs(mc.player.getVelocity().z);
      float radianYaw = (float) Math.toRadians(mc.player.getYaw());
      float boost = settingBoost.get().floatValue();
      float boost2 = settingBoost2.get().floatValue();


      if (mc.player.isGliding()) {
        if ((Math.round(Utils.getPlayerSpeed().horizontalLength())) > maximumSpeed.get()) {
          mc.player.addVelocity(MathHelper.sin(radianYaw) * -boost2, 0, MathHelper.cos(radianYaw) * boost2);
          return;
        }
        if ((Math.round(Utils.getPlayerSpeed().horizontalLength())) < maximumSpeed.get()) {
          if (mc.options.backKey.isPressed()) {
              mc.player.addVelocity(MathHelper.sin(radianYaw) * boost, 0, MathHelper.cos(radianYaw) * -boost);
          } else if (mc.player.getPitch() > 0) {
              mc.player.addVelocity(MathHelper.sin(radianYaw) * -boost, 0, MathHelper.cos(radianYaw) * boost);
          }
        }
    }
  }
}
