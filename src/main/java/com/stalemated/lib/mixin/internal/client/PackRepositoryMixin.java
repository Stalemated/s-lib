package com.stalemated.lib.mixin.internal.client;

import com.stalemated.lib.client.internal.resource.RegisteredClientPackSource;
import com.stalemated.lib.mixin.RequiresMod;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@RequiresMod("fabricloader")
@Mixin(PackRepository.class)
public abstract class PackRepositoryMixin {

    @Shadow
    @Final
    @Mutable
    private Set<RepositorySource> sources;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void s_lib$addClientPacks(RepositorySource[] repositorySources, CallbackInfo ci) {
        sources = RegisteredClientPackSource.withClientPacks(sources);
    }
}
