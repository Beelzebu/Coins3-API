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

import com.github.beelzebu.coins.api.Multiplier;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

/**
 * @author Beelzebu
 */
public interface CacheProvider {

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
        return getMultipliers().stream().filter(multiplier -> multiplier.canUsePlayer(uniqueId)).collect(Collectors.toSet());
    }

    /**
     * Get a collection of enabled multipliers for the specified server, these multipliers can be global or server
     * specific.
     *
     * @param server Server name to get multipliers.
     * @return {@link Collection<Multiplier>} containing enabled multipliers for the specified server.
     */
    default Collection<Multiplier> getMultipliers(@Nonnull String server) {
        Collection<Multiplier> multipliers = getMultipliers();
        Stream<Multiplier> multiplierStream = multipliers.stream().filter(multiplier -> multiplier.getServer().equals(server));
        if (multiplierStream.anyMatch(Multiplier::isEnabled)) {
            return multiplierStream.filter(Multiplier::isEnabled).collect(Collectors.toSet());
        }
        // TODO: enable server and global multiplier instead of just one queued multiplier.
        Optional<Multiplier> optionalMultiplier = multiplierStream.filter(Multiplier::isQueue).min(Comparator.comparingLong(Multiplier::getQueueStart)); // get first queued multiplier
        if (optionalMultiplier.isPresent()) {
            Multiplier multiplier = optionalMultiplier.get();
            multiplier.enable();
            return Collections.singleton(multiplier);
        }
        return Collections.emptySet();
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
    Set<UUID> getPlayers();

    /**
     * Get {@link CacheType} for the cache in use.
     *
     * @return {@link CacheType} for this provider.
     */
    CacheType getCacheType();
}
