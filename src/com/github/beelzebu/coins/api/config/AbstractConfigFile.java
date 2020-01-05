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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * @author Beelzebu
 */
public abstract class AbstractConfigFile {

    protected AbstractConfigFile() {
    }

    public String getString(String path) {
        return (String) get(path);
    }

    public List<String> getStringList(String path) {
        return get(path, List.class) != null ? get(path, List.class) : new ArrayList<>();
    }

    public boolean getBoolean(String path) {
        return (boolean) get(path, false);
    }

    public int getInt(String path) {
        return get(path, int.class) instanceof Number ? ((Number) get(path, int.class)).intValue() : -1;
    }

    public double getDouble(String path) {
        return get(path, double.class) instanceof Number ? ((Number) get(path, double.class)).doubleValue() : -1;
    }

    public Object get(String path, Object def) {
        return get(path, def.getClass()) != null ? get(path, def.getClass()) : def;
    }

    public String getString(String path, String def) {
        return get(path, String.class) != null ? get(path, String.class) : def;
    }

    public List<String> getStringList(String path, List<String> def) {
        return get(path, List.class) != null ? getStringList(path) : def;
    }

    public boolean getBoolean(String path, boolean def) {
        return get(path, boolean.class) != null ? get(path, boolean.class) : def;
    }

    public int getInt(String path, int def) {
        return get(path, int.class) != null ? getInt(path) : def;
    }

    public double getDouble(String path, double def) {
        return get(path, double.class) != null ? getDouble(path) : def;
    }

    public abstract Object get(String path);

    public <T> T get(String path, Class<? extends T> type) {
        Object value = get(path);
        try {
            return (T) value;
        } catch (ClassCastException e) {
            Logger.getLogger(AbstractConfigFile.class.getName()).info("There is an error reading value for " + path + " expected: " + type + " found: " + value.getClass());
        }
        return null;
    }

    public abstract Set<String> getConfigurationSection(String path);

    public abstract void reload();
}
