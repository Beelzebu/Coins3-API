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
package com.github.beelzebu.coins.api;

import java.util.Objects;

/**
 * Class to build multipliers.
 *
 * @author Beelzebu
 * @deprecated internal use only
 */
@Deprecated
public final class MultiplierBuilder {

    private final String server;
    private final MultiplierData data;
    private long endTime = 0L;
    private int id = -1;
    private boolean enabled = false;
    private boolean queue = false;

    private MultiplierBuilder(String server, MultiplierData data) {
        Objects.requireNonNull(server, "Server can't be null.");
        Objects.requireNonNull(data, "MultiplierData can't be null.");
        this.server = server;
        this.data = data;
    }

    public static MultiplierBuilder newBuilder(String server, MultiplierData data) {
        return new MultiplierBuilder(server, data);
    }

    public static Multiplier setId(Multiplier multiplier, int id) {
        multiplier.setId(id);
        return multiplier;
    }

    public MultiplierBuilder setID(int id) {
        this.id = id;
        return this;
    }

    public MultiplierBuilder setEndTime(long endTime) {
        this.endTime = endTime;
        return this;
    }

    public MultiplierBuilder setAmount(int amount) {
        data.setAmount(amount);
        return this;
    }

    public MultiplierBuilder setMinutes(int minutes) {
        data.setMinutes(minutes);
        return this;
    }

    public MultiplierBuilder setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public MultiplierBuilder setQueue(boolean queue) {
        this.queue = queue;
        return this;
    }

    public Multiplier build(boolean callEnable) {
        Multiplier multiplier = new Multiplier(server, data);
        if (server == null && data.getType() == MultiplierType.SERVER) {
            CoinsAPI.getPlugin().log("Multiplier %s, was created with SERVER type but doesn't have a valid server, forcing type to GLOBAL", id);
            multiplier.getData().setType(MultiplierType.GLOBAL);
        }
        multiplier.setId(id);
        multiplier.setQueue(queue);
        multiplier.setEndTime(endTime);
        if (enabled && callEnable) {
            multiplier.enable(queue);
        }
        return multiplier;
    }
}
