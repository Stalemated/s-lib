package com.stalemated.lib.network.internal;

//? if >=1.20.5 {
/*import com.stalemated.lib.util.ResourceIds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import static com.stalemated.lib.SLib.MOD_ID;

public record WrapperPayload(ResourceLocation channelId, byte[] data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<WrapperPayload> ID = new CustomPacketPayload.Type<>(ResourceIds.of(MOD_ID, "network"));

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
*///?} elif neoforge && >=1.20.2 {
/*import com.stalemated.lib.util.ResourceIds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import static com.stalemated.lib.SLib.MOD_ID;

public record WrapperPayload(ResourceLocation channelId, byte[] data) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceIds.of(MOD_ID, "network");

    public WrapperPayload(FriendlyByteBuf buf) {
        this(buf.readResourceLocation(), buf.readByteArray());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(this.channelId);
        buf.writeByteArray(this.data);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }
}
*///?}