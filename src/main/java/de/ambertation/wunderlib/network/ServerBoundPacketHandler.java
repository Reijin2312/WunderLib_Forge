package de.ambertation.wunderlib.network;

import de.ambertation.wunderlib.WunderLib;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

import io.netty.buffer.Unpooled;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public abstract class ServerBoundPacketHandler<D> {
    private static final Map<ResourceLocation, ServerBoundPacketHandler<?>> HANDLERS = new HashMap<>();

    protected ResourceLocation CHANNEL;

    public static <D, T extends ServerBoundPacketHandler<D>> T register(ResourceLocation channel, T packetHandler) {
        WunderLibNetwork.ensureInitialized();
        packetHandler.CHANNEL = channel;
        HANDLERS.put(channel, packetHandler);
        packetHandler.onRegister();
        return packetHandler;
    }

    public void sendToServer(D content) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            serializeOnClient(buf, content);
            WunderLibNetwork.sendToServer(CHANNEL, buf);
        } else {
            WunderLib.LOGGER.warn("Ignoring sendToServer on dedicated server for channel {}", CHANNEL);
        }
    }

    static void handleMessage(
            WunderLibNetwork.ServerBoundMessage message,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player == null) {
            context.setPacketHandled(true);
            return;
        }

        ServerBoundPacketHandler<?> handler = HANDLERS.get(message.channel());
        if (handler == null) {
            context.setPacketHandled(true);
            return;
        }

        FriendlyByteBuf buf = message.toFriendlyByteBuf();
        PacketSender responseSender = (replyChannel, replyBuf) ->
                WunderLibNetwork.sendToClient(player, replyChannel, replyBuf);

        @SuppressWarnings("unchecked")
        ServerBoundPacketHandler<Object> typedHandler = (ServerBoundPacketHandler<Object>) handler;
        Object content = typedHandler.deserializeOnServer(buf, player, responseSender);

        context.enqueueWork(() -> {
            MinecraftServer server = player.getServer();
            if (server != null) {
                typedHandler.processOnGameThread(server, player, content);
            }
        });
        context.setPacketHandled(true);
    }

    protected abstract void serializeOnClient(FriendlyByteBuf buf, D content);

    protected abstract D deserializeOnServer(FriendlyByteBuf buf, ServerPlayer player, PacketSender responseSender);

    protected abstract void processOnGameThread(MinecraftServer server, ServerPlayer player, D content);

    protected void onRegister() {
    }
}
