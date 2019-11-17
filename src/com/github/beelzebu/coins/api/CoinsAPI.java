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

import com.github.beelzebu.coins.api.plugin.CoinsPlugin;
import com.github.beelzebu.coins.api.utils.CoinsEntry;
import com.github.beelzebu.coins.api.utils.CoinsSet;
import com.github.beelzebu.coins.api.utils.UUIDUtil;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

/**
 * @author Beelzebu
 */
public final class CoinsAPI {

    private static final DecimalFormat DF = new DecimalFormat("#.#");
    private static final CoinsEntry<CoinsSet<CoinsUser>, Long> CACHED_TOP = new CoinsEntry<>(new CoinsSet<>(), -1L);
    private static final long TOP_CACHE_MILLIS = 30000;
    private static CoinsPlugin PLUGIN = null;

    private CoinsAPI() {
    }

    /**
     * Get the coins of a Player by his name.
     *
     * @param name Player to get the coins.
     * @return coins of the player
     */
    public static double getCoins(@Nonnull String name) {
        UUID uuid = UUIDUtil.getUniqueId(name);
        if (uuid == null) {
            return -1;
        }
        return PLUGIN.getCache().getCoins(uuid).orElseGet(() -> PLUGIN.getStorageProvider().getCoins(UUIDUtil.getUniqueId(name)));
    }

    /**
     * Get the coins of a Player by his UUID.
     *
     * @param uuid Player to get the coins.
     * @return coins of the player
     */
    public static double getCoins(@Nonnull UUID uuid) {
        return PLUGIN.getCache().getCoins(uuid).orElseGet(() -> PLUGIN.getStorageProvider().getCoins(uuid));
    }

    /**
     * Get the coins String of a player by his name.
     *
     * @param name Player to get the coins string.
     * @return Coins in decimal format "#.#"
     */
    public static String getCoinsString(@Nonnull String name) {
        double coins = getCoins(name.toLowerCase());
        if (coins >= 0) {
            return DF.format(coins);
        } else {
            return PLUGIN.getString("Errors.Unknown player", "").replace("%target%", name);
        }
    }

    /**
     * Get the coins String of a player by his name.
     *
     * @param uuid Player to get the coins string.
     * @return Coins in decimal format "#.#"
     */
    public static String getCoinsString(@Nonnull UUID uuid) {
        double coins = getCoins(uuid);
        if (coins >= 0) {
            return DF.format(coins);
        } else {
            return PLUGIN.getString("Errors.Unknown player", "").replace("%target%", uuid.toString());
        }
    }

    /**
     * Add coins to a player by his name, selecting if the multipliers should be used to calculate the coins. When this
     * method is called the following things will happen:
     * <ul>
     *     <li>the plugin implementation will get the UUID associated with the name, first it will try to get it from
     *     online players if the implementation allows it, then from the database if the UUID obtained is null.</li>
     *     <li>{@link #addCoins(UUID, double, boolean)} method will be called with the UUID that we obtained.</li>
     * </ul>
     *
     * @param name     Player name to add the coins.
     * @param coins    Coins to add.
     * @param multiply Multiply coins if there are any active multipliers
     * @return {@link CoinsResponse} Containing all data for this operation.
     */
    public static CoinsResponse addCoins(@Nonnull String name, double coins, boolean multiply) {
        return addCoins(UUIDUtil.getUniqueId(name), coins, multiply);
    }

    /**
     * Add coins to a player by his UUID, selecting if the multipliers should be used to calculate the coins.
     *
     * @param uuid     Player UUID to add the coins.
     * @param coins    Coins to add.
     * @param multiply Multiply coins if there are any active multipliers
     * @return {@link CoinsResponse} Containing all data for this operation.
     */
    public static CoinsResponse addCoins(@Nonnull UUID uuid, double coins, boolean multiply) {
        if (!isindb(uuid)) {
            return new CoinsResponse(CoinsResponse.CoinsResponseType.FAILED, "Errors.Unknown player", "%target%", uuid.toString());
        }
        double finalCoins = coins;
        if (multiply && !getMultipliers().isEmpty()) {
            int multiplyTotal = getMultipliers().stream().filter(Multiplier::isEnabled).mapToInt(multiplier -> {
                if (multiplier.getData().getType().equals(MultiplierType.PERSONAL) && !Objects.equals(uuid, multiplier.getData().getEnablerUUID())) {
                    return 0;
                }
                return multiplier.getData().getAmount();
            }).sum();
            finalCoins *= Math.max(multiplyTotal, 1);
            for (String perm : PLUGIN.getBootstrap().getPermissions(uuid)) {
                if (perm.startsWith("coins.multiplier.x")) {
                    try {
                        int i = Integer.parseInt(perm.split("coins.multiplier.x")[1]);
                        finalCoins *= i;
                        break;
                    } catch (NumberFormatException ignore) {
                    }
                }
            }
        }
        finalCoins += getCoins(uuid);
        return setCoins(uuid, finalCoins);
    }

    /**
     * Take coins of a player by his name.
     *
     * @param name  The name of the player to take the coins.
     * @param coins Coins to take from the player.
     * @return {@link CoinsResponse}
     */
    public static CoinsResponse takeCoins(@Nonnull String name, double coins) {
        return setCoins(UUIDUtil.getUniqueId(name), getCoins(name) - coins);
    }

    /**
     * Take coins of a player by his UUID.
     *
     * @param uuid  The UUID of the player to take the coins.
     * @param coins Coins to take from the player.
     * @return {@link CoinsResponse}
     */
    public static CoinsResponse takeCoins(@Nonnull UUID uuid, double coins) {
        return setCoins(uuid, getCoins(uuid) - coins);
    }

    /**
     * Reset the coins of a player by his name.
     *
     * @param name The name of the player to reset the coins.
     * @return {@link CoinsResponse}
     */
    public static CoinsResponse resetCoins(@Nonnull String name) {
        return setCoins(UUIDUtil.getUniqueId(name), PLUGIN.getConfig().getDouble("General.Starting Coins", 0));
    }

    /**
     * Reset the coins of a player by his UUID.
     *
     * @param uuid The UUID of the player to reset the coins.
     * @return {@link CoinsResponse}
     */
    public static CoinsResponse resetCoins(@Nonnull UUID uuid) {
        return setCoins(uuid, PLUGIN.getConfig().getDouble("General.Starting Coins", 0));
    }

    /**
     * Set the coins of a player by his name.
     *
     * @param name  The name of the player to set the coins.
     * @param coins Coins to set.
     * @return {@link CoinsResponse}
     */
    public static CoinsResponse setCoins(@Nonnull String name, double coins) {
        return setCoins(UUIDUtil.getUniqueId(name), coins);
    }

    /**
     * Set the coins of a player by his name.
     *
     * @param uuid  The UUID of the player to set the coins.
     * @param coins Coins to set.
     * @return {@link CoinsResponse}
     */
    public static CoinsResponse setCoins(@Nonnull UUID uuid, double coins) {
        if (isindb(uuid)) {
            if (Double.isNaN(coins) || Double.isInfinite(coins) || new BigDecimal(coins).compareTo(new BigDecimal(Double.MAX_VALUE)) > 0) {
                PLUGIN.log("An API call tried to exceed the max amount of coins that a account can handle.");
                PLUGIN.log(PLUGIN.getStackTrace(new IllegalArgumentException()));
                if (getCoins(uuid) < coins) {
                    return new CoinsResponse(CoinsResponse.CoinsResponseType.FAILED, "Errors.No Coins");
                }
                return new CoinsResponse(CoinsResponse.CoinsResponseType.FAILED, "Errors.Max value exceeded");
            }
            PLUGIN.getBootstrap().callCoinsChangeEvent(uuid, getCoins(uuid), coins);
            PLUGIN.getMessagingService().publishUser(uuid, coins);
            return PLUGIN.getStorageProvider().setCoins(uuid, coins);
        } else {
            return new CoinsResponse(CoinsResponse.CoinsResponseType.FAILED, "Errors.Unknown player", "%target%", uuid.toString());
        }
    }

    /**
     * Pay coins to another player.
     *
     * @param from   The player to get the coins.
     * @param to     The player to pay.
     * @param amount The amount of coins to pay.
     * @return {@link CoinsResponse}
     */
    public static CoinsResponse payCoins(@Nonnull String from, @Nonnull String to, double amount) {
        if (getCoins(from) >= amount) {
            takeCoins(from, amount);
            addCoins(to, amount, false);
            return CoinsResponse.SUCCESS;
        }
        return new CoinsResponse(CoinsResponse.CoinsResponseType.FAILED, "Errors.No Coins");
    }

    /**
     * Pay coins to another player.
     *
     * @param from   The player to get the coins.
     * @param to     The player to pay.
     * @param amount The amount of coins to pay.
     * @return {@link CoinsResponse}
     */
    public static CoinsResponse payCoins(@Nonnull UUID from, @Nonnull UUID to, double amount) {
        if (getCoins(from) >= amount) {
            if (takeCoins(from, amount).isSuccess() && addCoins(to, amount, false).isSuccess()) {
                return CoinsResponse.SUCCESS;
            }
        }
        return new CoinsResponse(CoinsResponse.CoinsResponseType.FAILED, "Errors.No Coins");
    }

    /**
     * Get if a player with the specified name exists in the storageProvider. Is not recommended check a player by his
     * name because it can change.
     *
     * @param name The name to look for in the storageProvider.
     * @return true if the player exists in the storageProvider or false if not.
     */
    public static boolean isindb(@Nonnull String name) {
        UUID uuid = UUIDUtil.getUniqueId(name);
        if (uuid != null && PLUGIN.getCache().getCoins(uuid).isPresent()) { // If the player is in the cache it should be in the storageProvider.
            return true;
        }
        boolean exists = PLUGIN.getStorageProvider().isindb(name);
        if (!exists && PLUGIN.getBootstrap().isOnline(name)) {
            return createPlayer(name, uuid).isSuccess();
        }
        return exists;
    }

    /**
     * Get if a player with the specified uuid exists in the storageProvider.
     *
     * @param uuid The uuid to look for in the storageProvider.
     * @return true if the player exists in the storageProvider or false if not.
     */
    public static boolean isindb(@Nonnull UUID uuid) {
        if (PLUGIN.getCache().getCoins(uuid).isPresent()) { // If the player is in the cache it should be in the storageProvider.
            return true;
        }
        boolean exists = PLUGIN.getStorageProvider().isindb(uuid);
        if (!exists && PLUGIN.getBootstrap().isOnline(uuid)) {
            return createPlayer(PLUGIN.getName(uuid, false), uuid).isSuccess();
        }
        return exists;
    }

    /**
     * Get the top players from the cache or the database, this method will try to get the top from the cache, if the
     * cache is older than {@link CoinsAPI#TOP_CACHE_MILLIS}, then it will try to get it from the database and update
     * the cache, if the amount of cached players is less than the requested players it will get the players from the
     * database too.
     *
     * @param top The length of the top list, for example "5" will get a max of 5 users for the top.
     * @return Array with the requested players.
     */
    public static CoinsUser[] getTopPlayers(int top) {
        if (CACHED_TOP.getValue() != null && CACHED_TOP.getValue() >= System.currentTimeMillis() && CACHED_TOP.getKey().size() <= top) {
            return CACHED_TOP.getKey().getFirst(top).toArray(new CoinsUser[top]);
        } else {
            CACHED_TOP.setKey(new CoinsSet<>(PLUGIN.getStorageProvider().getTopPlayers(top)));
            CACHED_TOP.setValue(System.currentTimeMillis() + TOP_CACHE_MILLIS);
            return CACHED_TOP.getKey().toArray(new CoinsUser[top]);
        }
    }

    /**
     * Register a user in the storageProvider with the default starting balance.
     *
     * @param name The name of the user that will be registered.
     * @param uuid The uuid of the user.
     */
    public static CoinsResponse createPlayer(@Nonnull String name, UUID uuid) {
        return createPlayer(name, uuid, PLUGIN.getConfig().getDouble("General.Starting Coins", 0));
    }

    /**
     * Register a user in the storageProvider with the specified balance.
     *
     * @param name    The name of the user that will be registered.
     * @param uuid    The uuid of the user.
     * @param balance The balance of the user.
     */
    public static CoinsResponse createPlayer(@Nonnull String name, UUID uuid, double balance) {
        return PLUGIN.getStorageProvider().createPlayer(uuid, name, balance);
    }

    /**
     * Get all enabled multipliers for this server.
     *
     * @return The active multiplier for this server.
     */
    public static Set<Multiplier> getMultipliers() {
        return getMultipliers(PLUGIN.getConfig().getString("Multipliers.Server", "default"));
    }

    public static Set<Multiplier> getMultipliers(MultiplierFilter filter) {
        return getMultipliers().stream().filter(filter.getPredicate()).collect(Collectors.toSet());
    }

    /**
     * Get all enabled multipliers in this server.
     *
     * @param server The server to modify and get info about multiplier.
     * @return The active multiplier for the specified server can be null;
     */
    public static Set<Multiplier> getMultipliers(@Nonnull String server) {
        return PLUGIN.getCache().getMultipliers(server);
    }

    /**
     * Get a multiplier from the storageProvider by his ID and add it to the cache.
     *
     * @param id The ID of the multiplier.
     * @return The multiplier from the Cache.
     */
    public static Multiplier getMultiplier(int id) {
        return PLUGIN.getCache().getMultiplier(id).orElse(PLUGIN.getStorageProvider().getMultiplier(id));
    }

    /**
     * Get all multipliers for a player from the storageProvider.
     *
     * @param uuid player to get the multipliers from the storageProvider.
     * @return all multipliers that this player have.
     */
    public static Set<Multiplier> getAllMultipliersFor(@Nonnull UUID uuid) {
        return PLUGIN.getStorageProvider().getMultipliers(uuid);
    }

    /**
     * Get all the multipliers for a player in the current server.
     *
     * @param uuid player to get multipliers from the storageProvider.
     * @return multipliers of the player in this server.
     */
    public static Set<Multiplier> getMultipliersFor(@Nonnull UUID uuid) {
        return PLUGIN.getStorageProvider().getMultipliers(uuid, PLUGIN.getConfig().getServerName());
    }

    /**
     * Get all multipliers for a player in the specified server.
     *
     * @param uuid   player to get multipliers from the storageProvider.
     * @param server where we should get the multipliers.
     * @return multipliers of the player in that server.
     */
    public static Set<Multiplier> getMultipliersFor(@Nonnull UUID uuid, @Nonnull String server) {
        return PLUGIN.getStorageProvider().getMultipliers(uuid, server);
    }

    public static Multiplier createMultiplier(UUID uuid, int amount, int minutes, String server, MultiplierType type) {
        Multiplier multiplier = MultiplierBuilder.newBuilder(server, new MultiplierData(uuid, PLUGIN.getName(uuid, false), amount, minutes, type)).build(false);
        PLUGIN.getStorageProvider().saveMultiplier(multiplier);
        return multiplier;
    }

    public static Multiplier createMultiplier(int amount, int minutes, String server, MultiplierType type) {
        Multiplier multiplier = MultiplierBuilder.newBuilder(server, new MultiplierData(amount, minutes, type)).build(false);
        PLUGIN.getStorageProvider().saveMultiplier(multiplier);
        return multiplier;
    }

    public static CoinsPlugin getPlugin() {
        return PLUGIN;
    }

    public static void setPlugin(@Nonnull CoinsPlugin plugin) {
        if (PLUGIN == null) {
            PLUGIN = plugin;
        } else {
            throw new IllegalStateException("Plugin was already set");
        }
    }

    public static void deletePlugin() {
        PLUGIN = null;
    }
}