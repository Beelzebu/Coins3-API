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
public final class MultiplierData {

    public static final String SERVER_NAME = "SERVER";
    public static final UUID SERVER_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID enablerUUID;
    private final String enablerName;
    private int amount;
    private int minutes;
    private MultiplierType type;

    public MultiplierData(UUID uniqueId, String name, int amount, int minutes, MultiplierType type) {
        Objects.requireNonNull(name, "Name can't be null");
        Objects.requireNonNull(uniqueId, "UUID can't be null");
        Objects.requireNonNull(type, "MultiplierType can't be null");
        if (type == MultiplierType.PERSONAL && (SERVER_UUID == uniqueId || Objects.equals(SERVER_NAME, name))) {
            throw new IllegalStateException("Can't create personal multiplier for server enabler");
        }
        enablerUUID = uniqueId;
        enablerName = name;
        this.amount = amount;
        this.minutes = minutes;
        this.type = type;
    }

    public MultiplierData(int amount, int minutes, MultiplierType type) {
        this(SERVER_UUID, SERVER_NAME, amount, minutes, type);
    }

    public MultiplierData(int amount, int minutes) {
        this(amount, minutes, MultiplierType.GLOBAL);
    }

    /**
     * Get the UUID of who enabled this multiplier
     *
     * @return UUID of who enabled this multiplier, never should be null unless specified by other plugin.
     */
    public UUID getEnablerUUID() {
        return enablerUUID;
    }

    /**
     * Get the username of who enabled this multiplier
     *
     * @return username of who enabled this multiplier, never should be null unless specified by other plugin.
     */
    public String getEnablerName() {
        return enablerName;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public int getMinutes() {
        return minutes;
    }

    public void setMinutes(int minutes) {
        this.minutes = minutes;
    }

    public MultiplierType getType() {
        return type;
    }

    public void setType(MultiplierType type) {
        Objects.requireNonNull(type, "MultiplierType can't be null");
        this.type = type;
    }

    /**
     * Get if this multiplier was created as the server comparing the name and uuid with {@link #SERVER_NAME} and {@link
     * #SERVER_UUID}.
     *
     * @return <i>true</i> if the uuid and name are equals to server name and uuid, <i>false</i> otherwise.
     */
    public boolean isServerEnabler() {
        return Objects.equals(enablerName, SERVER_NAME) && enablerUUID == SERVER_UUID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MultiplierData that = (MultiplierData) o;
        return getAmount() == that.getAmount() &&
                getMinutes() == that.getMinutes() &&
                getEnablerUUID().equals(that.getEnablerUUID()) &&
                getEnablerName().equals(that.getEnablerName()) &&
                getType() == that.getType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getEnablerUUID(), getEnablerName(), getAmount(), getMinutes(), getType());
    }

    @Override
    public String toString() {
        return "MultiplierData{" +
                "enablerUUID=" + enablerUUID +
                ", enablerName='" + enablerName + '\'' +
                ", amount=" + amount +
                ", minutes=" + minutes +
                ", type=" + type +
                '}';
    }
}
