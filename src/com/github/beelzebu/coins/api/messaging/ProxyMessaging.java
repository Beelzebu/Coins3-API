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

import com.github.beelzebu.coins.api.CoinsAPI;
import com.google.gson.JsonObject;

/**
 * @author Beelzebu
 */
public abstract class ProxyMessaging extends AbstractMessagingService {

    protected static final String CHANNEL = "coins:updates";

    @Override
    public final MessagingServiceType getType() {
        return MessagingServiceType.BUNGEECORD;
    }

    protected abstract void sendMessage(String message, boolean wait);

    @Override
    protected final void sendMessage(JsonObject jsonObject) {
        Message message = CoinsAPI.getPlugin().getGson().fromJson(jsonObject, Message.class);
        sendMessage(jsonObject.toString(), message.getType() != MessageType.MULTIPLIER_ENABLE);
    }
}
