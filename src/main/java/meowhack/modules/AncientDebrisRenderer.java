package meowhack.modules;

import meowhack.AddonTemplate;
import meteordevelopment.meteorclient.events.entity.player.BreakBlockEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import meteordevelopment.meteorclient.gui.widgets.WWidget;


import java.util.ArrayList;
import java.util.List;

public class AncientDebrisRenderer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> lagg = sgGeneral.add(new DoubleSetting.Builder()
        .name("stop-on-lags")
        .description("Pause on server lagging. (Time since last tick)")
        .defaultValue(0.3)
        .min(0.1)
        .max(1)
        .sliderMin(0.1)
        .sliderMax(1)
        .build());

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("radius")
        .defaultValue(5)
        .min(0)
        .sliderRange(1, 20)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgGeneral.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color.")
        .defaultValue(new SettingColor(5, 255, 0,75))
        .build());

    private final List<BlockPos> debrisList = new ArrayList<>();
    private static final String FILE_PATH = "config/ancient_debris.json";
    private final Gson gson = new Gson();

    public AncientDebrisRenderer() {
        super(AddonTemplate.CATEGORY, "AncientDebrisRenderer", "Renders ancient debris blocks around the player.");
    }


    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();

        // Button to Clear Interacted Blocks
        WButton clear = list.add(theme.button("Clear Rendering Cache")).expandX().widget();

        clear.action = () -> {
            debrisList.clear();    // Очистка списка
            saveDebrisList();      // Сохранение изменений
        };

        return list;
    }

    @Override
    public void onActivate() {
        //debrisList.clear();
        loadDebrisList();
    }


    @EventHandler
    private void BreakBlockEvent(BreakBlockEvent event) {
        BlockPos breakingPos = event.blockPos;

       // if (mc.world.getBlockState(breakingPos).getBlock() != Blocks.ANCIENT_DEBRIS) return;

        if (debrisList.contains(breakingPos)) {
            debrisList.remove(breakingPos);
            saveDebrisList();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post e) {
        World world = mc.world;
        if (world == null || mc.player == null) return;
        if (TickRate.INSTANCE.getTimeSinceLastTick() >= lagg.get()) return;

        BlockPos playerPos = mc.player.getBlockPos();
        int radius = range.get();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -1; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    // info("Проверка блока: " + pos + " | " + world.getBlockState(pos).getBlock());
                    if (world.getBlockState(pos).getBlock() == Blocks.ANCIENT_DEBRIS && !debrisList.contains(pos)) {
                      //  info("Найден древний обломок: " + pos);
                        if (!isBlockInLineOfSight(pos) && isExposedToAir(pos)) {
                            debrisList.add(pos);
                            saveDebrisList();
                          //  info("Блок найден: " + pos.toString());
                        }
                    }
                }
            }
        }
    }


    private boolean isBlockInLineOfSight(BlockPos placeAt) {
        Vec3d playerHead = new Vec3d(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ());
        double[] offsets = {0.01, 0.5, 0.99};

        for (double offset : offsets) {
            Vec3d targetVec = new Vec3d(placeAt.getX() + 0.5, placeAt.getY() + offset, placeAt.getZ() + 0.5);
            BlockHitResult bhr = mc.world.raycast(new RaycastContext(playerHead, targetVec, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));

            if (bhr != null && bhr.getBlockPos().equals(placeAt)) {
                return false; // Если хотя бы один из лучей достигает блока, он виден
            }
        }
        return true; // Если ни один из лучей не достиг блока, он вне зоны видимости
    }

    private boolean isExposedToAir(BlockPos pos) {
        boolean exposed = mc.world.isAir(pos.up()) || mc.world.isAir(pos.down()) ||
            mc.world.isAir(pos.north()) || mc.world.isAir(pos.south()) ||
            mc.world.isAir(pos.east()) || mc.world.isAir(pos.west());
        return exposed;
    }

    // Сохранение списка в файл
    private void saveDebrisList() {
        try (Writer writer = new FileWriter(FILE_PATH)) {
            gson.toJson(debrisList, writer);
        } catch (IOException e) {
            error("Ошибка сохранения списка: " + e.getMessage());
        }
    }

    // Загрузка списка из файла (если файла нет — создаёт новый)
    private void loadDebrisList() {
        File file = new File(FILE_PATH);

        // Если файла нет — создаём пустой файл
        if (!file.exists()) {
            try {
                if (file.createNewFile()) {
                    info("Файл для хранения Ancient Debris создан: " + FILE_PATH);
                }
            } catch (IOException e) {
                error("Ошибка при создании файла: " + e.getMessage());
            }
            return; // Прерываем, так как загружать нечего
        }

        // Если файл существует, загружаем данные
        try (Reader reader = new FileReader(file)) {
            Type type = new TypeToken<List<BlockPos>>() {}.getType();
            debrisList.clear();
            List<BlockPos> loadedList = gson.fromJson(reader, type);

            if (loadedList != null) {
                debrisList.addAll(loadedList);
            }

            info("Загружено " + debrisList.size() + " блоков Ancient Debris.");
        } catch (IOException e) {
            error("Ошибка загрузки списка: " + e.getMessage());
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (debrisList.isEmpty()) return;

        for (BlockPos pos : debrisList) {
            event.renderer.box(pos, sideColor.get(), meteordevelopment.meteorclient.utils.render.color.Color.WHITE, ShapeMode.Both, 0);
            // renderLine(event, pos);
        }
    }

    public void renderLine(Render3DEvent event, BlockPos placeAt) {
            Vec3d playerHead = new Vec3d(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ());
            Vec3d placeAtVec = new Vec3d(placeAt.getX() + 0.5, placeAt.getY(), placeAt.getZ() + 0.5);
            BlockHitResult bhr = mc.world.raycast(new RaycastContext(playerHead, placeAtVec, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
            info("placeAt block" + placeAt.toString() + "Block hit result" + bhr.getBlockPos().toString());
            event.renderer.line(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ(), placeAt.getX() + 0.5, placeAt.getY(), placeAt.getZ() + 0.5,  meteordevelopment.meteorclient.utils.render.color.Color.WHITE);
            event.renderer.line(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ(), placeAt.getX() + 0.5, placeAt.getY() + 1, placeAt.getZ() + 0.5,  meteordevelopment.meteorclient.utils.render.color.Color.WHITE);
    }
}
