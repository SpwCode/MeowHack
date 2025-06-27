package meowhack.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;

public class Calculator extends Command {
    public Calculator() {
        super("calculator", "Вычисляет простые выражения: число операция число. Пример: .calculator 8 + 4", "c" , "calc");
    }
    Integer result;
    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("message", StringArgumentType.greedyString()).executes(context -> {
            String msg = context.getArgument("message", String.class);
            calc(msg);
            return SINGLE_SUCCESS;
        }));
    }

    private void numberToStacks(Integer number){
        int count = number;
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
    }

    private void calc(String input) {
        input = input.trim();

        // Если просто число — обрабатываем numberToStacks
        if (input.matches("^\\d+$")) {
            try {
                int number = Integer.parseInt(input);
                numberToStacks(number);
            } catch (NumberFormatException e) {
                ChatUtils.error("Неверный формат числа: " + input);
            }
            return;
        }

        // Поддержка формата "9*3", "888+12", "100/25", "100-5"
        // Разбиваем строку на num1, operator, num2
        var matcher = input.replace(",", ".").trim().replaceAll("\\s+", "")
            .matches("^(\\d+(?:\\.\\d+)?)([+\\-*/])(\\d+(?:\\.\\d+)?)$") ?
            input.replace(",", ".").replaceAll("\\s+", "").replaceAll("([+\\-*/])", " $1 ").split(" ") :
            null;

        if (matcher == null || matcher.length != 3) {
            ChatUtils.error("Неверный формат. Пример: .calculator 9 * 3 или .calculator 9*3 или просто .calculator 1728");
            return;
        }

        try {
            double num1 = Double.parseDouble(matcher[0]);
            String operator = matcher[1];
            double num2 = Double.parseDouble(matcher[2]);

            double result;
            switch (operator) {
                case "+" -> result = num1 + num2;
                case "-" -> result = num1 - num2;
                case "*" -> result = num1 * num2;
                case "/" -> {
                    if (num2 == 0) {
                        ChatUtils.error("Деление на ноль!");
                        return;
                    }
                    result = num1 / num2;
                }
                default -> {
                    ChatUtils.error("Неизвестный оператор: " + operator);
                    return;
                }
            }

            ChatUtils.info(num1 + " " + operator + " " + num2 + " = " + result);
        } catch (NumberFormatException e) {
            ChatUtils.error("Ошибка разбора чисел.");
        }
    }

}
