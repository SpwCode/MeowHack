package meowhack.modules;

import meowhack.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.*;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

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

    private final Setting<Boolean> useBlocks = sgGeneral.add(new BoolSetting.Builder()
        .name("allow-blocks")
        .description("Разрешить летать на блоках")
        .defaultValue(false)
        .build()
    );

    public RocketRight() {
        super(AddonTemplate.CATEGORY, "Rocket-Right", "Использовать ракеты из инвентаря или хотбара в полете, когда в руках меч или инструмент.");
    }

    public void onActivate() {
        ticksLeft = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (ticksLeft <= 0) {
            if (mc.options.useKey.isPressed() &&
                this.mc.currentScreen == null &&
                mc.player.isGliding() &&
                !isLookingAtContainer() &&
                (this.mc.player.getMainHandStack().getItem() instanceof SwordItem ||
                    this.mc.player.getMainHandStack().getItem() instanceof MiningToolItem ||
                    this.mc.player.getMainHandStack().isEmpty() ||
                    (useBlocks.get() && this.mc.player.getMainHandStack().getItem() instanceof BlockItem))) {

                if (useFireworkFromInventory(8)) { // 8 - это номер слота хотбара (0-8)
                    ticksLeft = FireworkDelay.get() * 20;
                }
            }
        }
        ticksLeft--;
    }

    public boolean useFireworkFromInventory(int targetHotbarSlot) {
        // Находим ракету в инвентаре
        FindItemResult rocket = InvUtils.find(Items.FIREWORK_ROCKET);

        // Если ракета не в хотбаре - перемещаем в указанный слот
        if (rocket.slot() > 8) {
            InvUtils.move().from(rocket.slot()).toHotbar(targetHotbarSlot);
        }

        // Ищем ракету в хотбаре
        FindItemResult itemResult = InvUtils.findInHotbar(Items.FIREWORK_ROCKET);
        if (!itemResult.found()) return false;

        // Используем ракету
        if (itemResult.isOffhand()) {
            mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
        } else {
            InvUtils.swap(itemResult.slot(), true);
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            InvUtils.swapBack();
        }

        return true;
    }

    private boolean isLookingAtContainer() {
        if (mc.crosshairTarget == null || !(mc.crosshairTarget instanceof BlockHitResult)) {
            return false;
        }

        BlockPos pos = ((BlockHitResult) mc.crosshairTarget).getBlockPos();
        BlockState blockState = mc.world.getBlockState(pos);

        return blockState.getBlock() instanceof InventoryProvider || blockState.getBlock() instanceof ChestBlock ||
            blockState.getBlock() instanceof BarrelBlock || blockState.getBlock() instanceof ShulkerBoxBlock;
    }
}
