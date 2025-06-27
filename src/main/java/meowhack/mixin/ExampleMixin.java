package meowhack.mixin;

import meowhack.modules.VanillaFlyHighway;
import meteordevelopment.meteorclient.mixininterface.ICamera;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

    @Mixin(Camera.class)
    abstract class ExampleMixin implements ICamera {
        @ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V"))
        private void onUpdateSetRotationArgs(Args args) {

            VanillaFlyHighway vanillaFly = Modules.get().get(VanillaFlyHighway.class);

            if (vanillaFly.isActive() && vanillaFly.freeLook.get()) {
                //args.set(0, vanillaFly.cameraYaw);
                args.set(1, vanillaFly.cameraPitch);
            }
        }
    }
