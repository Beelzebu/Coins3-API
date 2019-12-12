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
import com.github.beelzebu.coins.api.executor.Executor;
import com.github.beelzebu.coins.api.executor.ExecutorManager;
import com.github.beelzebu.coins.api.storage.StorageType;
import com.google.gson.JsonObject;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.UUID;

/**
 * @author Beelzebu
 */
public abstract class AbstractMessagingService {

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
                JsonObject user = new JsonObject();
                user.addProperty("uuid", uuid.toString());
                user.addProperty("coins", coins);
                sendMessage(user, MessageType.USER_UPDATE);
            } catch (Exception ex) {
                CoinsAPI.getPlugin().log("An unexpected error has occurred while updating coins for: " + uuid);
                CoinsAPI.getPlugin().log("Check plugin log files for more information, please report this bug on https://github.com/Beelzebu/Coins3-API/issues");
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
            sendMessage(objectWith("multiplier", multiplier.toJson()), MessageType.MULTIPLIER_UPDATE);
        } catch (Exception ex) {
            CoinsAPI.getPlugin().log("An unexpected error has occurred while publishing a multiplier over messaging service.");
            CoinsAPI.getPlugin().log("Check plugin log files for more information, please report this bug on https://github.com/Beelzebu/Coins3-API/issues");
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
            sendMessage(add(objectWith("multiplier", multiplier.toJson()), "enable", true), MessageType.MULTIPLIER_UPDATE);
        } catch (Exception ex) {
            CoinsAPI.getPlugin().log("An unexpected error has occurred while enabling a multiplier over messaging service.");
            CoinsAPI.getPlugin().log("Check plugin log files for more information, please report this bug on https://github.com/Beelzebu/Coins3-API/issues");
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
            sendMessage(objectWith("multiplier", multiplier.toJson()), MessageType.MULTIPLIER_DISABLE);
        } catch (Exception ex) {
            CoinsAPI.getPlugin().log("An unexpected error has occurred while disabling a multiplier over messaging service.");
            CoinsAPI.getPlugin().log("Check plugin log files for more information, please report this bug on https://github.com/Beelzebu/Coins3-API/issues");
            CoinsAPI.getPlugin().debug(ex);
        }
    }

    /**
     * Send a request to get all multipliers from other servers using this messaging service, if this server is spigot
     * will request it to bungeecord and viceversa.
     */
    public final void requestMultipliers() {
        sendMessage(new JsonObject(), MessageType.MULTIPLIER_REQUEST);
    }

    /**
     * Send a request to get all executors from servers connected to this messaging service.
     */
    public final void requestExecutors() {
        sendMessage(new JsonObject(), MessageType.EXECUTOR_REQUEST);
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
     * @param data what we should send
     * @param type message type
     */
    protected final void sendMessage(JsonObject data, MessageType type) {
        sendMessage(new Message(type, data).toJson());
    }

    protected final void handleMessage(JsonObject data) {
        Message message = CoinsAPI.getPlugin().getGson().fromJson(data, Message.class);
        CoinsAPI.getPlugin().debug("&6Messaging Log: &7Received a new message: " + data);
        MessageType type = message.getType();
        switch (type) {
            case USER_UPDATE: {
                UUID uuid = UUID.fromString(message.getData().get("uuid").getAsString());
                double coins = message.getData().get("coins").getAsDouble();
                CoinsAPI.getPlugin().getBootstrap().callCoinsChangeEvent(uuid, CoinsAPI.getCoins(uuid), coins);
                OptionalDouble optionalCoins = CoinsAPI.getPlugin().getCache().getCoins(uuid);
                if (optionalCoins.isPresent() && optionalCoins.getAsDouble() == coins) {
                    return;
                }
                CoinsAPI.getPlugin().getCache().updatePlayer(uuid, coins);
            }
            break;
            case EXECUTOR_REQUEST: { // other server is requesting executors from this server.
                CoinsAPI.getPlugin().loadExecutors();
                ExecutorManager.getExecutors().forEach(ex -> sendMessage(objectWith("executor", ex.toJson()), MessageType.EXECUTOR_SEND));
            }
            break;
            case EXECUTOR_SEND: { // other server sent an executor
                ExecutorManager.addExecutor(Executor.fromJson(message.getData().get("executor").getAsString()));
            }
            break;
            case MULTIPLIER_REQUEST: { // other server is requesting multipliers from this server
                CoinsAPI.getPlugin().getCache().getMultipliers().forEach(multiplier -> sendMessage(objectWith("multiplier", multiplier.toJson()), MessageType.MULTIPLIER_UPDATE));
            }
            break;
            case MULTIPLIER_UPDATE: {
                Multiplier multiplier = Multiplier.fromJson(message.getData().getAsJsonObject("multiplier").toString());
                if (message.getData().has("enable") && message.getData().get("enable").getAsBoolean()) {
                    CoinsAPI.getPlugin().getBootstrap().callMultiplierEnableEvent(multiplier);
                }
                if (multiplier != null && !CoinsAPI.getPlugin().getCache().getMultiplier(multiplier.getId()).isPresent()) {
                    CoinsAPI.getPlugin().getCache().addMultiplier(multiplier);
                }
            }
            break;
            case MULTIPLIER_DISABLE: { // remove multiplier from cache and storage
                Multiplier multiplier = Multiplier.fromJson(message.getData().getAsJsonObject("multiplier").getAsString());
                if (CoinsAPI.getPlugin().getStorageProvider().getStorageType().equals(StorageType.SQLITE)) {// may be it wasn't removed from this database
                    CoinsAPI.getPlugin().getStorageProvider().deleteMultiplier(multiplier);
                }
                if (multiplier != null && CoinsAPI.getPlugin().getCache().getMultiplier(multiplier.getId()).isPresent()) {
                    CoinsAPI.getPlugin().getCache().deleteMultiplier(multiplier.getId());
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
