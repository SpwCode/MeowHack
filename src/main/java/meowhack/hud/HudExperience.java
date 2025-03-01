package meowhack.hud;

import meowhack.AddonTemplate;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

public class HudExperience extends HudElement {
    public static final HudElementInfo<HudExperience> INFO = new HudElementInfo<>(AddonTemplate.HUD_GROUP, "Experience Level info", "HUD element.", HudExperience::new);

    public HudExperience() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;

        if (player == null) return;

        int currentLevel = player.experienceLevel;
        int totalExperience = player.totalExperience;
        int experienceForNextLevel = getNextLevelXp(currentLevel);
        int experienceInCurrentLevel = (int) (player.experienceProgress * experienceForNextLevel);
        int remainingExperience = experienceForNextLevel - experienceInCurrentLevel;

        String currentLevelText = "Current Level: " + currentLevel;
        String totalExperienceText = "Total EXP: " + totalExperience;
        String nextLevelText = "EXP for next level: " + remainingExperience + " (" + experienceForNextLevel + ")";

        setSize(Math.max(renderer.textWidth(totalExperienceText, true), renderer.textWidth(nextLevelText, true)), renderer.textHeight(true) * 3);

        // Рендерим текст
        renderer.text(currentLevelText, x, y, Color.WHITE, true);
        renderer.text(totalExperienceText, x, y + renderer.textHeight(true), Color.WHITE, true);
        renderer.text(nextLevelText, x, y + renderer.textHeight(true) * 2, Color.WHITE, true);
    }

    private int getNextLevelXp(int currentLevel) {
        if (currentLevel >= 30) {
            return 112 + (currentLevel - 30) * 9;
        } else if (currentLevel >= 15) {
            return 37 + (currentLevel - 15) * 5;
        } else {
            return 7 + currentLevel * 2;
        }
    }
}
