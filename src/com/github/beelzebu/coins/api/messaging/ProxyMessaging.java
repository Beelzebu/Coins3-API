/*
 * This file is part of Coins3
 *
 * Copyright © 2019 Beelzebu
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.beelzebu.coins.api.messaging;

import com.github.beelzebu.coins.api.plugin.CoinsPlugin;
import com.google.gson.JsonObject;

/**
 * @author Beelzebu
 */
public abstract class ProxyMessaging extends AbstractMessagingService {

    /**
     * Messaging channel to register in bungeecord and bukkit.
     */
    protected static final String CHANNEL = "coins:updates";

    public ProxyMessaging(CoinsPlugin coinsPlugin) {
        super(coinsPlugin);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final MessagingServiceType getType() {
        return MessagingServiceType.BUNGEECORD;
    }

    /**
     * Send a message through this messaging service, using {@link #CHANNEL}.
     *
     * <p> Since bungeecord and bukkit messaging channels need a player connected to send the message we must specify if
     * we want to the server to wait until there is a player online to try to send the message or just skip it.
     *
     * <p> Currently only {@link MessageType#MULTIPLIER_ENABLE} message is skipped when there is no online player, this
     * is handled in {@link #sendMessage(JsonObject)} implementation.
     *
     * @param message message to send through this messaging service.
     * @param wait    if we should wait for a player to join and handle this message when there is no player in the
     *                server sending this message.
     */
    protected abstract void sendMessage(String message, boolean wait);

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void sendMessage(JsonObject jsonObject) {
        Message message = coinsPlugin.getGson().fromJson(jsonObject, Message.class);
        sendMessage(jsonObject.toString(), message.getType() != MessageType.MULTIPLIER_ENABLE);
    }
}
