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
package com.github.beelzebu.coins.api.cache;

import com.github.beelzebu.coins.api.CoinsAPI;
import com.github.beelzebu.coins.api.Multiplier;
import com.github.beelzebu.coins.api.MultiplierData;
import com.github.beelzebu.coins.api.MultiplierType;
import com.github.beelzebu.coins.api.plugin.CoinsPlugin;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

/**
 * @author Beelzebu
 */
public interface CacheProvider {

    int POLLER_INTERVAL_SECONDS = 120;

    /**
     * Start and setup all required settings for this {@link CacheProvider} to work
     */
    void start();

    /**
     * Stop using this {@link CacheProvider}, some implementations may will delete all cached when this method is
     * called.
     */
    void stop();

    /**
     * Get the balance of this player from the cache.
     *
     * @param uuid Player UUID to lookup in the cache.
     * @return optional which may or may not contain the coins.
     */
    OptionalDouble getCoins(@Nonnull UUID uuid);

    /**
     * Update balance for a player in cache, if the player doesn't exists in cache, this method will add it.
     *
     * @param uuid  Player UUID to update or add.
     * @param coins Player balance.
     */
    void updatePlayer(@Nonnull UUID uuid, double coins);

    /**
     * Remove a player from this cache.
     *
     * @param uuid Player UUID to remove from cache.
     */
    void removePlayer(@Nonnull UUID uuid);

    /**
     * Get a multiplier from the cache,
     */
    Optional<Multiplier> getMultiplier(int id);

    /**
     * Get all cached and usable multipliers for a player.
     *
     * <p> To check if the multiplier is usable by the player we use {@link Multiplier#canUsePlayer(UUID)} method
     *
     * @param uniqueId Player UUID to lookup.
     * @return {@link Collection<Multiplier>} containing all cached multipliers that the specified player can use.
     */
    default Collection<Multiplier> getUsableMultipliers(UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uniqueId can't be null");
        return getMultipliers().stream().filter(multiplier -> multiplier.canUsePlayer(uniqueId)).collect(Collectors.toSet());
    }

    /**
     * Get a collection of all multipliers for the specified server without any other filter.
     *
     * <p> Please note that {@link Multiplier#getServer()} always return {@link CoinsAPI#getServerName()} as the
     * server when {@link MultiplierType} in {@link MultiplierData)} attached to the multiplier is
     * {@link MultiplierType#GLOBAL}
     *
     * @param server Name of the server to lookup.
     * @return {@link Collection<Multiplier>} containing all cached multipliers that are for the specified server.
     * @see Multiplier#getServer()
     */
    default Collection<Multiplier> getMultipliers(@Nonnull String server) {
        Objects.requireNonNull(server, "Server name can't be null");
        return getMultipliers().stream().filter(multiplier -> multiplier.getServer().equals(server)).collect(Collectors.toSet());
    }

    /**
     * Get a collection of enabled multipliers for the specified server, these multipliers can be global or server
     * specific.
     *
     * @param server Server name to get multipliers.
     * @return {@link Collection<Multiplier>} containing enabled multipliers for the specified server.
     */
    default Collection<Multiplier> getEnabledMultipliers(@Nonnull String server) {
        Objects.requireNonNull(server, "Server name can't be null");
        getMultiplierPoller().checkMultipliersForDisable();
        return getMultipliers().stream().filter(multiplier -> multiplier.getServer().equals(CoinsAPI.getServerName())).filter(Multiplier::isEnabled).collect(Collectors.toSet());
    }

    /**
     * Add or update a multiplier in this cache.
     *
     * @param multiplier Multiplier to load or update in this cache.
     */
    void addMultiplier(@Nonnull Multiplier multiplier);

    /**
     * Delete a multiplier from this cache
     *
     * @param id ID of the multiplier to remove from this cache.
     */
    void deleteMultiplier(int id);

    /**
     * Get all cached multipliers.
     *
     * @return {@link Collection<Multiplier>} containing all cached multipliers.
     */
    Collection<Multiplier> getMultipliers();

    /**
     * Get UUID of all cached players.
     *
     * @return {@link Set<UUID>} containing all UUIDs of cached players.
     */
    Collection<UUID> getPlayers();

    /**
     * Get {@link CacheType} for the cache in use.
     *
     * @return {@link CacheType} for this provider.
     */
    CacheType getCacheType();

    MultiplierPoller getMultiplierPoller();

    class MultiplierPoller implements Runnable {

        private final CoinsPlugin plugin;

        public MultiplierPoller(CoinsPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public void run() {
            loadMultipliersIntoCache();
            checkCachedMultipliers();
        }

        /**
         * Load all multipliers from database and cache multipliers owned by online players, overriding cached
         * multipliers if multiplier from database has any difference.
         */
        public void loadMultipliersIntoCache() {
            Iterator<Multiplier> multipliers = plugin.getStorageProvider().getMultipliers().iterator();
            while (multipliers.hasNext()) {
                Multiplier multiplier = multipliers.next();
                if (plugin.getBootstrap().isOnline(multiplier.getData().getEnablerUUID())) {
                    Optional<Multiplier> cachedMultiplier = plugin.getCache().getMultiplier(multiplier.getId());
                    if (cachedMultiplier.isPresent() && cachedMultiplier.get().equals(multiplier)) {
                        plugin.log("Multiplier #" + multiplier.getId() + " is already cached but multiplier " +
                                "with same id from the database was found and with different data");
                        plugin.log("Overriding cached multiplier #" + multiplier.getId() + " with multiplier from database");
                        plugin.log("Cached multiplier: " + cachedMultiplier.get().toString());
                        plugin.log("Stored multiplier: " + multiplier.toString());
                        plugin.getCache().addMultiplier(multiplier);
                    }
                    if (!cachedMultiplier.isPresent()) {
                        plugin.getCache().addMultiplier(multiplier);
                    }
                    multipliers.remove();
                }
            }
        }

        public void checkCachedMultipliers() {
            checkMultipliersForDisable();
            List<Multiplier> multipliers = new ArrayList<>(plugin.getCache().getMultipliers());
            if (multipliers.isEmpty()) { // there is no enabled multiplier for this server
                multipliers = new ArrayList<>(plugin.getCache().getMultipliers()); // fetch multipliers again
                multipliers = multipliers.stream().filter(Multiplier::isQueue).collect(Collectors.toList());
                multipliers.sort(Comparator.comparingLong(Multiplier::getQueueStart));
                Iterator<Multiplier> it = multipliers.iterator();
                while (it.hasNext()) {
                    Multiplier multiplier = it.next();
                    if (multiplier.canBeEnabled()) { // this method will get all enabled multipliers and check if it can be enabled
                        multiplier.enable();
                    }
                    it.remove();
                }
            }
        }

        public void checkMultipliersForDisable() {
            // create a new set to avoid any ConcurrentModificationException
            new HashSet<>(plugin.getCache().getMultipliers()).stream()
                    .filter(multiplier -> multiplier.getServer().equals(CoinsAPI.getServerName())) // filter multiplier by server
                    .forEach(Multiplier::isEnabled); // check if multiplier is enabled, Multiplier#isEnabled will remove it from cache if is disabled
        }
    }
}
