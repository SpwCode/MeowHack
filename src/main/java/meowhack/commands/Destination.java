package meowhack.commands;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meowhack.modules.AutoPilot;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.pathing.BaritoneUtils;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.Dimension;
import net.minecraft.command.CommandSource;
import net.minecraft.util.math.BlockPos;

/**
 * The Meteor Client command API uses the <a href="https://github.com/Mojang/brigadier">same command system as Minecraft does</a>.
 */
public class Destination extends Command {
    /**
     * The {@code name} parameter should be in kebab-case.
     */
    public Destination() {
        super("destination", "Sends a message.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder
            .then(argument("X", (ArgumentType) IntegerArgumentType.integer())
                .then(argument("Y", (ArgumentType)IntegerArgumentType.integer())
                    .then(argument("Z", (ArgumentType)IntegerArgumentType.integer())
                        .executes(context -> {
                            if (PlayerUtils.getDimension() == Dimension.Overworld) {
                                AutoPilot autoPilot = (AutoPilot) Modules.get().get(AutoPilot.class);
                                BlockPos pos = new BlockPos(((Integer)context.getArgument("X", Integer.class)).intValue(), (int)mc.player.getY(), ((Integer)context.getArgument("Z", Integer.class)).intValue());
                                autoPilot.coords.set(pos);
                                autoPilot.toggle();
                            }
                            if (PlayerUtils.getDimension() == Dimension.Nether) {
                                ChatUtils.sendPlayerMsg(BaritoneUtils.getPrefix() + "goal " + BaritoneUtils.getPrefix() + ((Integer)context.getArgument("X", Integer.class)).toString() + " ~ " + ((Integer)context.getArgument("Z", Integer.class)).toString());
                                ChatUtils.sendPlayerMsg(BaritoneUtils.getPrefix() + "elytra");
                            }
                            return SINGLE_SUCCESS;
        }))));
    }
}
