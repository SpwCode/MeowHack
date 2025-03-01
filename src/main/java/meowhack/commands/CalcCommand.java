package meowhack.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;

public class CalcCommand extends Command {
    public CalcCommand() {
        super("calc", "Calculator.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("value", IntegerArgumentType.integer()).executes(context -> {
            int count = IntegerArgumentType.getInteger(context, "value");
            int shulkers = 0;
            if (count > 1728) {
                shulkers = count / 1728;
                count %= 1728;
            }
            int stacks = count / 64;
            int blocks = count % 64;
            String result = "";
            if (shulkers > 0) {
                result += shulkers + ",";
                double remainder = (double) count / 1728.0;
                result += String.format("%.2f", remainder).replace(",", "") + " шалкеров, ";
            }
            if (stacks > 0) {
                result += stacks + " стаков и ";
            }
            result += blocks + " блоков";
            ChatUtils.info(result);



            return SINGLE_SUCCESS;
        }));
    }
}
