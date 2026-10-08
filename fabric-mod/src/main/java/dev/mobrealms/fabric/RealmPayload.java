package dev.mobrealms.fabric;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Only server-produced snapshots cross the wire; mutations use validated command handlers. */
public record RealmPayload(String json) implements CustomPacketPayload {
    public static final Type<RealmPayload> TYPE=new Type<>(Identifier.parse("mobrealms:dashboard"));
    public static final StreamCodec<RegistryFriendlyByteBuf,RealmPayload> CODEC=StreamCodec.of(
        (buffer,payload)->buffer.writeUtf(payload.json(),200000),buffer->new RealmPayload(buffer.readUtf(200000)));
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
