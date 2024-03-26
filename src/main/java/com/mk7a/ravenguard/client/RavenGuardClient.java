package com.mk7a.ravenguard.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.PacketByteBuf;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.mk7a.ravenguard.RavenGuardMod.*;

public class RavenGuardClient implements ClientModInitializer {


    @Override
    public void onInitializeClient() {

        List<String> mods = FabricLoader.getInstance().getAllMods().stream()
                .map(modContainer -> modContainer.getMetadata().getName())
                .toList();

        ClientLoginNetworking.registerGlobalReceiver(MOD_LIST_CHANNEL, (client, handler, buf, synchronizer) -> {
            PacketByteBuf responsePacket = PacketByteBufs.create();

            responsePacket.writeString(mods.toString());
            return CompletableFuture.completedFuture(responsePacket);
        });


        ClientLoginNetworking.registerGlobalReceiver(MODPACK_VERSION_CHANNEL, (client, handler, buf, synchronizer) -> {
            PacketByteBuf responsePacket = PacketByteBufs.create();

            responsePacket.writeString(MODPACK_VERSION);
            return CompletableFuture.completedFuture(responsePacket);
        });

    }


}
