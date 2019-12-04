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

public class Message {

    private final MessageType type;
    private final JsonObject data;

    public Message(MessageType type, JsonObject data) {
        this.type = type;
        this.data = data;
    }

    public MessageType getType() {
        return type;
    }

    public JsonObject getData() {
        return data;
    }

    public JsonObject toJson() {
        return CoinsAPI.getPlugin().getGson().toJsonTree(this).getAsJsonObject();
    }
}
