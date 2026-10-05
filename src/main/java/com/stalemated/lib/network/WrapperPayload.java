//? if >=1.20.5 {
/*package com.stalemated.lib.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record WrapperPayload(ResourceLocation channelId, byte[] data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<WrapperPayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("s_lib", "network"));

    public static final StreamCodec<FriendlyByteBuf, WrapperPayload> CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, WrapperPayload::channelId,
            ByteBufCodecs.BYTE_ARRAY, WrapperPayload::data,
            WrapperPayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
*///?}