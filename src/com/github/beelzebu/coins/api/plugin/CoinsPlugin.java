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
package com.github.beelzebu.coins.api.plugin;

import com.github.beelzebu.coins.api.cache.CacheProvider;
import com.github.beelzebu.coins.api.cache.CacheType;
import com.github.beelzebu.coins.api.config.AbstractConfigFile;
import com.github.beelzebu.coins.api.config.CoinsConfig;
import com.github.beelzebu.coins.api.messaging.AbstractMessagingService;
import com.github.beelzebu.coins.api.messaging.MessagingServiceType;
import com.github.beelzebu.coins.api.storage.StorageProvider;
import com.github.beelzebu.coins.api.storage.StorageType;
import com.google.gson.Gson;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * @author Beelzebu
 */
public interface CoinsPlugin {

    void load();

    void enable();

    void reload();

    void disable();

    CoinsBootstrap getBootstrap();

    CacheProvider getCache();

    void setCache(CacheProvider cacheProvider);

    void setCacheType(CacheType cacheType);

    StorageProvider getStorageProvider();

    void setStorageProvider(StorageProvider storageProvider);

    void setStorageType(StorageType storageType);

    AbstractMessagingService getMessagingService();

    void setMessagingService(AbstractMessagingService messagingService);

    void setMessagingServiceType(MessagingServiceType messagingServiceType);

    Gson getGson();

    void loadExecutors();

    void log(String message, Object... replace);

    void debug(String message, Object... replace);

    void debug(Exception ex);

    void debug(SQLException ex);

    String getStackTrace(Exception e);

    UUID getUniqueId(String name, boolean fromdb);

    String getName(UUID uniqueId, boolean fromdb);

    CoinsConfig getConfig();

    AbstractConfigFile getMessages(String lang);

    String getString(String path, String locale);

    List<String> getStringList(String path, String locale);

    void reloadMessages();

    String translateColor(String string);

    String removeColor(String string);
}
