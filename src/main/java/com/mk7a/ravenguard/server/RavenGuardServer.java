package com.mk7a.ravenguard.server;

import com.moandjiezana.toml.Toml;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mk7a.ravenguard.RavenGuardMod.MOD_LIST_CHANNEL;




public class RavenGuardServer implements DedicatedServerModInitializer {


    public static final Logger LOGGER = LogManager.getLogger();

    Set<String> expectedMods = new HashSet<>();
    boolean kickForUnexpectedMods = false;
    Set<String> unexpectedModWhitelist = new HashSet<>();

    private void readConfig() {

        File configFile = FabricLoader.getInstance().getConfigDir().resolve("RavenGuard/RavenGuard.toml").toFile();
        if (!configFile.exists()) {
            LOGGER.info("RavenGuard config file not found, creating...");
            try {
                configFile.getParentFile().mkdirs();
                try (var stream = RavenGuardServer.class.getResourceAsStream("/config/server/RavenGuard.toml")) {
                    Files.copy(stream, configFile.toPath());
                }

            } catch (Exception e) {
                LOGGER.error("Failed to create config file!");
                LOGGER.error(e.getStackTrace());
                return;
            }
        }

        Toml config = new Toml().read(configFile);

        config.getList("expectedMods").forEach(mod -> this.expectedMods.add((String) mod));
        this.kickForUnexpectedMods = config.getBoolean("kickForUnexpectedMods");
        config.getList("unexpectedModWhitelist").forEach(mod -> this.unexpectedModWhitelist.add((String) mod));



    }

    @Override
    public void onInitializeServer() {

        readConfig();


        // Register a listener for the mod list response packet
        ServerLoginNetworking.registerGlobalReceiver(MOD_LIST_CHANNEL, (server, handler, understood, buf, synchronizer, responseSender) -> {

            // Disconnect if the client didn't understand the packet
            if (!understood) {
                handler.disconnect(Text.of("§8[§6RavenGuard§8] §cYour client failed to validate itself."));
                return;
            }

            synchronizer.waitFor(server.submit(() -> {


                String modsStr = buf.readString(32767);
                modsStr = modsStr.replace("[", "").replace("]", "");
                List<String> clientMods = new ArrayList<>(List.of(modsStr.split(", ")));

                var modsFormatted = "[" + clientMods.stream()
                        .map(m -> "\"" + m + "\"")
                        .collect(Collectors.joining(", "))
                        + "]";

                LOGGER.info("[RavenGuard] Player " + handler.getConnectionInfo() + "mods :" + modsFormatted);

                // Filter out expected mods
                var unexpectedMods = clientMods.stream()
                        .filter(mod -> !expectedMods.contains(mod))
                        .filter(mod -> !unexpectedModWhitelist.contains(mod))
                        .toList();

                if (!unexpectedMods.isEmpty()) {
                    LOGGER.warn("[RavenGuard] Unexpected mods: " + unexpectedMods);
                    if (this.kickForUnexpectedMods) {
                        handler.disconnect(Text.of("§8[§6RavenGuard§8] §cKICKED - Your mod list is incompatible. " +
                                "\n§7 Ensure you have the latest unmodified modpack version."));
                    }
                }

                var unexpectedModsWhitelisted = clientMods.stream()
                        .filter(mod -> !expectedMods.contains(mod))
                        .filter(mod -> unexpectedModWhitelist.contains(mod))
                        .toList();
                if (!unexpectedModsWhitelisted.isEmpty()) {
                    LOGGER.info("[RavenGuard] Unexpected mods (whitelisted): " + unexpectedModsWhitelisted);
                }



            }));

        });

        // Register a listener for the login query packet
        ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) -> {
            // Send packet to request mod list
            sender.sendPacket(MOD_LIST_CHANNEL, PacketByteBufs.empty());
        });


    }


}
