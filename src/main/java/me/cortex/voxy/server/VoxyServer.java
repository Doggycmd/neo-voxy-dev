package me.cortex.voxy.server;

import me.cortex.voxy.common.Logger;
import me.cortex.voxy.common.network.VoxyNetworkHandler;
import me.cortex.voxy.common.network.VoxyPacketPayload;
import me.cortex.voxy.common.world.WorldEngine;
import me.cortex.voxy.common.world.service.LodStreamingService;
import me.cortex.voxy.commonImpl.VoxyCommon;
import me.cortex.voxy.commonImpl.WorldIdentifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side initialization and management for LOD streaming.
 * <p>
 * Handles:
 * <ul>
 * <li>Server lifecycle events</li>
 * <li>Chunk load events for automatic LOD generation</li>
 * <li>Player sync requests</li>
 * <li>Per-dimension streaming services</li>
 * </ul>
 */
public class VoxyServer {

    // Per-dimension streaming services
    private static final ConcurrentHashMap<ServerLevel, LodStreamingService> streamingServices = new ConcurrentHashMap<>();

    // Current server reference
    private static MinecraftServer currentServer;

    // Is server-side LOD generation enabled
    private static boolean isInitialized = false;

    /**
     * Initialize server-side LOD streaming.
     * Called when the server starts.
     */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        currentServer = event.getServer();

        // Register server-side message handler
        VoxyNetworkHandler.setServerMessageHandler(VoxyServer::handleClientMessage);

        // Create VoxyCommon instance for server-side LOD storage (dedicated server
        // only)
        if (VoxyCommon.IS_DEDICATED_SERVER && VoxyCommon.getInstance() == null) {
            Logger.info("Creating VoxyServerInstance for server-side LOD storage");
            VoxyCommon.setInstanceFactory(VoxyServerInstance::new);
            VoxyCommon.createInstance();
        }

        isInitialized = true;
        Logger.info("VoxyServer initialized - LOD streaming and generation enabled");
    }

    /**
     * Handle chunk load events to generate LOD data from chunks.
     */
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!isInitialized)
            return;

        LevelAccessor level = event.getLevel();
        if (!(level instanceof ServerLevel serverLevel))
            return;

        // Only process full LevelChunks, not proto-chunks
        if (!(event.getChunk() instanceof LevelChunk levelChunk))
            return;

        // Get world identifier for this server level
        WorldIdentifier worldId = WorldIdentifier.of(serverLevel);
        if (worldId == null)
            return;

        // Check if Voxy instance is available
        var instance = VoxyCommon.getInstance();
        if (instance == null)
            return;
        if (!instance.isIngestEnabled(worldId))
            return;

        // Get or create the world engine for this level
        var engine = instance.getOrCreate(worldId);
        if (engine == null)
            return;

        // Ingest the chunk into the LOD system
        try {
            instance.getIngestService().enqueueIngest(engine, levelChunk);
        } catch (Exception e) {
            Logger.error("Failed to ingest server chunk at " + levelChunk.getPos(), e);
        }
    }

    /**
     * Handle messages from clients.
     */
    private static void handleClientMessage(ServerPlayer player, VoxyPacketPayload payload) {
        switch (payload.messageType()) {
            case VoxyPacketPayload.MSG_SYNC_REQUEST -> handleSyncRequest(player);
            case VoxyPacketPayload.MSG_CACHE_RESPONSE -> handleCacheResponse(player, payload);
            case VoxyPacketPayload.MSG_RATE_UPDATE -> handleRateUpdate(player, payload);
            case VoxyPacketPayload.MSG_REQUEST_SECTIONS -> handleSectionRequest(player, payload);
        }
    }

    /**
     * Handle sync request from a player.
     */
    private static void handleSyncRequest(ServerPlayer player) {
        Logger.info("Received sync request from " + player.getName().getString());

        ServerLevel level = player.serverLevel();
        WorldIdentifier worldId = WorldIdentifier.of(level);

        if (worldId == null) {
            Logger.warn("No WorldIdentifier for level " + level.dimension().location());
            VoxyNetworkHandler.sendToPlayer(player,
                    new VoxyPacketPayload(VoxyPacketPayload.MSG_SYNC_COMPLETE, new byte[0]));
            return;
        }

        var instance = VoxyCommon.getInstance();
        if (instance == null) {
            Logger.warn("VoxyCommon instance not available");
            VoxyNetworkHandler.sendToPlayer(player,
                    new VoxyPacketPayload(VoxyPacketPayload.MSG_SYNC_COMPLETE, new byte[0]));
            return;
        }

        WorldEngine engine = instance.getNullable(worldId);
        if (engine == null) {
            Logger.warn("No WorldEngine available for level " + level.dimension().location() +
                    " - no LOD data yet. Chunks must be loaded first.");
            VoxyNetworkHandler.sendToPlayer(player,
                    new VoxyPacketPayload(VoxyPacketPayload.MSG_SYNC_COMPLETE, new byte[0]));
            return;
        }

        // Get or create streaming service for this level
        LodStreamingService service = streamingServices.computeIfAbsent(level,
                l -> {
                    Logger.info("Creating LodStreamingService for " + level.dimension().location());
                    return new LodStreamingService(engine);
                });

        // Actually start the sync for this player
        service.startSyncForPlayer(player);
        Logger.info(
                "LOD streaming started for " + player.getName().getString() + " in " + level.dimension().location());
    }

    /**
     * Handle cache response from client.
     */
    private static void handleCacheResponse(ServerPlayer player, VoxyPacketPayload payload) {
        // Forward to streaming service if exists
        var service = streamingServices.get(player.serverLevel());
        if (service != null) {
            service.handleCacheResponse(player, payload);
        }
    }

    /**
     * Handle rate update from client.
     */
    private static void handleRateUpdate(ServerPlayer player, VoxyPacketPayload payload) {
        // Forward to streaming service if exists
        var service = streamingServices.get(player.serverLevel());
        if (service != null) {
            service.handleRateUpdate(player, payload);
        }
    }

    /**
     * Handle section request from client (pull mode).
     */
    private static void handleSectionRequest(ServerPlayer player, VoxyPacketPayload payload) {
        ServerLevel level = player.serverLevel();
        WorldIdentifier worldId = WorldIdentifier.of(level);

        if (worldId == null) {
            return;
        }

        var instance = VoxyCommon.getInstance();
        if (instance == null) {
            return;
        }

        WorldEngine engine = instance.getNullable(worldId);
        if (engine == null) {
            return;
        }

        // Get or create streaming service for this level
        LodStreamingService service = streamingServices.computeIfAbsent(level,
                l -> {
                    Logger.info("Creating LodStreamingService for " + level.dimension().location());
                    return new LodStreamingService(engine);
                });

        // Forward section request to streaming service
        service.handleSectionRequest(player, payload);
    }

    /**
     * Handle level load events.
     */
    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            Logger.info("Server level loaded: " + serverLevel.dimension().location());
        }
    }

    /**
     * Shutdown all streaming services.
     * Called when the server stops.
     */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        Logger.info("VoxyServer shutting down - closing streaming services");

        isInitialized = false;

        for (var service : streamingServices.values()) {
            try {
                service.close();
            } catch (Exception e) {
                Logger.error("Error closing streaming service: " + e.getMessage());
            }
        }
        streamingServices.clear();

        // Shutdown VoxyCommon instance (dedicated server only)
        // In singleplayer, the client handles the instance lifecycle
        if (VoxyCommon.IS_DEDICATED_SERVER && VoxyCommon.getInstance() != null) {
            Logger.info("Shutting down VoxyServerInstance");
            VoxyCommon.shutdownInstance();
        }

        currentServer = null;
    }

    /**
     * Handle player disconnect to clean up streaming state.
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var level = player.serverLevel();
            var service = streamingServices.get(level);
            if (service != null) {
                service.onPlayerDisconnect(player.getUUID());
            }
            VoxyNetworkHandler.removePlayer(player.getUUID());
        }
    }

    /**
     * Handle server tick for command processing.
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // Tick chunk processors for generate command
        VoxyServerCommands.tickProcessors();
    }

    /**
     * Check if the server is available.
     */
    public static boolean isServerAvailable() {
        return currentServer != null;
    }

    /**
     * Get the current server.
     */
    public static MinecraftServer getServer() {
        return currentServer;
    }

    /**
     * Get streaming service for a level.
     */
    public static LodStreamingService getStreamingService(ServerLevel level) {
        return streamingServices.get(level);
    }

    /**
     * Get all active streaming services.
     */
    public static java.util.Collection<LodStreamingService> getAllStreamingServices() {
        return streamingServices.values();
    }

    /**
     * Broadcast sync request to all players in a level.
     */
    public static void broadcastSync(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            handleSyncRequest(player);
        }
    }
}
