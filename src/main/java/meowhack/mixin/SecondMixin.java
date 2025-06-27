package meowhack.mixin;

import meowhack.modules.VanillaFlyHighway;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(Entity.class)
public abstract class SecondMixin {

    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void updateChangeLookDirection(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        if ((Object) this != mc.player) return;

        VanillaFlyHighway vanillaFly = Modules.get().get(VanillaFlyHighway.class);

        if (vanillaFly.isActive() && vanillaFly.freeLook.get()) {
            //vanillaFly.cameraYaw += (float) (cursorDeltaX / vanillaFly.sensitivity.get().floatValue());
            vanillaFly.cameraPitch += (float) (cursorDeltaY / 8.0F);

            if (Math.abs(vanillaFly.cameraPitch) > 90.0F) vanillaFly.cameraPitch = vanillaFly.cameraPitch > 0.0F ? 90.0F : -90.0F;
            // ci.cancel();
        }
    }
}
