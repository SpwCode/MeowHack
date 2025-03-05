package meowhack.modules;


import meowhack.AddonTemplate;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.c2s.play.RenameItemC2SPacket;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import net.minecraft.block.AnvilBlock;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;

public class AutoEnchBeta extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("operation-delay")
        .description("The amount of delay between interactions.")
        .defaultValue(20)
        .min(1)
        .sliderRange(1, 200)
        .build()
    );

    private final Setting<Integer> scanDelayForAnvil = sgGeneral.add(new IntSetting.Builder()
        .name("anvil-scan-delay ")
        .description("Delay between anvil interactions. In seconds")
        .defaultValue(30)
        .range(1, 60)
        .sliderMax(60)
        .build()
    );

    private static File file = new File(MeteorClient.FOLDER, "bookbot.txt");
    private final PointerBuffer filters;
    private int delayTimer,anvilWait;
    public int lineNumber = 1;
    private static final MinecraftClient mc = MinecraftClient.getInstance();


    public AutoEnchBeta() {
        super(AddonTemplate.CATEGORY, "AutoEnchBeta", "Automatically do stuff.");

        if (!file.exists()) {
            file = null;
        }

        filters = BufferUtils.createPointerBuffer(1);

        ByteBuffer txtFilter = MemoryUtil.memASCII("*.txt");

        filters.put(txtFilter);
        filters.rewind();
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WHorizontalList list = theme.horizontalList();

        WButton selectFile = list.add(theme.button("Select File")).widget();

        WLabel fileName = list.add(theme.label((file != null && file.exists()) ? file.getName() : "No file selected.")).widget();

        selectFile.action = () -> {
            String path = TinyFileDialogs.tinyfd_openFileDialog(
                "Select File",
                new File(MeteorClient.FOLDER, "bookbot.txt").getAbsolutePath(),
                filters,
                null,
                false
            );

            if (path != null) {
                file = new File(path);
                fileName.set(file.getName());
            }
        };

        return list;
    }

    @Override
    public void onActivate() {
        if ((file == null || !file.exists())) {
            info("No file selected, please select a file in the GUI.");
            toggle();
            return;
        }
        delayTimer = delay.get();
    }

    private void clickAtAnvil() {
        if (anvilWait > 0) {
            anvilWait--;
            return;
        }
        BlockPos playerPos = mc.player.getBlockPos();
        for (BlockPos pos : BlockPos.iterate(playerPos.add(-4, -4, -4), playerPos.add(4, 5, 4))) {
            BlockState state = mc.world.getBlockState(pos);
            if (state.getBlock() instanceof AnvilBlock) {
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Utils.vec3d(pos), Direction.UP, pos, false));
                anvilWait = scanDelayForAnvil.get()*20;
                return;
            }
        }
        anvilWait = scanDelayForAnvil.get()*20;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {

        // Check current screen handler
        if (!(mc.player.currentScreenHandler instanceof AnvilScreenHandler)) {
            clickAtAnvil();
            return;
        }

        // Check delay
        if (delayTimer > 0) {
            delayTimer--;
            return;
        }

        // Reset delay
        delayTimer = delay.get();


        // Ignore if somehow the file got deleted
        if ((file == null || !file.exists())) {
            info("No file selected, please select a file in the GUI.");
            toggle();
            return;
        }

        // Handle the file being empty
        if (file.length() == 0) {
            MutableText message = Text.literal("");
            message.append(Text.literal("The file is empty! ").formatted(Formatting.RED));
            message.append(Text.literal("Click here to edit it.")
                .setStyle(Style.EMPTY
                        .withFormatting(Formatting.UNDERLINE, Formatting.RED)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, file.getAbsolutePath()))
                )
            );
            toggle();
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int currentLineNumber = 0;
            while ((line = reader.readLine()) != null) {
                currentLineNumber++;
                if (currentLineNumber != lineNumber) {
                    continue;
                }

                String[] parts = line.split(",");
                int slotNumber = Integer.parseInt(parts[0].trim());
                //error(String.valueOf(lineNumber) + " Слот: " + String.valueOf(slotNumber));
                if (parts[1].trim().isEmpty() || parts[2].trim().isEmpty()) {
                    error("Invalid instruction on line " + lineNumber + ": " + line);
                    continue;
                }

                if (!(mc.player.currentScreenHandler instanceof AnvilScreenHandler)) {
                    error("Not in a Anvil screen, can't execute instruction: " + line);
                    continue;
                }

                if ((slotNumber == 2)) {
                    takeResults();
                    return;
                }

                if ((slotNumber == 3)) {
                        ItemStack itemStack = ((AnvilScreenHandler) mc.player.currentScreenHandler).slots.get(0).getStack();
                        if (itemStack.isEmpty()) {
                            return;
                        }
                        mc.player.networkHandler.sendPacket(new RenameItemC2SPacket(parts[2].trim()));
                        lineNumber++;
                        return;
                }

                if (slotNumber == 9) {
                    lineNumber = 1;
                        if (mc.player.currentScreenHandler instanceof AnvilScreenHandler) {
                            mc.player.networkHandler.sendPacket(new RenameItemC2SPacket(""));
                        }
                    return;
                }

                for (int i = 3; i < ((AnvilScreenHandler) mc.player.currentScreenHandler).slots.size(); i++) {
                    ItemStack itemStack = ((AnvilScreenHandler) mc.player.currentScreenHandler).slots.get(i).getStack();
                    if (itemStack.getCustomName() != null) {
                        InvUtils.drop().slotId(i); // Выбрасываем предмет
                        continue;
                    }

                    if (!itemStack.getItem().toString().contains(parts[1].trim().toLowerCase())) {
                        continue;
                    }

                    ItemEnchantmentsComponent enchantments = EnchantmentHelper.getEnchantments(itemStack);
                    if (!(enchantments.toString().toLowerCase().contains(parts[2].trim().toLowerCase()))) continue;

                    InvUtils.move().fromId(i).toId(slotNumber);
                    lineNumber++;
                    return;
                }
            }
        }
            catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void takeResults() {
        AnvilScreenHandler anvilScreenHandler = (AnvilScreenHandler) mc.player.currentScreenHandler;
        ItemStack resultStack = anvilScreenHandler.getSlot(2).getStack();
        InvUtils.shiftClick().slotId(2);
        if (resultStack.isEmpty()) {
            lineNumber++;
        }
    }
}
