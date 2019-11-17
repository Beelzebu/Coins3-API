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
import com.github.beelzebu.coins.api.Multiplier;
import com.github.beelzebu.coins.api.cache.CacheType;
import com.github.beelzebu.coins.api.executor.Executor;
import com.github.beelzebu.coins.api.executor.ExecutorManager;
import com.github.beelzebu.coins.api.storage.StorageType;
import com.google.gson.JsonObject;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Beelzebu
 */
public abstract class AbstractMessagingService {

    protected final LinkedHashSet<UUID> messages = new LinkedHashSet<>();

    /**
     * Publish user coins over all servers using this messaging service.
     *
     * @param uuid  user to publish.
     * @param coins coins to publish.
     */
    public void publishUser(UUID uuid, double coins) {
        Objects.requireNonNull(uuid, "UUID can't be null");
        if (coins > -1) {
            try {
                CoinsAPI.getPlugin().getCache().updatePlayer(uuid, coins);
                CoinsAPI.getPlugin().debug("Updated local data for: " + uuid);
                JsonObject user = new JsonObject();
                user.addProperty("uuid", uuid.toString());
                user.addProperty("coins", coins);
                sendMessage(user, MessageType.USER_UPDATE);
            } catch (Exception ex) {
                CoinsAPI.getPlugin().log("An unexpected error has occurred while updating coins for: " + uuid);
                CoinsAPI.getPlugin().log("Check CoinsAPI.getPlugin() log files for more information, please report this bug on https://github.com/Beelzebu/Coins/issues");
                CoinsAPI.getPlugin().debug(ex);
            }
        }
    }

    /**
     * Publish a multiplier over all servers using this messaging service.
     *
     * @param multiplier -
     */
    public final void updateMultiplier(Multiplier multiplier) {
        Objects.requireNonNull(multiplier, "Multiplier can't be null");
        try {
            CoinsAPI.getPlugin().getCache().addMultiplier(multiplier);
            sendMessage(objectWith("multiplier", multiplier.toJson()), MessageType.MULTIPLIER_UPDATE);
        } catch (Exception ex) {
            CoinsAPI.getPlugin().log("An unexpected error has occurred while publishing a multiplier over messaging service.");
            CoinsAPI.getPlugin().log("Check CoinsAPI.getPlugin() log files for more information, please report this bug on https://github.com/Beelzebu/Coins/issues");
            CoinsAPI.getPlugin().debug(ex);
        }
    }

    /**
     * Enable a multiplier in all servers using this messaging service.
     *
     * @param multiplier -
     */
    public final void enableMultiplier(Multiplier multiplier) {
        Objects.requireNonNull(multiplier, "Multiplier can't be null");
        try {
            CoinsAPI.getPlugin().getCache().addMultiplier(multiplier);
            sendMessage(add(objectWith("multiplier", multiplier.toJson()), "enable", true), MessageType.MULTIPLIER_UPDATE);
        } catch (Exception ex) {
            CoinsAPI.getPlugin().log("An unexpected error has occurred while enabling a multiplier over messaging service.");
            CoinsAPI.getPlugin().log("Check CoinsAPI.getPlugin() log files for more information, please report this bug on https://github.com/Beelzebu/Coins/issues");
            CoinsAPI.getPlugin().debug(ex);
        }
    }

    /**
     * Disable a multiplier in all servers using this messaging service.
     *
     * @param multiplier -
     */
    public final void disableMultiplier(Multiplier multiplier) {
        Objects.requireNonNull(multiplier, "Multiplier can't be null");
        try {
            CoinsAPI.getPlugin().getCache().addMultiplier(multiplier);
            sendMessage(objectWith("multiplier", multiplier.toJson()), MessageType.MULTIPLIER_DISABLE);
        } catch (Exception ex) {
            CoinsAPI.getPlugin().log("An unexpected error has occurred while disabling a multiplier over messaging service.");
            CoinsAPI.getPlugin().log("Check CoinsAPI.getPlugin() log files for more information, please report this bug on https://github.com/Beelzebu/Coins/issues");
            CoinsAPI.getPlugin().debug(ex);
        }
    }

    /**
     * Send a request to get all multipliers from other servers using this messaging service, if this server is spigot
     * will request it to bungeecord and viceversa.
     */
    public final void getMultipliers() {
        sendMessage(new JsonObject(), MessageType.MULTIPLIER_UPDATE);
    }

    /**
     * Send a request to get all executors from bungeecord or other bungeecord instances if you're using more than one
     * bungeecord server.
     */
    public final void getExecutors() {
        sendMessage(new JsonObject(), MessageType.GET_EXECUTORS);
    }

    /**
     * Sub classes must override this to send the message so we can handle it before
     *
     * @param message JSON message to send
     */
    protected abstract void sendMessage(JsonObject message);

    /**
     * Send a message in JSON format using this messaging service
     *
     * @param message what we should send
     * @param type    message type
     */
    protected final void sendMessage(JsonObject message, MessageType type) {
        if (getType().equals(MessagingServiceType.NONE)) {
            return;
        }
        UUID uuid = UUID.randomUUID();
        message.addProperty("messageid", uuid.toString());
        message.addProperty("type", type.toString());
        if (CoinsAPI.getPlugin().getCache().getCacheType() == CacheType.REDIS && type != MessageType.GET_EXECUTORS) {
            handleMessage(message);
            return;
        }
        messages.add(uuid);
        sendMessage(message);
    }

    protected final void handleMessage(JsonObject message) {
        UUID messageId = UUID.fromString(message.get("messageid").getAsString());
        if (messages.contains(messageId)) { // the message was sent from this server so don't read it
            messages.remove(messageId);
            return;
        }
        CoinsAPI.getPlugin().debug("&6Messaging Log: &7Received a new message: " + message);
        MessageType type = MessageType.valueOf(message.get("type").getAsString());
        switch (type) {
            case USER_UPDATE: {
                UUID uuid = UUID.fromString(message.get("uuid").getAsString());
                double coins = message.get("coins").getAsDouble();
                // TODO: check if we should update also storage when updating cache across servers
                //CoinsAPI.getPlugin().getStorageProvider().setCoins(uuid, coins);
                CoinsAPI.getPlugin().getCache().updatePlayer(uuid, coins);
            }
            break;
            case GET_EXECUTORS: {
                if (message.has("executor")) {
                    ExecutorManager.addExecutor(Executor.fromJson(message.getAsJsonObject("executor").toString()));
                } else {
                    CoinsAPI.getPlugin().loadExecutors();
                    ExecutorManager.getExecutors().forEach(ex -> sendMessage(objectWith("executor", ex.toJson()), type));
                }
            }
            break;
            case MULTIPLIER_UPDATE: {
                if (message.has("multiplier")) {
                    Multiplier multiplier = Multiplier.fromJson(message.getAsJsonObject("multiplier").toString());
                    CoinsAPI.getPlugin().getCache().addMultiplier(multiplier);
                } else {
                    CoinsAPI.getPlugin().getCache().getMultipliers().forEach(multiplier -> sendMessage(objectWith("multiplier", multiplier.toJson()), type));
                }
            }
            break;
            case MULTIPLIER_DISABLE: {
                Multiplier multiplier = Multiplier.fromJson(message.get("multiplier").getAsString());
                CoinsAPI.getPlugin().getCache().deleteMultiplier(multiplier); // remove from the local cache and storage
                if (CoinsAPI.getPlugin().getStorageProvider().getStorageType().equals(StorageType.SQLITE)) {// may be it wasn't removed from this storageProvider
                    CoinsAPI.getPlugin().getStorageProvider().deleteMultiplier(multiplier);
                }
            }
            break;
        }
    }

    // simple method to use one line lambda expressions when handling messages
    private JsonObject objectWith(String key, JsonObject value) {
        return add(new JsonObject(), key, value);
    }

    private JsonObject add(JsonObject jobj, String key, Object value) {
        jobj.addProperty(key, value.toString());
        return jobj;
    }

    /**
     * Start this messaging service.
     */
    public abstract void start();

    /**
     * Get the messaging service type in use.
     *
     * @return messaging service type defined by implementing classes.
     */
    public abstract MessagingServiceType getType();

    /**
     * Stop and shutdown this messaging service instance.
     */
    public abstract void stop();

}