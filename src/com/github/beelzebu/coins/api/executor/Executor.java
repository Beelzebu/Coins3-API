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
package com.github.beelzebu.coins.api.executor;

import com.github.beelzebu.coins.api.CoinsAPI;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.List;

/**
 * @author Beelzebu
 */
public class Executor {

    private final String id;
    private final String displayname;
    private final double cost;
    private final List<String> commands;

    public Executor(String id, String displayname, double cost, List<String> commands) {
        this.id = id;
        this.displayname = displayname;
        this.cost = cost;
        this.commands = commands;
    }

    public String getId() {
        return id;
    }

    public String getDisplayname() {
        return displayname;
    }

    public double getCost() {
        return cost;
    }

    public List<String> getCommands() {
        return commands;
    }

    public static JsonObject toJson(Executor ex) {
        return CoinsAPI.getPlugin().getGson().toJsonTree(ex).getAsJsonObject();
    }

    public static Executor fromJson(String json) throws JsonParseException {
        return CoinsAPI.getPlugin().getGson().fromJson(json, Executor.class);
    }

    public JsonObject toJson() {
        return toJson(this);
    }
}
