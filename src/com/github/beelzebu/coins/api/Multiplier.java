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

/**
 * Handle Coins multipliers.
 *
 * @author Beelzebu
 */
public final class Multiplier {

    private int id;
    private String server;
    private MultiplierData data;
    private boolean enabled = false;
    private boolean queue = false;
    private boolean custom = false;
    private long endTime = 0;

    public Multiplier(String server, MultiplierData data) {
        this.data = data;
        this.server = server;
    }

    public int getId() {
        return id;
    }

    void setId(int id) {
        this.id = id;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public MultiplierData getData() {
        return data;
    }

    public void setData(MultiplierData data) {
        this.data = data;
    }

    public boolean isEnabled() {
        return enabled;
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isQueue() {
        return queue;
    }

    void setQueue(boolean queue) {
        this.queue = queue;
    }

    public boolean isCustom() {
        return custom;
    }

    void setCustom(boolean custom) {
        this.custom = custom;
    }

    public long getEndTime() {
        return endTime;
    }

    void setEndTime(long endTime) {
        this.endTime = endTime;
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

    /**
     * Enable this multiplier with the data from {@link #getData()}
     *
     * @param queue if the multiplier should be queued or immediately enabled.
     */
    public void enable(boolean queue) {
        if (System.currentTimeMillis() + data.getMinutes() * 60000 > endTime && endTime > 1) {
            return;
        }
        enabled = true;
        endTime = System.currentTimeMillis() + data.getMinutes() * 60000;
        if (queue && (CoinsAPI.getMultipliers(server).isEmpty() || CoinsAPI.getMultipliers().stream().noneMatch(Multiplier::isEnabled)) || !data.getType().equals(MultiplierType.SERVER)) {
            queue = false;
        }
        this.queue = queue;
        if (!queue) {
            CoinsAPI.getPlugin().getCache().addMultiplier(this);
            CoinsAPI.getPlugin().getStorageProvider().enableMultiplier(this);
            CoinsAPI.getPlugin().getMessagingService().enableMultiplier(this);
            CoinsAPI.getPlugin().getBootstrap().callMultiplierEnableEvent(this);
        } else {
            CoinsAPI.getPlugin().getCache().addQueueMultiplier(this);
        }
    }

    /**
     * Disable and then delete this multiplier from the storageProvider.
     */
    public void disable() {
        try {
            CoinsAPI.getPlugin().getCache().removeQueueMultiplier(this);
            CoinsAPI.getPlugin().getCache().deleteMultiplier(this);
            CoinsAPI.getPlugin().getStorageProvider().deleteMultiplier(this);
            CoinsAPI.getPlugin().getMessagingService().disableMultiplier(this);
        } catch (Exception ex) {
            CoinsAPI.getPlugin().log("An unexpected exception has occurred while disabling a multiplier with the id: " + id);
            CoinsAPI.getPlugin().log("Check plugin log files for more information, please report this bug on https://github.com/Beelzebu/Coins/issues");
            CoinsAPI.getPlugin().debug(ex);
        }
    }

    public String getMultiplierTimeFormatted() {
        return StringUtils.formatTime(checkMultiplierTime());
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

    private long checkMultiplierTime() {
        if (endTime - System.currentTimeMillis() <= 0) {
            disable();
        }
        return endTime - System.currentTimeMillis() >= 0 ? endTime - System.currentTimeMillis() : 0;
    }
}
