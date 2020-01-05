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

import com.github.beelzebu.coins.api.plugin.CoinsBootstrap;
import com.github.beelzebu.coins.api.plugin.CoinsPlugin;
import java.util.Collections;
import java.util.List;

public abstract class MultipliersConfig extends AbstractConfigFile {

    protected CoinsPlugin<? extends CoinsBootstrap> coinsPlugin;

    public MultipliersConfig(CoinsPlugin<? extends CoinsBootstrap> coinsPlugin) {
        this.coinsPlugin = coinsPlugin;
    }

    public String getServerName() {
        return getString("Server name", "default").toLowerCase();
    }

    public String getCommand() {
        return coinsPlugin.getConfig().getString("General.Command.Multiplier.Name", "multiplier");
    }

    public String getCommandDescription() {
        return coinsPlugin.getConfig().getString("General.Command.Multiplier.Description", "Command to see and edit multipliers");
    }

    public String getCommandUsage() {
        return coinsPlugin.getConfig().getString("General.Command.Multiplier.Usage", "/multiplier");
    }

    public String getCommandPermission() {
        return coinsPlugin.getConfig().getString("General.Command.Multiplier.Permission", "coins.multiplier");
    }

    public List<String> getCommandAliases() {
        return coinsPlugin.getConfig().getStringList("General.Command.Multiplier.Aliases", Collections.emptyList());
    }

}
