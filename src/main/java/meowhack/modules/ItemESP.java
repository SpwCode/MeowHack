package meowhack.modules;

import meowhack.AddonTemplate;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class ItemESP extends Module {
    public ItemESP() {
        super(AddonTemplate.CATEGORY, "item-esp", "Renders specific dropped items through walls.");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<List<Item>> targetItems = sgGeneral.add(new ItemListSetting.Builder()
        .name("target-items")
        .description("Items to look for.")
        .defaultValue(Items.ELYTRA)
        .build()
    );

    public final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    public final Setting<Double> fillOpacity = sgGeneral.add(new DoubleSetting.Builder()
        .name("fill-opacity")
        .description("Opacity of the shape fill.")
        .defaultValue(0.4)
        .range(0, 1)
        .sliderMax(1)
        .build()
    );

    public final Setting<Boolean> distance = sgGeneral.add(new BoolSetting.Builder()
        .name("distance-colors")
        .description("Color depends on distance.")
        .defaultValue(false)
        .build()
    );

    private final Setting<SettingColor> miscColor = sgGeneral.add(new ColorSetting.Builder()
        .name("color")
        .description("Default color.")
        .defaultValue(new SettingColor(255, 0, 0, 255))
        .build()
    );

    private final Color sideColor = new Color();
    private final Color lineColor = new Color();
    private final Color baseColor = new Color();

    private int count = 0;

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (mc.world == null || mc.player == null) return;

        count = 0;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof ItemEntity itemEntity)) continue;

            ItemStack stack = itemEntity.getStack();
            if (stack == null || !targetItems.get().contains(stack.getItem())) continue;

            if (!EntityUtils.isInRenderDistance(entity)) continue;

            Color color = getColor(entity);
            if (color == null) continue;

            lineColor.set(color);
            sideColor.set(color).a((int) (color.a * fillOpacity.get()));

            Vec3d pos = new Vec3d(itemEntity.getX(), itemEntity.getY(), itemEntity.getZ());
            Box box = new Box(
                pos.x - 0.25, pos.y, pos.z - 0.25,
                pos.x + 0.25, pos.y + 0.5, pos.z + 0.25
            );

            event.renderer.box(box, sideColor, lineColor, shapeMode.get(), 0);
            count++;
        }
    }

    public Color getColor(Entity entity) {
        if (distance.get()) {
            return EntityUtils.getColorFromDistance(entity);
        } else {
            return baseColor.set(miscColor.get());
        }
    }

    @Override
    public String getInfoString() {
        return Integer.toString(count);
    }
}
