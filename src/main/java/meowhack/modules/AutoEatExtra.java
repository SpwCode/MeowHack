package meowhack.modules;

import meowhack.AddonTemplate;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.pathing.BaritoneUtils;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.AnchorAura;
import meteordevelopment.meteorclient.systems.modules.combat.BedAura;
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.meteorclient.systems.modules.movement.AutoWalk;
import meteordevelopment.meteorclient.systems.modules.player.AutoGap;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.List;

public class AutoEatExtra extends Module {
    public AutoEatExtra() {
        super(AddonTemplate.CATEGORY, "auto-eat-extra", "Automatically eats fucking food.");
    }

    private static final Class<? extends Module>[] AURAS = new Class[] { KillAura.class, CrystalAura.class, AnchorAura.class, BedAura.class };

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgHunger = settings.createGroup("Hunger");

    // General

    private final Setting<List<Item>> blacklist = sgGeneral.add(new ItemListSetting.Builder()
        .name("blacklist")
        .description("Which items to not eat.")
        .defaultValue(
            Items.ENCHANTED_GOLDEN_APPLE,
            Items.GOLDEN_APPLE,
            Items.CHORUS_FRUIT,
            Items.POISONOUS_POTATO,
            Items.PUFFERFISH,
            Items.CHICKEN,
            Items.ROTTEN_FLESH,
            Items.SPIDER_EYE,
            Items.SUSPICIOUS_STEW
        )
        .build()
    );


    private final Setting<List<Module>> pausedModules = sgGeneral.add(new ModuleListSetting.Builder()
        .name("paused-modules")
        .defaultValue(List.of(
            Modules.get().get(CrystalAura.class),
            Modules.get().get(BedAura.class),
            Modules.get().get(AutoWalk.class)
        ))
        .build()
    );

    private final Setting<Boolean> pauseBaritone = sgGeneral.add(new BoolSetting.Builder()
        .name("pause-baritone")
        .description("Pause baritone when eating.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> healthThreshold = sgGeneral.add(new DoubleSetting.Builder()
        .name("health-threshold")
        .description("The level of health you eat at.")
        .defaultValue(10)
        .range(1, 19)
        .sliderRange(1, 19)
        .build()
    );

    private final Setting<Integer> hungerThreshold = sgHunger.add(new IntSetting.Builder()
        .name("hunger-threshold")
        .description("The level of hunger you eat at.")
        .defaultValue(19)
        .min(1)
        .max(19)
        .sliderMin(1)
        .sliderMax(19)
        .build()
    );




    private boolean eating;
    private int wait;
    private int selectedSlot;

    private final List<Module> currentModulesIsPaused = new ArrayList<>();
    private boolean currentBaritoneIsPaused;

    @Override
    public void onActivate() {
//        if (pausedModules.get().isEmpty()) {
//            for (Module module : Modules.get().getAll()) pausedModules.get().add(module);
//        }
        wait = 0;
        eating = false;
    }

    @EventHandler
    private void a(PacketEvent.Send e) {
        if (eating && e.packet instanceof PlayerActionC2SPacket p && p.getAction() == PlayerActionC2SPacket.Action.RELEASE_USE_ITEM) e.cancel();
    }

    @EventHandler
    private void a(PacketEvent.Sent e) {
        if (eating && wait < 35 && e.packet instanceof UpdateSelectedSlotC2SPacket) {
            eating = false;
            wait = 100;
            resumeAuras();
        }
    }


//    @Override
//    public String getInfoString() {
//        return String.valueOf(wait);
//    }


    @EventHandler(priority = EventPriority.LOW)
    private void a(TickEvent.Pre e) {

        if (wait > 0) {
            wait--;
            if (wait == 0 && eating) {
                resumeAuras();
                eating = false;
                wait = 0;
                InvUtils.swap(selectedSlot, false);
            }
            return;
        }

        if (mc.options.useKey.isPressed() || mc.options.attackKey.isPressed()) return;
        if (Modules.get().get(AutoGap.class).isEating()) return;

        if ((mc.player.getHungerManager().getFoodLevel() <= hungerThreshold.get()) || (mc.player.getHealth() <= healthThreshold.get())){
            int slot = findSlot();
            if (slot != -1) {
                selectedSlot = mc.player.getInventory().selectedSlot;
                InvUtils.swap(slot, false);
                eating = true;
                wait = 36;
//                wait = wait_eat_delay.get();
                pauseAuras();
                mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, slot, 0, 0));
            }
        }
    }


    private void pauseAuras() {
        // Pause auras
        currentModulesIsPaused.clear();
        for (Module module : pausedModules.get()) {
            if (module.isActive()) {
                currentModulesIsPaused.add(module);
                module.toggle();
            }
        }

        // Pause baritone

            if (pauseBaritone.get() && !currentBaritoneIsPaused) {
        currentBaritoneIsPaused = true;
        ChatUtils.sendPlayerMsg(BaritoneUtils.getPrefix() + "pause");
    }
}


    private void resumeAuras() {
        // Resume auras
        for (Module module : currentModulesIsPaused) {
            if (!module.isActive()) {
                module.toggle();
            }
        }
        currentModulesIsPaused.clear();

        // Resume baritone
            if (pauseBaritone.get() && currentBaritoneIsPaused) {
        currentBaritoneIsPaused = false;
        ChatUtils.sendPlayerMsg(BaritoneUtils.getPrefix() + "resume");
    }
}


    private int findSlot() {
        int slot = -1;
        int bestHunger = -1;

        for (int i = 0; i < 9; i++) {
            Item item = mc.player.getInventory().getStack(i).getItem();
            FoodComponent foodComponent = item.getComponents().get(DataComponentTypes.FOOD);
            if (foodComponent == null) continue;

            int hunger = foodComponent.nutrition();
            if (hunger > bestHunger) {
                if (blacklist.get().contains(item)) continue;

                slot = i;
                bestHunger = hunger;
            }
        }

        return slot;
    }

    private static List<Item> getDefaultBlacklist() {
        List<Item> l = new ArrayList<>(9);

        l.add(Items.ENCHANTED_GOLDEN_APPLE);
        l.add(Items.GOLDEN_APPLE);
        l.add(Items.CHORUS_FRUIT);
        l.add(Items.POISONOUS_POTATO);
        l.add(Items.PUFFERFISH);
        l.add(Items.CHICKEN);
        l.add(Items.ROTTEN_FLESH);
        l.add(Items.SPIDER_EYE);
        l.add(Items.SUSPICIOUS_STEW);

        return l;
    }
}
