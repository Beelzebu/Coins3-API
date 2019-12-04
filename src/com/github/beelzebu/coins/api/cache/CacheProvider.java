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

    void start();

    void stop();

    /**
     * Get the coins of this player from the cache.
     *
     * @param uuid player to lookup in the cache.
     * @return optional which may or may not contain the coins.
     */
    OptionalDouble getCoins(@Nonnull UUID uuid);

    void updatePlayer(@Nonnull UUID uuid, double coins);

    void removePlayer(@Nonnull UUID uuid);

    /**
     * Get a multiplier from the cache,
     */
    Optional<Multiplier> getMultiplier(int id);

    default Collection<Multiplier> getUsableMultipliers(UUID uniqueId) {
        return getMultipliers().stream().filter(multiplier -> multiplier.canUsePlayer(uniqueId)).collect(Collectors.toSet());
    }

    default Collection<Multiplier> getMultipliers(@Nonnull String server) {
        Collection<Multiplier> multipliers = getMultipliers();
        Stream<Multiplier> multiplierStream = multipliers.stream().filter(multiplier -> multiplier.getServer().equals(server));
        if (multiplierStream.anyMatch(Multiplier::isEnabled)) {
            return multiplierStream.filter(Multiplier::isEnabled).collect(Collectors.toSet());
        }
        Optional<Multiplier> optionalMultiplier = multiplierStream.filter(Multiplier::isQueue).min(Comparator.comparingLong(Multiplier::getQueueStart)); // get first queued multiplier
        if (optionalMultiplier.isPresent()) {
            Multiplier multiplier = optionalMultiplier.get();
            multiplier.enable();
            return Collections.singleton(multiplier);
        }
        return Collections.emptySet();
    }

    void addMultiplier(@Nonnull Multiplier multiplier);

    void deleteMultiplier(int id);

    void updateMultiplier(@Nonnull Multiplier multiplier, boolean callenable);

    Collection<Multiplier> getMultipliers();

    Set<UUID> getPlayers();

    CacheType getCacheType();

}
