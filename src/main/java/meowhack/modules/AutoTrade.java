package meowhack.modules;

import fi.dy.masa.itemscroller.util.InventoryUtils;
import meowhack.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.FreeLook;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.Hand;
import net.minecraft.village.TradeOffer;


public class AutoTrade extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> dontDemand = sgGeneral.add(new BoolSetting.Builder()
        .name("don't-demand-bonus")
        .description("Trade fair price")
        .defaultValue(true)
        .build()
    );


    public AutoTrade() {
        super(AddonTemplate.CATEGORY, "Auto Trade", "This module use ItemScroller mod to trade with villagers automatically. ItemScroller required.");
    }

    @Override
    public void onDeactivate() {
        mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
        mc.options.useKey.setPressed(false);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {

        Entity target = mc.targetedEntity;

        if ((target instanceof VillagerEntity) && !((VillagerEntity) target).isBaby()) {
            mc.options.useKey.setPressed(false);
            mc.options.useKey.setPressed(true);
        }

        if (mc.player.currentScreenHandler instanceof MerchantScreenHandler) {
            ScreenHandler handler1 = mc.player.currentScreenHandler;
            if (((MerchantScreenHandler) handler1).getRecipes().isEmpty()) return;
            TradeOffer offer = ((MerchantScreenHandler) handler1).getRecipes().get(0);
            int beenUsed = offer.getUses();
            int maxUses = offer.getMaxUses();


            if ((offer.getDemandBonus() == 0 && beenUsed < maxUses) || !dontDemand.get()) {
                InventoryUtils.villagerTradeEverythingPossibleWithAllFavoritedTrades();
            }
            mc.setScreen(null);
        }
    }

}
