package com.hakan.message.title;

import com.hakan.HCore;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import org.bukkit.entity.Player;

public class TitleHandler_v1_18_R1 implements TitleHandler {

    @Override
    public void send(Player player, HTitle hTitle) {
        IChatBaseComponent titleString = IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + hTitle.getTitle() + "\"}");
        IChatBaseComponent subtitleString = IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + hTitle.getSubtitle() + "\"}");

        HCore.sendPacket(player, new ClientboundSetTitlesAnimationPacket(hTitle.getFadeIn(), hTitle.getStay(), hTitle.getFadeOut()));
        HCore.sendPacket(player, new ClientboundSetTitleTextPacket(titleString));
        HCore.sendPacket(player, new ClientboundSetSubtitleTextPacket(subtitleString));
    }
}