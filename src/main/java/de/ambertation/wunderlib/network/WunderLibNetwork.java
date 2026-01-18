package de.ambertation.wunderlib.network;

import de.ambertation.wunderlib.WunderLib;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import io.netty.buffer.Unpooled;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

final class WunderLibNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static final int SERVERBOUND_ID = 0;
    private static final int CLIENTBOUND_ID = 1;

    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(WunderLib.ID("network"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    private static volatile BiConsumer<ClientBoundMessage, Supplier<NetworkEvent.Context>> clientHandler =
            (message, contextSupplier) -> {
                NetworkEvent.Context context = contextSupplier.get();
                context.setPacketHandled(true);
            };

    static {
        CHANNEL.registerMessage(
                SERVERBOUND_ID,
                ServerBoundMessage.class,
                ServerBoundMessage::encode,
                ServerBoundMessage::decode,
                ServerBoundPacketHandler::handleMessage,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                CLIENTBOUND_ID,
                ClientBoundMessage.class,
                ClientBoundMessage::encode,
                ClientBoundMessage::decode,
                WunderLibNetwork::handleClientbound,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    private WunderLibNetwork() {
    }

    static void ensureInitialized() {
        // Trigger class load and message registration.
    }

    static void registerClientHandler(
            BiConsumer<ClientBoundMessage, Supplier<NetworkEvent.Context>> handler
    ) {
        clientHandler = handler;
    }

    static void sendToServer(ResourceLocation channel, FriendlyByteBuf buf) {
        CHANNEL.sendToServer(new ServerBoundMessage(channel, toByteArray(buf)));
    }

    static void sendToClient(ServerPlayer player, ResourceLocation channel, FriendlyByteBuf buf) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ClientBoundMessage(channel, toByteArray(buf)));
    }

    private static void handleClientbound(
            ClientBoundMessage message,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        clientHandler.accept(message, contextSupplier);
    }

    private static byte[] toByteArray(FriendlyByteBuf buf) {
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        return data;
    }

    static final class ServerBoundMessage {
        private final ResourceLocation channel;
        private final byte[] data;

        private ServerBoundMessage(ResourceLocation channel, byte[] data) {
            this.channel = channel;
            this.data = data;
        }

        ResourceLocation channel() {
            return channel;
        }

        FriendlyByteBuf toFriendlyByteBuf() {
            return new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
        }

        static void encode(ServerBoundMessage message, FriendlyByteBuf buf) {
            buf.writeResourceLocation(message.channel);
            buf.writeByteArray(message.data);
        }

        static ServerBoundMessage decode(FriendlyByteBuf buf) {
            ResourceLocation channel = buf.readResourceLocation();
            byte[] data = buf.readByteArray();
            return new ServerBoundMessage(channel, data);
        }
    }

    static final class ClientBoundMessage {
        private final ResourceLocation channel;
        private final byte[] data;

        private ClientBoundMessage(ResourceLocation channel, byte[] data) {
            this.channel = channel;
            this.data = data;
        }

        ResourceLocation channel() {
            return channel;
        }

        FriendlyByteBuf toFriendlyByteBuf() {
            return new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
        }

        static void encode(ClientBoundMessage message, FriendlyByteBuf buf) {
            buf.writeResourceLocation(message.channel);
            buf.writeByteArray(message.data);
        }

        static ClientBoundMessage decode(FriendlyByteBuf buf) {
            ResourceLocation channel = buf.readResourceLocation();
            byte[] data = buf.readByteArray();
            return new ClientBoundMessage(channel, data);
        }
    }
}
