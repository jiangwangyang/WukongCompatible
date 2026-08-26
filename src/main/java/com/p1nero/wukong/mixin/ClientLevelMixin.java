package com.p1nero.wukong.mixin;

import com.p1nero.wukong.epicfight.weapon.WukongWeaponCategories;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    private static final ResourceLocation EFR_AFTERIMAGE =
            new ResourceLocation("cdmoveset", "corrupt_after_image");

    @Inject(
            method = "addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void wukong$suppressEfrAfterimage(
            ParticleOptions particle,
            double x,
            double y,
            double z,
            double encodedEntityId,
            double ySpeed,
            double zSpeed,
            CallbackInfo ci
    ) {
        ResourceLocation particleId = ForgeRegistries.PARTICLE_TYPES.getKey(particle.getType());
        if (!EFR_AFTERIMAGE.equals(particleId)) {
            return;
        }

        long decodedId = Double.doubleToRawLongBits(encodedEntityId);
        if (decodedId < Integer.MIN_VALUE || decodedId > Integer.MAX_VALUE) {
            return;
        }

        Entity entity = ((ClientLevel) (Object) this).getEntity((int) decodedId);
        if (entity instanceof LivingEntity livingEntity && WukongWeaponCategories.isWeaponValid(livingEntity)) {
            ci.cancel();
        }
    }
}
