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

import com.github.beelzebu.coins.api.utils.StringUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Handle Coins multipliers.
 *
 * @author Beelzebu
 */
public final class Multiplier {

    private int id;
    private String server;
    private final MultiplierData data;
    private boolean enabled = false;
    private long start = 0;
    private long queueStart = 0;

    public Multiplier(String server, MultiplierData data) {
        this.data = data;
        this.server = server;
    }

    public int getId() {
        return id;
    }

    private void setId(int id) {
        this.id = id;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public MultiplierData getData() {
        return data;
    }

    public boolean isEnabled() {
        getAndCheckRemainingMillis();
        return enabled;
    }

    public boolean isQueue() {
        return !enabled;
    }

    void setQueue(boolean queue) {
        if (queue) {
            queueStart = System.currentTimeMillis();
        } else {
            queueStart = 0;
        }
        enabled = !queue;
    }

    public long getStart() {
        return start;
    }

    public long getQueueStart() {
        return queueStart;
    }

    public static Multiplier fromJson(String multiplier) {
        Objects.requireNonNull(multiplier, "Tried to load a null Multiplier");
        CoinsAPI.getPlugin().debug("Loading multiplier from JSON: " + multiplier);
        try {
            return CoinsAPI.getPlugin().getGson().fromJson(multiplier, Multiplier.class);
        } catch (JsonSyntaxException ex) {
            CoinsAPI.getPlugin().debug(ex);
        }
        return null;
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean enable() {
        synchronized (this) {
            if (getId() < 0) {
                throw new IllegalStateException("Multiplier has an invalid ID");
            }
            if (isEnabled()) {
                return true;
            }
            if (!data.getType().equals(MultiplierType.PERSONAL) && !CoinsAPI.getMultipliers(CoinsAPI.getPlugin().getConfig().getServerName()).isEmpty()) {
                setQueue(true);
            } else {
                setQueue(false);
            }
            CoinsAPI.getPlugin().getCache().addMultiplier(this);
            if (isEnabled()) {
                start = System.currentTimeMillis();
                enabled = true;
                CoinsAPI.getPlugin().getStorageProvider().enableMultiplier(this);
                CoinsAPI.getPlugin().getMessagingService().enableMultiplier(this);
            }
        }
        return isEnabled();
    }

    /**
     * Disable and then delete this multiplier from the storageProvider.
     */
    public void disable() {
        synchronized (this) {
            if (!enabled) {
                return;
            }
            enabled = false;
            try {
                CoinsAPI.getPlugin().getCache().deleteMultiplier(getId());
                CoinsAPI.getPlugin().getStorageProvider().deleteMultiplier(this);
                CoinsAPI.getPlugin().getMessagingService().disableMultiplier(this);
            } catch (Exception ex) {
                CoinsAPI.getPlugin().log("An unexpected exception has occurred while disabling a multiplier with the id: " + id);
                CoinsAPI.getPlugin().log("Check plugin log files for more information, please report this bug on https://github.com/Beelzebu/Coins/issues");
                CoinsAPI.getPlugin().debug(ex);
            }
        }
    }

    public String getEndTimeFormatted() {
        return StringUtils.formatTime(getAndCheckRemainingMillis());
    }

    /**
     * Get the server for this multiplier, if this multipliers is {@link MultiplierType#GLOBAL} then this server name is
     * returned, otherwise the server specified for this multipliers is returned.
     *
     * @return server for this multiplier in lowercase.
     */
    public String getServer() {
        return data.getType() == MultiplierType.GLOBAL ? CoinsAPI.getPlugin().getConfig().getServerName().toLowerCase() : server.toLowerCase();
    }

    public JsonObject toJson() {
        return CoinsAPI.getPlugin().getGson().toJsonTree(this).getAsJsonObject();
    }

    private long getAndCheckRemainingMillis() {
        if (!isEnabled()) {
            return 0;
        }
        if (System.currentTimeMillis() >= start + TimeUnit.MINUTES.toMillis(data.getMinutes())) {
            disable();
        }
        return System.currentTimeMillis() - start + TimeUnit.MINUTES.toMillis(data.getMinutes()) > 0 ? System.currentTimeMillis() - start + TimeUnit.MINUTES.toMillis(data.getMinutes()) : 0;
    }

    public long getEndTime() {
        getAndCheckRemainingMillis();
        return start + TimeUnit.MINUTES.toMillis(data.getMinutes());
    }

    public boolean canUsePlayer(UUID uniqueId) {
        if (!isEnabled()) {
            return false;
        }
        if (!Objects.equals(CoinsAPI.getPlugin().getConfig().getServerName(), getServer())) {
            return false;
        }
        MultiplierData multiplierData = getData();
        return !multiplierData.getType().equals(MultiplierType.PERSONAL) || multiplierData.getEnablerUUID().equals(uniqueId);
    }

    public Builder toBuilder() {
        return builder().setId(id).setServer(server).setData(data).setEnabled(enabled);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Multiplier that = (Multiplier) o;
        return getId() == that.getId() &&
                isEnabled() == that.isEnabled() &&
                getStart() == that.getStart() &&
                getQueueStart() == that.getQueueStart() &&
                getServer().equals(that.getServer()) &&
                getData().equals(that.getData());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getServer(), getData(), isEnabled(), getStart(), getQueueStart());
    }

    @Override
    public String toString() {
        return "Multiplier{" +
                "id=" + id +
                ", server='" + server + '\'' +
                ", data=" + data +
                ", enabled=" + enabled +
                ", start=" + start +
                ", queueStart=" + queueStart +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private int id = -1;
        private String server;
        private MultiplierData data;
        private boolean enabled;

        public Builder() {
        }

        public Builder setId(int id) {
            this.id = id;
            return this;
        }

        public Builder setServer(String server) {
            this.server = server;
            return this;
        }

        public Builder setData(MultiplierData data) {
            this.data = data;
            return this;
        }

        public Builder setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Multiplier build(boolean callEnable) {
            Multiplier multiplier = new Multiplier(server, data);
            multiplier.setId(id);
            if (server == null && data.getType() == MultiplierType.SERVER) {
                CoinsAPI.getPlugin().log("Multiplier %s, was created with SERVER type but doesn't have a valid server, forcing type to GLOBAL", id);
                multiplier.getData().setType(MultiplierType.GLOBAL);
            }
            if (callEnable) {
                if (!enabled) {
                    throw new IllegalStateException("Can't call enable for a disabled multiplier.");
                }
                multiplier.enable();
            }
            return multiplier;
        }
    }
}
