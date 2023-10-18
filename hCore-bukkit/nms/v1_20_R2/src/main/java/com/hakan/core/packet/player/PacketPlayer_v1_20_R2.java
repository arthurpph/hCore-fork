package com.hakan.core.packet.player;

import com.hakan.core.packet.event.PacketEvent;
import com.hakan.core.packet.utils.PacketUtils;
import com.hakan.core.utils.ReflectionUtils;
import com.hakan.core.utils.Validate;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;

/**
 * {@inheritDoc}
 */
public final class PacketPlayer_v1_20_R2 extends PacketPlayer {

    private final PlayerConnection connection;

    /**
     * {@inheritDoc}
     */
    public PacketPlayer_v1_20_R2(@Nonnull Player player) {
        super(player);
        this.connection = ((CraftPlayer) player).getHandle().c;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void send(@Nonnull Object... packets) {
        Validate.notNull(packets, "packets cannot be null!");

        if (!super.player.isOnline())
            return;
        for (Object packet : packets)
            this.connection.b((Packet<?>) packet);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void register() {
        try {
            NetworkManager networkManager = ReflectionUtils.getField(
                    this.connection, ServerCommonPacketListenerImpl.class, "c");
            if (networkManager == null) return;

            super.pipeline = networkManager.n.pipeline().addBefore("packet_handler", CHANNEL + super.player.getUniqueId(), new ChannelDuplexHandler() {
                @Override
                public void channelRead(ChannelHandlerContext channelHandlerContext, Object msg) throws Exception {
                    PacketEvent packetEvent = new PacketEvent(player, msg, PacketEvent.Type.READ);
                    PacketUtils.callEvent(packetEvent);

                    if (packetEvent.isCancelled()) return;
                    super.channelRead(channelHandlerContext, msg);
                }

                @Override
                public void write(ChannelHandlerContext channelHandlerContext, Object o, ChannelPromise channelPromise) throws Exception {
                    PacketEvent packetEvent = new PacketEvent(player, o, PacketEvent.Type.WRITE);
                    PacketUtils.callEvent(packetEvent);

                    if (packetEvent.isCancelled()) return;
                    super.write(channelHandlerContext, o, channelPromise);
                }
            });
        } catch (Exception ignored) {
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregister() {
        if (super.pipeline != null && super.pipeline.get(CHANNEL) != null)
            super.pipeline.remove(CHANNEL + super.player.getUniqueId());
    }
}
