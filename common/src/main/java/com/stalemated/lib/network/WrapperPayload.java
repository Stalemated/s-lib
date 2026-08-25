package com.stalemated.lib.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record WrapperPayload(Identifier channelId, byte[] data) implements CustomPayload {
    public static final CustomPayload.Id<WrapperPayload> ID = new CustomPayload.Id<>(Identifier.of("s_lib", "network"));
    
    public static final PacketCodec<PacketByteBuf, WrapperPayload> CODEC = PacketCodec.tuple(
            Identifier.PACKET_CODEC, WrapperPayload::channelId,
            PacketCodecs.BYTE_ARRAY, WrapperPayload::data,
            WrapperPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
