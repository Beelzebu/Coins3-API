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
package com.github.beelzebu.coins.api.config;

import com.github.beelzebu.coins.api.CoinsAPI;
import com.github.beelzebu.coins.api.cache.CacheType;
import com.github.beelzebu.coins.api.messaging.MessagingServiceType;
import com.github.beelzebu.coins.api.storage.StorageType;
import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * @author Beelzebu
 */
public abstract class CoinsConfig extends AbstractConfigFile {

    public CoinsConfig(File file) {
        super(file);
    }

    // #EasterEgg
    // this can be enabled in minigame arenas where the only transaction is to add coins to players
    public boolean vaultMultipliers() {
        return getBoolean("Vault.Use Multipliers", false);
    }

    public boolean useBungee() {
        return CoinsAPI.getPlugin().getMessagingService().getType().equals(MessagingServiceType.BUNGEECORD);
    }

    public boolean isRedisLoadMultipliers() {
        return getBoolean("Redis.Load Multipliers", true);
    }

    public List<String> getCommandAliases() {
        return getStringList("General.Command.Aliases", Collections.emptyList());
    }

    public String getCommand() {
        return getString("General.Command.Name", "coins");
    }

    public String getServerName() {
        return getString("Multipliers.Server", "default");
    }

    public StorageType getStorageType() {
        StorageType type = StorageType.SQLITE;
        try {
            return StorageType.valueOf(getString("Storage Type", "sqlite").toUpperCase());
        } catch (IllegalArgumentException ex) {
            CoinsAPI.getPlugin().log("You have defined a invalid storage type in the config.");
        }
        return type;
    }

    public MessagingServiceType getMessagingService() {
        MessagingServiceType type = MessagingServiceType.NONE;
        try {
            return MessagingServiceType.valueOf(getString("Messaging Service", "none").toUpperCase());
        } catch (IllegalArgumentException ex) {
            CoinsAPI.getPlugin().log("You have defined a invalid storage type in the config.");
        }
        return type;
    }

    public CacheType getCacheType() {
        CacheType type = CacheType.LOCAL;
        try {
            return CacheType.valueOf(getString("Cache", "local").toUpperCase());
        } catch (IllegalArgumentException ex) {
            CoinsAPI.getPlugin().log("You have defined a invalid cache type in the config.");
        }
        return type;
    }

    public boolean isDebug() {
        return getBoolean("General.Logging.Debug.Enabled", false);
    }

    public boolean isDebugFile() {
        return getBoolean("General.Logging.Debug.File", true);
    }
}
