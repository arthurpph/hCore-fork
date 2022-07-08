package com.hakan.core.npc.entity;

import com.hakan.core.HCore;
import com.hakan.core.npc.HNPC;
import com.hakan.core.npc.pathfinder.PathfinderEntity_v1_8_R3;
import com.hakan.core.npc.skin.HNpcSkin;
import com.hakan.core.renderer.HRenderer;
import com.hakan.core.utils.Validate;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.server.v1_8_R3.DataWatcher;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.MinecraftServer;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntity;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityEquipment;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityHeadRotation;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityMetadata;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityTeleport;
import net.minecraft.server.v1_8_R3.PacketPlayOutNamedEntitySpawn;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerInfo;
import net.minecraft.server.v1_8_R3.PacketPlayOutScoreboardTeam;
import net.minecraft.server.v1_8_R3.PlayerInteractManager;
import net.minecraft.server.v1_8_R3.Scoreboard;
import net.minecraft.server.v1_8_R3.ScoreboardTeam;
import net.minecraft.server.v1_8_R3.ScoreboardTeamBase;
import net.minecraft.server.v1_8_R3.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;

/**
 * {@inheritDoc}
 */
public final class HNpcEntity_v1_8_R3 implements HNpcEntity {

    /**
     * Creates nms player.
     *
     * @param npc HNPC instance.
     * @return nms player.
     */
    private static EntityPlayer createEntityPlayer(HNPC npc) {
        HNpcSkin skin = npc.getSkin();
        Location location = npc.getLocation();
        WorldServer world = ((CraftWorld) npc.getWorld()).getHandle();
        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
        GameProfile gameProfile = new GameProfile(UUID.randomUUID(), npc.getID());
        PlayerInteractManager manager = new PlayerInteractManager(((CraftWorld) npc.getWorld()).getHandle());

        Property property = new Property("textures", skin.getTexture(), skin.getSignature());
        gameProfile.getProperties().clear();
        gameProfile.getProperties().put("textures", property);

        EntityPlayer entityPlayer = new EntityPlayer(server, world, gameProfile, manager);
        entityPlayer.setLocation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        entityPlayer.setInvisible(false);
        entityPlayer.setHealth(77.21f);
        return entityPlayer;
    }


    private final HNPC hnpc;
    private final HRenderer renderer;
    private final ScoreboardTeam scoreboard;
    private EntityPlayer nmsPlayer;

    /**
     * {@inheritDoc}
     */
    public HNpcEntity_v1_8_R3(@Nonnull HNPC hnpc) {
        this.hnpc = Validate.notNull(hnpc, "hnpc cannot be null!");
        this.renderer = this.hnpc.getRenderer();
        this.nmsPlayer = createEntityPlayer(hnpc);
        this.scoreboard = new ScoreboardTeam(new Scoreboard(), hnpc.getID());
        this.scoreboard.setNameTagVisibility(ScoreboardTeamBase.EnumNameTagVisibility.NEVER);
        this.scoreboard.getPlayerNameSet().add(this.nmsPlayer.getName());
        HCore.syncScheduler().after(20).run(this::updateSkin);
        HCore.syncScheduler().after(25).run(this::updateSkin);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getID() {
        return this.nmsPlayer.getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void walk(double speed, @Nonnull Location to, @Nonnull Runnable runnable) {
        PathfinderEntity_v1_8_R3.create(this.hnpc.getLocation(), to, speed,
                (custom) -> this.hnpc.setLocation(custom.getBukkitEntity().getLocation()),
                (custom) -> {
                    custom.dead = true;
                    this.hnpc.setLocation(to);
                    runnable.run();
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateLocation() {
        Location location = this.hnpc.getLocation();
        this.nmsPlayer.setLocation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());

        float yaw = Math.round(location.getYaw() % 360f * (256f / 360f));
        float pitch = Math.round(location.getPitch() % 360f * (256f / 360f));
        HCore.sendPacket(this.renderer.getShownViewersAsPlayer(),
                new PacketPlayOutEntityHeadRotation(this.nmsPlayer, (byte) (location.getYaw() * (256f / 360f))),
                new PacketPlayOutEntity.PacketPlayOutEntityLook(this.getID(), (byte) yaw, (byte) pitch, false),
                new PacketPlayOutEntityTeleport(this.nmsPlayer));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateSkin() {
        List<Player> players = this.renderer.getShownViewersAsPlayer();
        this.hide(players);
        this.nmsPlayer = createEntityPlayer(this.hnpc);
        HCore.syncScheduler().after(20).run(() -> this.show(players));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateEquipments() {
        if (this.hnpc.getEquipments().size() == 0)
            return;

        this.hnpc.getEquipments().forEach((slot, item) -> HCore.sendPacket(this.renderer.getShownViewersAsPlayer(),
                new PacketPlayOutEntityEquipment(this.getID(), slot.getSlot(), CraftItemStack.asNMSCopy(item))));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void show(@Nonnull List<Player> players) {
        DataWatcher dataWatcher = this.nmsPlayer.getDataWatcher();
        dataWatcher.watch(10, (byte) 127);

        players.forEach(player -> this.scoreboard.getPlayerNameSet().add(player.getName()));
        HCore.sendPacket(players,
                new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, this.nmsPlayer),
                new PacketPlayOutNamedEntitySpawn(this.nmsPlayer),
                new PacketPlayOutEntityMetadata(this.getID(), dataWatcher, true),
                new PacketPlayOutScoreboardTeam(this.scoreboard, 0));
        HCore.asyncScheduler().after(5)
                .run(() -> HCore.sendPacket(players, new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, this.nmsPlayer)));

        this.updateEquipments();
        this.updateLocation();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void hide(@Nonnull List<Player> players) {
        players.forEach(player -> this.scoreboard.getPlayerNameSet().remove(player.getName()));
        HCore.sendPacket(players,
                new PacketPlayOutEntityDestroy(this.getID()),
                new PacketPlayOutScoreboardTeam(this.scoreboard, 0));
    }
}