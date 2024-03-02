package com.hakan.core.npc.entity;

import com.hakan.core.HCore;
import com.hakan.core.npc.Npc;
import com.hakan.core.skin.Skin;
import com.hakan.core.utils.ReflectionUtils;
import com.hakan.core.utils.Validate;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.datafixers.util.Pair;
import joptsimple.internal.Reflection;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.PacketPlayOutAnimation;
import net.minecraft.network.protocol.game.PacketPlayOutEntity;
import net.minecraft.network.protocol.game.PacketPlayOutEntityDestroy;
import net.minecraft.network.protocol.game.PacketPlayOutEntityEquipment;
import net.minecraft.network.protocol.game.PacketPlayOutEntityHeadRotation;
import net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata;
import net.minecraft.network.protocol.game.PacketPlayOutEntityTeleport;
import net.minecraft.network.protocol.game.PacketPlayOutScoreboardTeam;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EnumGamemode;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.ScoreboardTeam;
import net.minecraft.world.scores.ScoreboardTeamBase;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R2.CraftServer;
import org.bukkit.craftbukkit.v1_20_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R2.inventory.CraftItemStack;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * {@inheritDoc}
 */
public final class NpcEntity_v1_20_R2 implements NpcEntity {
    private Field getField(Class<?> clazz, Class<?> fieldType, int index) throws NoSuchFieldException {
        Field[] fields = clazz.getDeclaredFields();
        int count = 0;
        for (Field field : fields) {
            if (field.getType().isAssignableFrom(fieldType) && !Modifier.isStatic(field.getModifiers())) {
                if (count == index) {
                    return field;
                }
                count++;
            }
        }
        throw new NoSuchFieldException("Field not found");
    }

    /**
     * Creates nms player.
     *
     * @param npc Npc instance.
     * @return nms player.
     */
    @Nonnull
    private static EntityPlayer createEntityPlayer(@Nonnull Npc npc) {
        Validate.notNull(npc, "npc cannot be null");

        Skin skin = npc.getSkin();
        Location location = npc.getLocation();
        WorldServer world = ((CraftWorld) npc.getWorld()).getHandle();
        DedicatedServer server = ((CraftServer) Bukkit.getServer()).getServer();
        GameProfile gameProfile = new GameProfile(UUID.randomUUID(), npc.getID());

        Property property = new Property("textures", skin.getTexture(), skin.getSignature());
        gameProfile.getProperties().clear();
        gameProfile.getProperties().put("textures", property);

        EntityPlayer entityPlayer = new EntityPlayer(server, world, gameProfile, ClientInformation.a());
        entityPlayer.a(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        entityPlayer.persistentInvisibility = false; //set invisibility to true
        entityPlayer.b(5, true); //set invisibility to true
        entityPlayer.c(77.21f); //set health to 77.21f
        return entityPlayer;
    }

    private void setValue(Object packet, String fieldName, Object value) {
        try {
            Field field = packet.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(packet, value);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }


    private final Npc npc;
    private final EntityPlayer nmsPlayer;
    private final ScoreboardTeam scoreboard;

    /**
     * {@inheritDoc}
     */
    public NpcEntity_v1_20_R2(@Nonnull Npc npc) {
        this.npc = Validate.notNull(npc, "npc cannot be null!");
        this.nmsPlayer = createEntityPlayer(npc);
        this.scoreboard = new ScoreboardTeam(new Scoreboard(), npc.getID());
        this.scoreboard.a(ScoreboardTeamBase.EnumNameTagVisibility.b);
        this.scoreboard.g().add(npc.getID());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getID() {
        return this.nmsPlayer.ah();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void playAnimation(@Nonnull List<Player> players, @Nonnull Npc.Animation animation) {
        Validate.notNull(players, "players cannot be null");
        HCore.sendPacket(players, new PacketPlayOutAnimation(this.nmsPlayer, animation.getId()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateLocation(@Nonnull List<Player> players) {
        this.updateHeadRotation(Validate.notNull(players, "players cannot be null!"));
        HCore.sendPacket(players, new PacketPlayOutEntityTeleport(this.nmsPlayer));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateHeadRotation(@Nonnull List<Player> players) {
        Validate.notNull(players, "players cannot be null!");

        Location location = this.npc.getLocation();
        this.nmsPlayer.a(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());

        float yaw = Math.round(location.getYaw() % 360f * (256f / 360f));
        float pitch = Math.round(location.getPitch() % 360f * (256f / 360f));
        HCore.sendPacket(players, new PacketPlayOutEntityHeadRotation(this.nmsPlayer, (byte) (location.getYaw() * (256f / 360f))),
                new PacketPlayOutEntity.PacketPlayOutEntityLook(this.getID(), (byte) yaw, (byte) pitch, false));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateSkin(@Nonnull List<Player> players) {
        Validate.notNull(players, "players cannot be null!");

        this.hide(players);

        GameProfile gameProfile = this.nmsPlayer.fQ();
        gameProfile.getProperties().get("textures").clear();
        gameProfile.getProperties().put("textures", new Property("textures", this.npc.getSkin().getTexture(), this.npc.getSkin().getSignature()));

        this.show(players);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateEquipments(@Nonnull List<Player> players) {
        Validate.notNull(players, "players cannot be null!");

        if (this.npc.getEquipments().isEmpty())
            return;

        List<Pair<EnumItemSlot, ItemStack>> equipmentList = new ArrayList<>();
        this.npc.getEquipments().forEach((key, value) -> equipmentList.add(new Pair<>(EnumItemSlot.valueOf(key.name()), CraftItemStack.asNMSCopy(value))));
        HCore.sendPacket(players, new PacketPlayOutEntityEquipment(this.getID(), equipmentList));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void show(@Nonnull List<Player> players) {
        Validate.notNull(players, "players cannot be null!");

        GameProfile gameProfile = this.nmsPlayer.fQ();
        DataWatcher dataWatcher = this.nmsPlayer.al();
        gameProfile.getProperties().get("textures").clear();
        gameProfile.getProperties().put("textures", new Property("textures", this.npc.getSkin().getTexture(), this.npc.getSkin().getSignature()));
        dataWatcher.b(new DataWatcherObject<>(10, DataWatcherRegistry.b), 0);
        dataWatcher.b(new DataWatcherObject<>(17, DataWatcherRegistry.a), (byte) 127);

        ClientboundPlayerInfoUpdatePacket infoAdd = null;

        try {
            Field entriesField = this.getField(ClientboundPlayerInfoUpdatePacket.class, List.class, 0);
            entriesField.setAccessible(true);

            EnumSet<ClientboundPlayerInfoUpdatePacket.a> actions = EnumSet.noneOf(ClientboundPlayerInfoUpdatePacket.a.class);
            actions.add(ClientboundPlayerInfoUpdatePacket.a.a);
            actions.add(ClientboundPlayerInfoUpdatePacket.a.e);

            ClientboundPlayerInfoUpdatePacket.b entry = new ClientboundPlayerInfoUpdatePacket.b(gameProfile.getId(), gameProfile, true, 0,
                    EnumGamemode.b, this.nmsPlayer.ab(), null);

            infoAdd = new ClientboundPlayerInfoUpdatePacket(
                    actions, Collections.EMPTY_LIST);

            entriesField.set(infoAdd, Collections.singletonList(entry));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        players.forEach(player -> this.scoreboard.g().add(player.getName()));
        HCore.sendPacket(players,
                infoAdd,
                new PacketPlayOutEntityMetadata(this.getID(), dataWatcher.c()),
                new PacketPlayOutSpawnEntity(this.nmsPlayer),
                PacketPlayOutScoreboardTeam.a(this.scoreboard, true));
        ClientboundPlayerInfoUpdatePacket finalInfoAdd = infoAdd;
        HCore.asyncScheduler().after(5)
                .run(() -> HCore.sendPacket(players, finalInfoAdd));

        HCore.asyncScheduler().after(2)
                .run(() -> this.updateLocation(players));
        this.updateEquipments(players);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void hide(@Nonnull List<Player> players) {
        Validate.notNull(players, "players cannot be null!");

        players.forEach(player -> this.scoreboard.g().remove(player.getName()));
        HCore.sendPacket(players, new PacketPlayOutEntityDestroy(this.getID()),
                PacketPlayOutScoreboardTeam.a(this.scoreboard, true));
    }
}
