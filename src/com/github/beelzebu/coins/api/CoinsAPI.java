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

import com.github.beelzebu.coins.api.cache.CacheProvider;
import com.github.beelzebu.coins.api.plugin.CoinsPlugin;
import com.github.beelzebu.coins.api.utils.CoinsEntry;
import com.github.beelzebu.coins.api.utils.CoinsSet;
import com.github.beelzebu.coins.api.utils.UUIDUtil;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Beelzebu
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class CoinsAPI {

    public static final String API_VERSION = "3.0-SNAPSHOT";
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
        return getCoins(uuid);
    }

    /**
     * Get the coins of a Player by his UUID.
     *
     * @param uuid Player to get the coins.
     * @return coins of the player
     */
    public static double getCoins(@Nonnull UUID uuid) {
        OptionalDouble optionalCoins = PLUGIN.getCache().getCoins(uuid);
        if (!optionalCoins.isPresent()) { // send coins to other servers and cache
            double coins = PLUGIN.getStorageProvider().getCoins(uuid);
            PLUGIN.getMessagingService().publishUser(uuid, coins);
            PLUGIN.getCache().updatePlayer(uuid, coins);
        }
        // try again to get coins from cache, otherwise fallback to database
        return PLUGIN.getCache().getCoins(uuid).orElseGet(() -> {
            PLUGIN.debug("Getting coins from storage for: '" + uuid + "', because player isn't in cache.");
            return PLUGIN.getStorageProvider().getCoins(uuid);
        });
    }

    /**
     * Get the coins String of a player by his name.
     *
     * @param name Player to get the coins string.
     * @return Coins in decimal format "#.#"
     */
    public static String getCoinsString(@Nonnull String name) {
        double coins = getCoins(name);
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
     * <ol>
     *     <li>the plugin implementation will get the UUID associated with the name, first it will try to get it from
     *     online players if the implementation allows it, then from the database if the UUID obtained is null.</li>
     *     <li>{@link #addCoins(UUID, double, boolean)} method will be called with the UUID that we obtained.</li>
     * </ol>
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
        if (multiply && !getUsableMultipliers(uuid).isEmpty()) {
            int multiplyTotal = getUsableMultipliers(uuid).stream().mapToInt(multiplier -> multiplier.getData().getAmount()).sum();
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
        if (uuid.equals(MultiplierData.SERVER_UUID) && coins != 0) {
            return new CoinsResponse(CoinsResponse.CoinsResponseType.FAILED, "Can't set balance for multipliers account to any value different to 0");
        }
        if (isindb(uuid)) {
            if (Double.isNaN(coins) || Double.isInfinite(coins) || new BigDecimal(getCoins(uuid) + coins).compareTo(new BigDecimal(Double.MAX_VALUE)) > 0) {
                PLUGIN.log("An API call tried to exceed the max amount of coins that a account can handle.");
                PLUGIN.log(PLUGIN.getStackTrace(new IllegalArgumentException()));
                return new CoinsResponse(CoinsResponse.CoinsResponseType.FAILED, "Errors.Max value exceeded");
            }
            PLUGIN.getMessagingService().publishUser(uuid, coins, getCoins(uuid));
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
        Objects.requireNonNull(from, "from name can't be null");
        Objects.requireNonNull(to, "to name can't be null");
        UUID fromUUID = UUIDUtil.getUniqueId(from);
        if (fromUUID == null) {
            return new CoinsResponse(CoinsResponse.CoinsResponseType.FAILED, "Errors.Unknown player", "%target%", from);
        }
        UUID toUUID = UUIDUtil.getUniqueId(to);
        if (toUUID == null) {
            return new CoinsResponse(CoinsResponse.CoinsResponseType.FAILED, "Errors.Unknown player", "%target%", to);
        }
        return payCoins(fromUUID, toUUID, amount);
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
        Objects.requireNonNull(from, "from UUID can't be null");
        Objects.requireNonNull(to, "to UUID can't be null");
        if (from == MultiplierData.SERVER_UUID || to == MultiplierData.SERVER_UUID) {
            return new CoinsResponse(CoinsResponse.CoinsResponseType.FAILED, "Can't pay from or to server account.");
        }
        if (getCoins(from) >= amount) {
            CoinsResponse takeResponse = takeCoins(from, amount);
            if (takeResponse.isSuccess()) {
                CoinsResponse addResponse = addCoins(to, amount, false);
                if (addResponse.isSuccess()) {
                    return addResponse;
                } else {
                    PLUGIN.debug("Pay failed in transaction: \n" +
                            "from: '" + from + "' to: '" + to + "' ammount: " + amount + " response: '" + addResponse.getResponse() + ":" + addResponse.getMessage(""));
                    addResponse = addCoins(from, amount, false);
                    PLUGIN.debug("Adding coins back to: '" + from + "' response: " + addResponse.getResponse() + ":" + addResponse.getMessage(""));
                }
            } else {
                return takeResponse;
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
            CACHED_TOP.setKey(new CoinsSet<>(PLUGIN.getStorageProvider().getTopPlayers(top).stream().filter(Objects::nonNull).filter(coinsUser -> !coinsUser.getUniqueId().equals(MultiplierData.SERVER_UUID)).collect(Collectors.toSet())));
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
     * Get a multiplier from the database by his ID and add it to the cache if it wasn't already cached.
     *
     * @param id The ID of the multiplier.
     * @return The multiplier from the Cache.
     */
    public static Multiplier getMultiplier(int id) {
        Optional<Multiplier> optionalMultiplier = PLUGIN.getCache().getMultiplier(id);
        if (optionalMultiplier.isPresent()) {
            return optionalMultiplier.get();
        }
        Multiplier multiplier = PLUGIN.getStorageProvider().getMultiplier(id);
        if (multiplier != null) {
            PLUGIN.getCache().addMultiplier(multiplier);
        }
        return multiplier;
    }

    /**
     * Create a new multiplier for a player and save it.
     *
     * <p> When a multiplier is created his ID is always -1 (which is invalid, so multipliers with this ID can't be
     * enabled and will throw an exception), so this method will save it and the storage provider will set the ID to a
     * valid value.
     *
     * @param uuid    UUID of the player who owns this multiplier.
     * @param amount  Amount for the multiplier.
     * @param minutes Duration for this multiplier.
     * @param server  Server for this multiplier.
     * @param type    Multiplier Type.
     * @return {@link Multiplier} created with the provided data and a valid ID.
     */
    public static Multiplier createMultiplier(UUID uuid, int amount, int minutes, String server, MultiplierType type) {
        MultiplierData multiplierData;
        if (uuid != null) {
            multiplierData = new MultiplierData(uuid, PLUGIN.getName(uuid, false), amount, minutes, type);
        } else {
            multiplierData = new MultiplierData(amount, minutes, type);
        }
        Multiplier multiplier = Multiplier.builder().setServer(server).setData(multiplierData).build(false);
        return PLUGIN.getStorageProvider().saveMultiplier(multiplier);
    }

    /**
     * Create a new multiplier with server owner and save it.
     *
     * <p> {@link MultiplierData} for this multiplier will always return {@link MultiplierData#SERVER_UUID} for
     * {@link MultiplierData#getEnablerUUID()} and {@link MultiplierData#SERVER_NAME} for
     * {@link MultiplierData#getEnablerName()}
     *
     * <p> When a multiplier is created his ID is always -1 (which is invalid, so multipliers with this ID can't be
     * enabled and will throw an exception), so this method will save it and the storage provider will set the ID to a
     * valid value.
     *
     * @param amount  Amount for the multiplier.
     * @param minutes Duration for this multiplier.
     * @param server  Server for this multiplier.
     * @param type    Multiplier Type.
     * @return {@link Multiplier} created with the provided data and a valid ID.
     */
    public static Multiplier createMultiplier(int amount, int minutes, String server, MultiplierType type) {
        return createMultiplier(null, amount, minutes, server, type);
    }

    /**
     * Get all multipliers attached to a server from cache.
     *
     * @param server Server name for the multiplier.
     * @return {@link Collection<Multiplier>} containing all cached multipliers for the specified server from cache.
     * @see Multiplier#getServer()
     */
    public static Collection<Multiplier> getMultipliers(@Nonnull String server) {
        return PLUGIN.getCache().getMultipliers(server);
    }

    /**
     * Get all enabled multipliers associated with the specified server from cache.
     *
     * <p> These multipliers can be any {@link MultiplierType}, this means that {@link MultiplierType#GLOBAL} and
     * {@link MultiplierType#PERSONAL} multipliers will be in the collection too.
     *
     * @param server Server name for the multiplier.
     * @return {@link Collection<Multiplier>} containing all enabled multipliers for the specified server from cache.
     * @see Multiplier#getServer()
     */
    public static Collection<Multiplier> getEnabledMultipliers(@Nonnull String server) {
        Objects.requireNonNull(server, "Server name can't be null");
        return PLUGIN.getCache().getEnabledMultipliers(server);
    }

    /**
     * Get all enabled or disabled multipliers attached to a server from the database.
     *
     * @param server  Server name to search in the database to get multipliers.
     * @param enabled If we should get only enabled multipliers, if false this method will return all multipliers.
     * @return Collection containing all multipliers for the specified server from the database.
     */
    public static Collection<Multiplier> getMultipliers(@Nonnull String server, boolean enabled) {
        if (enabled) {
            return PLUGIN.getCache().getEnabledMultipliers(server);
        } else {
            return PLUGIN.getCache().getMultipliers(server);
        }
    }

    /**
     * Get all usable multipliers for a player in the current server, the purpose of this method is to be used to
     * calculate the total amount to multiply coins when using {@link CoinsAPI#addCoins(UUID, double, boolean)}
     *
     * @param uuid UUID of the player to look for
     * @return {@link Collection<Multiplier>} containing all usable multipliers for this player from cache.
     * @see Multiplier#canUsePlayer(UUID)
     */
    public static Collection<Multiplier> getUsableMultipliers(@Nonnull UUID uuid) {
        return PLUGIN.getCache().getUsableMultipliers(uuid);
    }

    /**
     * Get all multipliers owned by the specified player.
     *
     * @param uuid player to get multipliers from the storageProvider.
     * @return multipliers of the player in this server.
     */
    public static Collection<Multiplier> getMultipliersFor(@Nonnull UUID uuid) {
        return getMultipliersFor(uuid, getServerName());
    }

    /**
     * Get all multipliers owned by a player in a specific server.
     *
     * @param uuid   player to get multipliers from the database.
     * @param server where we should get the multipliers.
     * @return multipliers of the player in that server.
     */
    public static Collection<Multiplier> getMultipliersFor(@Nonnull UUID uuid, @Nullable String server) {
        if (Objects.isNull(server)) {
            return PLUGIN.getCache().getMultipliers().stream().filter(multiplier -> multiplier.getData().getEnablerUUID().equals(uuid)).collect(Collectors.toSet());
        } else {
            return PLUGIN.getCache().getMultipliers().stream().filter(multiplier -> multiplier.getData().getEnablerUUID().equals(uuid)).filter(multiplier -> Objects.equals(multiplier.getServer(), server)).collect(Collectors.toSet());
        }
    }

    /**
     * Get all multipliers for a player, filtering if multipliers must be enabled or not from cache.
     *
     * @param uuid    UUID of the player to get multipliers from the cache.
     * @param server  Server name for the multipliers.
     * @param enabled f we should get only enabled multipliers, if false this method will return all multipliers.
     * @return multipliers of the player in that server.
     */
    public static Collection<Multiplier> getMultipliersFor(@Nonnull UUID uuid, @Nonnull String server, boolean enabled) {
        if (enabled) {
            return PLUGIN.getCache().getEnabledMultipliers(server).stream().filter(multiplier -> multiplier.getData().getEnablerUUID().equals(uuid)).collect(Collectors.toSet());
        } else {
            return getMultipliersFor(uuid, server);
        }
    }

    /**
     * Get name of the current server where the plugin is being executed.
     *
     * @return Server name from {@link CoinsPlugin#getMultipliersConfig()}
     */
    public static String getServerName() {
        Objects.requireNonNull(PLUGIN.getMultipliersConfig(), "multipliers config can't be null");
        return PLUGIN.getMultipliersConfig().getString("Server name", "default").toLowerCase();
    }

    public static CoinsPlugin getPlugin() {
        return PLUGIN;
    }

    public static void setPlugin(@Nonnull CoinsPlugin plugin) {
        if (PLUGIN == null) {
            PLUGIN = plugin;
            plugin.getBootstrap().scheduleAsync(plugin.getCache().getMultiplierPoller(), CacheProvider.POLLER_INTERVAL_SECONDS * 20); // we must multiply it by 20 because interval is in ticks
            CoinsResponse create = createPlayer(MultiplierData.SERVER_NAME, MultiplierData.SERVER_UUID, 0);
            if (create.isFailed()) {
                plugin.log("An error has occurred while creating account for server multipliers in the database.");
                plugin.log(create.getMessage(""));
                return;
            }
            CoinsResponse set = setCoins(MultiplierData.SERVER_UUID, 0);
            if (set.isFailed()) {
                plugin.log("An error has occurred while setting balance to 0 for server multipliers account in the database.");
                plugin.log(set.getMessage(""));
            }
        } else {
            throw new IllegalStateException("Plugin was already set");
        }
    }

    public static void deletePlugin() {
        PLUGIN = null;
    }
}
