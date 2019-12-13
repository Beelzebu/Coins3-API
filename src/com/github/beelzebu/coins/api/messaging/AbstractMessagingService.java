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
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;

/**
 * @author Beelzebu
 */
@SuppressWarnings("unused")
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
                sendMessage(new Message(MessageType.USER_UPDATE, user));
            } catch (Exception ex) {
                CoinsAPI.getPlugin().log("An unexpected error has occurred while updating coins for: " + uuid);
                CoinsAPI.getPlugin().log("Check plugin log files for more information, please report this bug on https://github.com/Beelzebu/Coins3-API/issues");
                CoinsAPI.getPlugin().debug(ex);
            }
        }
    }

    /**
     * Publish user coins update over all servers using this messaging service.
     *
     * @param uuid     user to publish.
     * @param coins    coins to publish.
     * @param oldCoins old user balance.
     */
    public void publishUser(UUID uuid, double coins, double oldCoins) {
        Objects.requireNonNull(uuid, "UUID can't be null");
        if (coins > -1) {
            try {
                JsonObject user = new JsonObject();
                user.addProperty("uuid", uuid.toString());
                user.addProperty("coins", coins);
                user.addProperty("oldCoins", oldCoins);
                sendMessage(new Message(MessageType.USER_UPDATE, user));
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
            sendMessage(new Message(MessageType.MULTIPLIER_UPDATE, objectWith("multiplier", multiplier.toJson())));
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
            sendMessage(new Message(MessageType.MULTIPLIER_UPDATE, add(objectWith("multiplier", multiplier.toJson()), "enable", true)));
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
            sendMessage(new Message(MessageType.MULTIPLIER_DISABLE, objectWith("multiplier", multiplier.toJson())));
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
        sendMessage(new Message(MessageType.MULTIPLIER_REQUEST, new JsonObject()));
    }

    /**
     * Send a request to get all executors from servers connected to this messaging service.
     */
    public final void requestExecutors() {
        sendMessage(new Message(MessageType.EXECUTOR_REQUEST, new JsonObject()));
    }

    /**
     * Sub classes must override this to send the message so we can handle it before
     *
     * @param message JSON message to send
     */
    protected abstract void sendMessage(JsonObject message);

    /**
     * Send a {@link Message} over this messaging service
     *
     * @param message message to send to other servers
     */
    protected final void sendMessage(Message message) {
        if (getType() == MessagingServiceType.NONE) {
            switch (message.getType()) {
                case USER_UPDATE:
                case MULTIPLIER_DISABLE:
                case MULTIPLIER_UPDATE:
                    handleMessage(message.toJson());
            }
            return;
        }
        sendMessage(message);
    }

    protected final void handleMessage(JsonObject data) {
        Message message = CoinsAPI.getPlugin().getGson().fromJson(data, Message.class);
        CoinsAPI.getPlugin().debug("&6Messaging: &7Handling message: " + data);
        switch (message.getType()) {
            case USER_UPDATE: {
                UUID uuid = UUID.fromString(message.getData().get("uuid").getAsString());
                double coins = message.getData().get("coins").getAsDouble();
                double oldCoins = message.getData().has("oldCoins") ? message.getData().get("oldCoins").getAsDouble() : coins;
                if (coins != oldCoins) {
                    CoinsAPI.getPlugin().getBootstrap().callCoinsChangeEvent(uuid, oldCoins, coins);
                }
                OptionalDouble optionalCoins = CoinsAPI.getPlugin().getCache().getCoins(uuid);
                if (optionalCoins.isPresent() && optionalCoins.getAsDouble() == coins) {
                    return;
                }
                CoinsAPI.getPlugin().getCache().updatePlayer(uuid, coins);
            }
            break;
            case EXECUTOR_REQUEST: { // other server is requesting executors from this server.
                CoinsAPI.getPlugin().loadExecutors();
                ExecutorManager.getExecutors().forEach(ex -> sendMessage(new Message(MessageType.EXECUTOR_SEND, objectWith("executor", ex.toJson()))));
            }
            break;
            case EXECUTOR_SEND: { // other server sent an executor
                ExecutorManager.addExecutor(Executor.fromJson(message.getData().get("executor").getAsString()));
            }
            break;
            case MULTIPLIER_REQUEST: { // other server is requesting multipliers from this server
                CoinsAPI.getPlugin().getCache().getMultipliers().forEach(multiplier -> sendMessage(new Message(MessageType.MULTIPLIER_UPDATE, objectWith("multiplier", multiplier.toJson()))));
            }
            break;
            case MULTIPLIER_UPDATE: {
                Multiplier multiplier = Multiplier.fromJson(message.getData().getAsJsonObject("multiplier").toString());
                if (multiplier != null && !CoinsAPI.getPlugin().getCache().getMultiplier(multiplier.getId()).isPresent()) {
                    Optional<Multiplier> optionalMultiplier = CoinsAPI.getPlugin().getCache().getMultiplier(multiplier.getId());
                    if (optionalMultiplier.isPresent() && !optionalMultiplier.get().equals(multiplier)) {
                        CoinsAPI.getPlugin().debug("Received a different version of multiplier: " + multiplier.getId());
                        CoinsAPI.getPlugin().debug("Old multiplier: " + optionalMultiplier.get().toString());
                        CoinsAPI.getPlugin().debug("New multiplier: " + multiplier.toString());
                        CoinsAPI.getPlugin().getCache().addMultiplier(multiplier); // override multiplier since received multiplier is different
                    }
                }
            }
            break;
            case MULTIPLIER_ENABLE: {
                Multiplier multiplier = Multiplier.fromJson(message.getData().getAsJsonObject("multiplier").toString());
                CoinsAPI.getPlugin().getBootstrap().callMultiplierEnableEvent(multiplier);
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
