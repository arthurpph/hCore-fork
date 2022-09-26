package com.hakan.core.ui.sign.wrapper;

import com.hakan.core.HCore;
import com.hakan.core.ui.sign.SignGui;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.IBlockData;
import net.minecraft.server.v1_16_R3.IChatBaseComponent;
import net.minecraft.server.v1_16_R3.PacketPlayInUpdateSign;
import net.minecraft.server.v1_16_R3.PacketPlayOutBlockChange;
import net.minecraft.server.v1_16_R3.PacketPlayOutOpenSignEditor;
import net.minecraft.server.v1_16_R3.TileEntitySign;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.block.CraftSign;
import org.bukkit.craftbukkit.v1_16_R3.util.CraftMagicNumbers;

import javax.annotation.Nonnull;

/**
 * {@inheritDoc}
 */
public final class SignWrapper_v1_16_R3 extends SignWrapper {

    /**
     * {@inheritDoc}
     */
    private SignWrapper_v1_16_R3(@Nonnull SignGui signGui) {
        super(signGui);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void open() {
        Location location = super.signGui.getPlayer().getLocation();
        BlockPosition blockPosition = new BlockPosition(location.getBlockX(), LOWEST_Y_AXIS + 1, location.getBlockZ());

        PacketPlayOutBlockChange packet = new PacketPlayOutBlockChange(((CraftWorld) super.signGui.getPlayer().getWorld()).getHandle(), blockPosition);
        packet.block = CraftMagicNumbers.getBlock(super.signGui.getType().asMaterial()).getBlockData();
        HCore.sendPacket(super.signGui.getPlayer(), packet);

        IChatBaseComponent[] components = CraftSign.sanitizeLines(super.signGui.getLines());
        TileEntitySign sign = new TileEntitySign();
        sign.setPosition(new BlockPosition(blockPosition.getX(), blockPosition.getY(), blockPosition.getZ()));
        System.arraycopy(components, 0, sign.lines, 0, sign.lines.length);
        HCore.sendPacket(super.signGui.getPlayer(), sign.getUpdatePacket());

        HCore.sendPacket(super.signGui.getPlayer(), new PacketPlayOutOpenSignEditor(blockPosition));
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public <T> String[] inputReceive(@Nonnull T packet) {
        PacketPlayInUpdateSign packetPlayInUpdateSign = (PacketPlayInUpdateSign) packet;

        BlockPosition position = packetPlayInUpdateSign.b();
        Block block = super.signGui.getPlayer().getWorld().getBlockAt(position.getX(), position.getY(), position.getZ());
        IBlockData data = CraftMagicNumbers.getBlock(block.getType()).getBlockData();
        HCore.sendPacket(super.signGui.getPlayer(), new PacketPlayOutBlockChange(position, data));

        String[] b = packetPlayInUpdateSign.c();
        String[] lines = new String[b.length];
        System.arraycopy(b, 0, lines, 0, b.length);

        return lines;
    }
}