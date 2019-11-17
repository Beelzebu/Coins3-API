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
import java.util.UUID;

/**
 * @author Beelzebu
 */
public final class CoinsUser {

    private final UUID uniqueId;
    private final String name;
    private final double coins;

    public CoinsUser(UUID uniqueId, String name, double coins) {
        Objects.requireNonNull(uniqueId, "uniqueId can't be null");
        Objects.requireNonNull(name, "name can't be null");
        this.uniqueId = uniqueId;
        this.name = name;
        this.coins = coins;
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public String getName() {
        return name;
    }

    public double getCoins() {
        return coins;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CoinsUser coinsUser = (CoinsUser) o;
        return Double.compare(coinsUser.coins, coins) == 0 &&
                uniqueId.equals(coinsUser.uniqueId) &&
                name.equals(coinsUser.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uniqueId, name, coins);
    }
}
