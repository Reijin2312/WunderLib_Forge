package de.ambertation.wunderlib.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public abstract class ClientBoundPacketHandler<D> {
    private static final Map<ResourceLocation, ClientBoundPacketHandler<?>> HANDLERS = new HashMap<>();

    static {
        WunderLibNetwork.registerClientHandler(ClientBoundPacketHandler::handleMessage);
    }

    protected ResourceLocation CHANNEL;

    public static <D, T extends ClientBoundPacketHandler<D>> T register(ResourceLocation channel, T packetHandler) {
        WunderLibNetwork.ensureInitialized();
        packetHandler.CHANNEL = channel;
        HANDLERS.put(channel, packetHandler);
        packetHandler.onRegister();
        return packetHandler;
    }

    private static void handleMessage(
            WunderLibNetwork.ClientBoundMessage message,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        ClientBoundPacketHandler<?> handler = HANDLERS.get(message.channel());
        if (handler == null) {
            context.setPacketHandled(true);
            return;
        }

        var buf = message.toFriendlyByteBuf();
        @SuppressWarnings("unchecked")
        ClientBoundPacketHandler<Object> typedHandler = (ClientBoundPacketHandler<Object>) handler;
        Object content = typedHandler.deserializeOnClient(buf);

        context.enqueueWork(() -> typedHandler.processOnGameThread(Minecraft.getInstance(), content));
        context.setPacketHandled(true);
    }

    protected abstract D deserializeOnClient(FriendlyByteBuf buf);

    protected abstract void processOnGameThread(Minecraft client, D content);

    protected void onRegister() {
    }
}
