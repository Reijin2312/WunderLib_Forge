package de.ambertation.wunderlib.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

@FunctionalInterface
public interface PacketSender {
    void sendPacket(ResourceLocation channel, FriendlyByteBuf data);
}
