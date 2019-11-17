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
package com.github.beelzebu.coins.api.storage.sql;

import com.github.beelzebu.coins.api.CoinsAPI;
import com.github.beelzebu.coins.api.MultiplierType;

/**
 * @author Beelzebu
 */
public enum SQLQuery {
    /**
     * Select an user by his uuid.
     * </br>
     * <strong>Params:</strong>
     * <ul>
     * <li> UUID for the query. </li>
     * </ul>
     */
    SELECT_NAME("SELECT name FROM `" + ((SQLDatabase) CoinsAPI.getPlugin().getStorageProvider()).getDataTable() + "` WHERE uuid = ?;"),
    SELECT_UUID("SELECT uuid FROM `" + ((SQLDatabase) CoinsAPI.getPlugin().getStorageProvider()).getDataTable() + "` WHERE name = ?;"),
    SELECT_BALANCE("SELECT balance FROM `" + ((SQLDatabase) CoinsAPI.getPlugin().getStorageProvider()).getDataTable() + "` WHERE uuid = ?;"),
    SELECT_USER_NAME("SELECT name,uuid,balance FROM `" + ((SQLDatabase) CoinsAPI.getPlugin().getStorageProvider()).getDataTable() + "` WHERE name = ?;"),
    SELECT_USER_UUID("SELECT name,uuid,balance FROM `" + ((SQLDatabase) CoinsAPI.getPlugin().getStorageProvider()).getDataTable() + "` WHERE uuid = ?;"),
    /**
     * Update coins for a user by his uuid:
     * </br>
     * <strong>Params:</strong>
     * <ul>
     * <li> New balance to set.</li>
     * <li> UUID for the query</li>
     * </ul>
     */
    UPDATE_COINS("UPDATE `" + ((SQLDatabase) CoinsAPI.getPlugin().getStorageProvider()).getDataTable() + "` SET balance = ? WHERE uuid = ?;"),
    /**
     * Update name and last login for user, based on his UUID.
     * </br>
     * <strong>Params:</strong>
     * <ul>
     * <li> Username to update.</li>
     * <li> Lastlogin in millis.</li>
     * <li> UUID for the query</li>
     * </ul>
     */
    UPDATE_USER_NAME_LOGIN("UPDATE `" + ((SQLDatabase) CoinsAPI.getPlugin().getStorageProvider()).getDataTable() + "` SET name = ?, lastlogin = ? WHERE uuid = ?;"),
    /**
     * Update data for a user when the server is in online mode.
     * </br>
     * <strong>Params:</strong>
     * <ul>
     * <li> UUID to update.</li>
     * <li> Lastlogin in millis.</li>
     * <li> Username for the query</li>
     * </ul>
     */
    UPDATE_USER_UUID_LOGIN("UPDATE `" + ((SQLDatabase) CoinsAPI.getPlugin().getStorageProvider()).getDataTable() + "` SET uuid = ?, lastlogin = ? WHERE name = ?;"),
    /**
     * Create a user in the database.
     * </br>
     * <strong>Params:</strong>
     * <ul>
     * <li> UUID of the user.</li>
     * <li> Username of the user.</li>
     * <li> Starting coins.</li>
     * <li> Current time in millis</li>
     * </ul>
     */
    CREATE_USER("INSERT INTO `" + ((SQLDatabase) CoinsAPI.getPlugin().getStorageProvider()).getDataTable() + "` (`id`, `uuid`, `name`, `balance`, `lastlogin`) VALUES (null, ?, ?, ?, ?);"),
    /**
     * Create a multiplier in the database.
     * </br>
     * <strong>Params:</strong>
     * <ul>
     * <li> Servername for the multiplier.</li>
     * <li> UUID of the owner of the multiplier.</li>
     * <li> Type of this multiplier.</li>
     * <li> Amount of this multiplier.</li>
     * <li> Minutes that the multiplier must be enabled.</li>
     * <li> If the multiplier must be added to the queue.</li>
     * <li> If the multiplier is enabled</li>
     * </ul>
     *
     * @see MultiplierType
     */
    CREATE_MULTIPLIER("INSERT INTO `" + ((SQLDatabase) CoinsAPI.getPlugin().getStorageProvider()).getMultipliersTable() + "` (`id`, `server`, `uuid`, `type`, `amount`, `minutes`, `endtime`, `queue`, `enabled`) VALUES (null, ?, ?, ?, ?, ?, ?, ?, ?);"),
    /**
     * Select top users from the database.
     * </br>
     * <strong>Params:</strong>
     * <ul>
     * <li> Limit of users to select</li>
     * </ul>
     */
    SELECT_TOP("SELECT uuid,name,balance FROM `" + ((SQLDatabase) CoinsAPI.getPlugin().getStorageProvider()).getDataTable() + "` WHERE name NOT REGEXP '(?)-.+' ORDER BY balance DESC LIMIT ?;"),
    /**
     * Deletes a multiplier by his ID.
     * </br>
     * <strong>Params:</strong>
     * <ul>
     * <li> Multiplier ID</li>
     * </ul>
     */
    DELETE_MULTIPLIER("DELETE FROM " + ((SQLDatabase) CoinsAPI.getPlugin().getStorageProvider()).getMultipliersTable() + " WHERE id = ?;"),
    /**
     * Enables a multiplier by his ID.
     * </br>
     * <strong>Params:</strong>
     * <ul>
     * <li> Multiplier ID</li>
     * </ul>
     */
    ENABLE_MULTIPLIER("UPDATE " + ((SQLDatabase) CoinsAPI.getPlugin().getStorageProvider()).getMultipliersTable() + " SET enabled = true WHERE id = ?;"),
    /**
     * Select all multipliers from the database.
     */
    SELECT_ALL_MULTIPLIERS("SELECT * FROM " + ((SQLDatabase) CoinsAPI.getPlugin().getStorageProvider()).getMultipliersTable()),
    /**
     * Select all multipliers from a player that can be enabled.
     * </br>
     * <strong>Params:</strong>
     * <ul>
     * <li> UUID of the player</li>
     * </ul>
     */
    SELECT_ALL_MULTIPLIERS_PLAYER("SELECT * FROM " + ((SQLDatabase) CoinsAPI.getPlugin().getStorageProvider()).getMultipliersTable() + " WHERE uuid = ? AND enabled = false AND queue = false;"),
    /**
     * Select all multipliers from a player that can be enabled for a specific server.
     * </br>
     * <strong>Params:</strong>
     * <ul>
     * <li> UUID of the player</li>
     * <li> Server name</li>
     * </ul>
     */
    SELECT_ALL_MULTIPLIERS_SERVER("SELECT * FROM " + ((SQLDatabase) CoinsAPI.getPlugin().getStorageProvider()).getMultipliersTable() + " WHERE uuid = ? AND enabled = false AND queue = false AND server = ?;"),
    /**
     * Select a multiplier from the database by his id.
     * </br>
     * <strong>Params:</strong>
     * <ul>
     * <li> Multiplier ID</li>
     * </ul>
     */
    SELECT_MULTIPLIER("SELECT * FROM " + ((SQLDatabase) CoinsAPI.getPlugin().getStorageProvider()).getMultipliersTable() + " WHERE id = ?;"),
    /**
     * Select all players from the database.
     */
    SELECT_ALL_PLAYERS("SELECT * FROM " + ((SQLDatabase) CoinsAPI.getPlugin().getStorageProvider()).getDataTable() + ";");

    private final String query;

    SQLQuery(String query) {
        this.query = query;
    }

    public String getQuery() {
        return query;
    }
}
