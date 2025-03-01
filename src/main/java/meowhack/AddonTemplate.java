package meowhack;

import meowhack.commands.CommandExample;
import meowhack.hud.HudExperience;
import com.mojang.logging.LogUtils;
import meowhack.modules.AreaScan;
import meowhack.modules.AutoBoost;
import meowhack.modules.RocketRight;
import meowhack.modules.VanillaNuker;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class AddonTemplate extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("MeowHack");
    public static final HudGroup HUD_GROUP = new HudGroup("Example");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Meteor Addon Template");

        // Modules
        Modules.get().add(new AreaScan());
        Modules.get().add(new RocketRight());
        Modules.get().add(new VanillaNuker());
        Modules.get().add(new AutoBoost());

        // Commands
        Commands.add(new CommandExample());

        // HUD
        Hud.get().register(HudExperience.INFO);
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "meowhack";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("MeteorDevelopment", "meteor-addon-template");
    }
}
