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
package com.github.beelzebu.coins.api.storage;

import com.github.beelzebu.coins.api.CoinsResponse;
import com.github.beelzebu.coins.api.CoinsUser;
import com.github.beelzebu.coins.api.Multiplier;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.UUID;
import javax.annotation.Nonnull;

/**
 * @author Beelzebu
 */
public interface StorageProvider {

    int DATABASE_VERSION = 3;

    void setup();

    void shutdown();

    CoinsResponse createPlayer(@Nonnull UUID uuid, @Nonnull String name, double balance);

    CoinsResponse updatePlayer(@Nonnull UUID uuid, @Nonnull String name);

    UUID getUUID(String name);

    String getName(UUID uuid);

    double getCoins(UUID uuid);

    CoinsResponse setCoins(UUID uuid, double balance);

    boolean isindb(UUID uuid);

    boolean isindb(String name);

    LinkedHashSet<CoinsUser> getTopPlayers(int top);

    Multiplier saveMultiplier(Multiplier multiplier) throws RuntimeException;

    Multiplier getMultiplier(int id);

    Collection<Multiplier> getMultipliers();

    Collection<Multiplier> getMultipliers(String server);

    Collection<Multiplier> getMultipliers(String server, boolean enabled);

    Collection<Multiplier> getMultipliersFor(UUID uuid);

    Collection<Multiplier> getMultipliersFor(UUID uuid, boolean enabled);

    Collection<Multiplier> getMultipliersFor(UUID uuid, String server);

    Collection<Multiplier> getMultipliersFor(UUID uuid, String server, boolean enabled);

    void enableMultiplier(Multiplier multiplier);

    void deleteMultiplier(Multiplier multiplier);

    void updateMultiplier(Multiplier multiplier);

    LinkedHashMap<String, Double> getAllPlayers();

    StorageType getStorageType();
}
