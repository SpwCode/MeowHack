package meowhack.modules;

import meowhack.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.util.Hand;

public class RocketRight extends Module {
    private double ticksLeft;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    public final Setting<Integer> FireworkDelay = sgGeneral.add(new IntSetting.Builder()
        .name("firework-delay")
        .description("The delay in seconds in between using fireworks if \"Use Fireworks\" is enabled.")
        .min(1)
        .defaultValue(1)
        .sliderMax(10)
        .build()
    );
    public RocketRight() {
        super(AddonTemplate.CATEGORY, "RocketRight", "Использовать рокеты из инвентаря или хотбара в полете, когда в руках меч или инструмент.");
    }

    public void onActivate() {
        ticksLeft = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (ticksLeft <= 0) {
            if (mc.options.useKey.isPressed() && this.mc.currentScreen == null && !(this.mc.player.isOnGround()) && (this.mc.player.getMainHandStack().getItem() instanceof SwordItem || this.mc.player.getMainHandStack().getItem() instanceof MiningToolItem || this.mc.player.getMainHandStack().isEmpty())) {
                FindItemResult rocket = InvUtils.find(Items.FIREWORK_ROCKET);
                if (rocket.slot() > 8) {
                    InvUtils.move().from(rocket.slot()).toHotbar(8);
                }
                FindItemResult itemResult = InvUtils.findInHotbar(Items.FIREWORK_ROCKET);
                if (!itemResult.found()) return;
                if (itemResult.isOffhand()) {
                    mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
                    ticksLeft = FireworkDelay.get() * 20;
                } else {
                    InvUtils.swap(itemResult.slot(), true);
                    mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
                    mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                    InvUtils.swapBack();
                    ticksLeft = FireworkDelay.get() * 20;
                }
            }
        }
        ticksLeft--;
    }
}
