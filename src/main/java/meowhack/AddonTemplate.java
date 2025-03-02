package meowhack;

import meowhack.commands.CalcCommand;
import meowhack.commands.Destination;
import meowhack.hud.HudExperience;
import com.mojang.logging.LogUtils;
import meowhack.modules.*;
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
        Modules.get().add(new VanillaFly());
        Modules.get().add(new AutoBoost());
        Modules.get().add(new AutoPilot());
        Modules.get().add(new MoveHelper());

        // Commands
        Commands.add(new Destination());
        Commands.add(new CalcCommand());

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
