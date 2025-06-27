package meowhack.modules;

import meowhack.AddonTemplate;
import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.TntBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Arrays;
import java.util.List;

public class ExplosionProtector extends Module {
    public ExplosionProtector() {
        super(AddonTemplate.CATEGORY, "explosion-protector", "Explosion protection.");
    }
    private final SettingGroup groupDefault = settings.getDefaultGroup();
    Setting<Boolean> tnt = groupDefault.add(new BoolSetting.Builder()
    		.name("break-tnt")
    		.description("Break near tnt blocks")
    		.defaultValue(true)
    		.build());
    Setting<Boolean> crystalHead = groupDefault.add(new BoolSetting.Builder()
    		.name("anti-crystal-head")
    		.defaultValue(true)
    		.build());
	Setting<Boolean> rotate = groupDefault.add(new BoolSetting.Builder()
			.name("rotate")
			.description("Look at block or crystal.")
			.defaultValue(false)
			.build());


	@EventHandler
	public void event_PacketReceive(PacketEvent.Receive e) throws Exception {
    	if(!online()) return;

        if (crystalHead.get() && e.packet instanceof BlockBreakingProgressS2CPacket) {
        	BlockBreakingProgressS2CPacket w = (BlockBreakingProgressS2CPacket) e.packet;

    		BlockPos p = mc.player.getBlockPos();
    		List<BlockPos> safe = Arrays.asList( p.up(), p.up(2), p.up(3) );

    		if(safe.contains(w.getPos())) {
			    mc.world.getEntities().forEach(s ->{
				    if(s instanceof EndCrystalEntity && safe.contains(s.getBlockPos())) {
						kill(s);
						place_obsidian(s.getBlockPos());
				    }
			    });
//    			safe.forEach(s ->{
//    				place_obsidian(s);
//    			});

    		}
        }


        if (tnt.get() && e.packet instanceof BlockUpdateS2CPacket p && p.getState().getBlock() instanceof TntBlock) {
	        BlockPos pos = mc.player.getBlockPos();
	        List<BlockPos> safe = List.of(
			        pos, pos.down(), pos.up(), pos.up(2), pos.up(3),
			        pos.east(), pos.west(), pos.north(), pos.south(),
			        pos.up().east(), pos.up().west(), pos.up().north(), pos.up().south(),
			        pos.up(2).east(), pos.up(2).west(), pos.up(2).north(), pos.up(2).south()
	        );

	        BlockPos a = p.getPos();
	        if(safe.contains(a)) {
		        look(a);
		        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, a, Direction.UP));
		        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, a, Direction.UP));
		        place_obsidian(a);
	        }
        }
	}

	@EventHandler
	public void event_EntityAdded(EntityAddedEvent e) {
    	if( !online() || !crystalHead.get() || !(e.entity instanceof EndCrystalEntity) ) return;

		BlockPos p = mc.player.getBlockPos();
		List<BlockPos> safe = Arrays.asList(
				p.up(2), p.up(3), p.up(4),
				p.up(2).east(), p.up(2).west(), p.up(2).north(), p.up(2).south()
				);
		BlockPos a = e.entity.getBlockPos();
		if(safe.contains(a)) {
			place_obsidian(a.down());
			kill(e.entity);
			place_obsidian(a);
			place_obsidian(a.up());
		}
    }

	private boolean online(){
		return mc.world.getPlayers().size() > 1;
	}

	private void kill(Entity a) {
		look(a.getBlockPos());
		mc.interactionManager.attackEntity(mc.player, a);
		a.remove(RemovalReason.KILLED);
	}

    private void place_obsidian(BlockPos pos) {
    	if(mc.player.getBlockPos().getY() - pos.getY() > 2) return;
		FindItemResult obsidian =  InvUtils.findInHotbar(Items.OBSIDIAN);
		if(obsidian.found())  BlockUtils.place(pos, obsidian, 50);
	}

    private void look(BlockPos a) {
    	if(!rotate.get()) return;
    	return;
    }

}
