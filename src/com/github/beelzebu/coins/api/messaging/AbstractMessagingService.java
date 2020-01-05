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
import com.github.beelzebu.coins.api.plugin.CoinsBootstrap;
import com.github.beelzebu.coins.api.plugin.CoinsPlugin;
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

    protected final CoinsPlugin<? extends CoinsBootstrap> coinsPlugin;

    public AbstractMessagingService(CoinsPlugin<? extends CoinsBootstrap> coinsPlugin) {
        this.coinsPlugin = coinsPlugin;
    }

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
                coinsPlugin.log("An unexpected error has occurred while updating coins for: " + uuid);
                coinsPlugin.log("Check plugin log files for more information, please report this bug on https://github.com/Beelzebu/Coins3-API/issues");
                coinsPlugin.debug(ex);
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
        if (coins == oldCoins) {
            publishUser(uuid, coins);
            return;
        }
        if (coins > -1) {
            try {
                JsonObject user = new JsonObject();
                user.addProperty("uuid", uuid.toString());
                user.addProperty("coins", coins);
                user.addProperty("oldCoins", oldCoins);
                sendMessage(new Message(MessageType.USER_UPDATE, user));
            } catch (Exception ex) {
                coinsPlugin.log("An unexpected error has occurred while updating coins for: " + uuid);
                coinsPlugin.log("Check plugin log files for more information, please report this bug on https://github.com/Beelzebu/Coins3-API/issues");
                coinsPlugin.debug(ex);
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
            coinsPlugin.log("An unexpected error has occurred while publishing a multiplier over messaging service.");
            coinsPlugin.log("Check plugin log files for more information, please report this bug on https://github.com/Beelzebu/Coins3-API/issues");
            coinsPlugin.debug(ex);
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
            sendMessage(new Message(MessageType.MULTIPLIER_ENABLE, objectWith("multiplier", multiplier.toJson())));
        } catch (Exception ex) {
            coinsPlugin.log("An unexpected error has occurred while enabling a multiplier over messaging service.");
            coinsPlugin.log("Check plugin log files for more information, please report this bug on https://github.com/Beelzebu/Coins3-API/issues");
            coinsPlugin.debug(ex);
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
            coinsPlugin.log("An unexpected error has occurred while disabling a multiplier over messaging service.");
            coinsPlugin.log("Check plugin log files for more information, please report this bug on https://github.com/Beelzebu/Coins3-API/issues");
            coinsPlugin.debug(ex);
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
     * @param jsonObject JSON message to send
     */
    protected abstract void sendMessage(JsonObject jsonObject);

    /**
     * Send a {@link Message} over this messaging service
     *
     * @param message message to send to other servers
     */
    protected final void sendMessage(Message message) {
        if (getType() == MessagingServiceType.NONE) {
            switch (message.getType()) {
                case MULTIPLIER_REQUEST:
                case EXECUTOR_REQUEST:
                case EXECUTOR_SEND:
                    break;
                default:
                    handleMessage(message.toJson());
                    break;
            }
            return;
        }
        sendMessage(message.toJson());
    }

    /**
     * Handle received messages to update cache and call events.
     *
     * @param jsonObject JSON message received in the messaging service implementation.
     */
    protected final void handleMessage(JsonObject jsonObject) {
        Message message = CoinsPlugin.GSON.fromJson(jsonObject, Message.class);
        coinsPlugin.debug("&6Messaging: &7Handling message: " + jsonObject);
        switch (message.getType()) {
            case USER_UPDATE: {
                UUID uuid = UUID.fromString(message.getData().get("uuid").getAsString());
                double coins = message.getData().get("coins").getAsDouble();
                double oldCoins = message.getData().has("oldCoins") ? message.getData().get("oldCoins").getAsDouble() : coins;
                if (coins != oldCoins) {
                    coinsPlugin.getBootstrap().callCoinsChangeEvent(uuid, oldCoins, coins);
                }
                OptionalDouble optionalCoins = coinsPlugin.getCache().getCoins(uuid);
                if (optionalCoins.isPresent() && optionalCoins.getAsDouble() == coins) {
                    return;
                }
                coinsPlugin.getCache().updatePlayer(uuid, coins);
            }
            break;
            case EXECUTOR_REQUEST: { // other server is requesting executors from this server.
                coinsPlugin.loadExecutors();
                ExecutorManager.getExecutors().forEach(ex -> sendMessage(new Message(MessageType.EXECUTOR_SEND, objectWith("executor", ex.toJson()))));
            }
            break;
            case EXECUTOR_SEND: { // other server sent an executor
                ExecutorManager.addExecutor(Executor.fromJson(message.getData().get("executor").getAsString()));
            }
            break;
            case MULTIPLIER_REQUEST: { // other server is requesting multipliers from this server
                coinsPlugin.getCache().getMultipliers().forEach(multiplier -> sendMessage(new Message(MessageType.MULTIPLIER_UPDATE, objectWith("multiplier", multiplier.toJson()))));
            }
            break;
            case MULTIPLIER_UPDATE: {
                Multiplier multiplier = Multiplier.fromJson(message.getData().getAsJsonObject("multiplier").toString());
                if (multiplier != null) {
                    Optional<Multiplier> optionalMultiplier = coinsPlugin.getCache().getMultiplier(multiplier.getId());
                    if (optionalMultiplier.isPresent()) {
                        if (optionalMultiplier.get().equals(multiplier)) {
                            return;
                        }
                        coinsPlugin.debug("Received a different version of multiplier: " + multiplier.getId());
                        coinsPlugin.debug("Old multiplier: " + optionalMultiplier.get().toString());
                        coinsPlugin.debug("New multiplier: " + multiplier.toString());
                    }
                    coinsPlugin.getCache().addMultiplier(multiplier); // override multiplier since received multiplier is different
                }
            }
            break;
            case MULTIPLIER_ENABLE: {
                Multiplier multiplier = Multiplier.fromJson(message.getData().getAsJsonObject("multiplier").toString());
                if (multiplier != null) {
                    coinsPlugin.getBootstrap().callMultiplierEnableEvent(CoinsAPI.getMultiplier(multiplier.getId()));
                } else {
                    coinsPlugin.debug("Received a null multiplier from messaging service");
                }
            }
            break;
            case MULTIPLIER_DISABLE: { // remove multiplier from cache and storage
                Multiplier multiplier = Multiplier.fromJson(message.getData().getAsJsonObject("multiplier").getAsString());
                if (coinsPlugin.getStorageProvider().getStorageType().equals(StorageType.SQLITE)) {// may be it wasn't removed from this database
                    coinsPlugin.getStorageProvider().deleteMultiplier(multiplier);
                }
                if (multiplier != null && coinsPlugin.getCache().getMultiplier(multiplier.getId()).isPresent()) {
                    coinsPlugin.getCache().deleteMultiplier(multiplier.getId());
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
