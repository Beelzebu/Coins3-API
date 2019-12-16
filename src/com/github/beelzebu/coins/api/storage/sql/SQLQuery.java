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

import com.github.beelzebu.coins.api.MultiplierType;

/**
 * @author Beelzebu
 */
public enum SQLQuery {

    /**
     * Select all players from the database.
     */
    SELECT_ALL_PLAYERS("SELECT name,uuid,balance FROM " + SQLDatabase.DATA_TABLE + ";"),
    /**
     * Select an user by his uuid.
     * </br>
     * <strong>Params:</strong>
     * <ul>
     * <li> UUID for the query. </li>
     * </ul>
     */
    SELECT_NAME("SELECT name FROM `" + SQLDatabase.DATA_TABLE + "` WHERE uuid = ?;"),
    SELECT_UUID("SELECT uuid FROM `" + SQLDatabase.DATA_TABLE + "` WHERE name = ?;"),
    SELECT_BALANCE("SELECT balance FROM `" + SQLDatabase.DATA_TABLE + "` WHERE uuid = ?;"),
    SELECT_USER_NAME("SELECT name,uuid,balance FROM `" + SQLDatabase.DATA_TABLE + "` WHERE name = ?;"),
    SELECT_USER_UUID("SELECT name,uuid,balance FROM `" + SQLDatabase.DATA_TABLE + "` WHERE uuid = ?;"),
    /**
     * Update coins for a user by his uuid:
     * </br>
     * <strong>Params:</strong>
     * <ul>
     * <li> New balance to set.</li>
     * <li> UUID for the query</li>
     * </ul>
     */
    UPDATE_COINS("UPDATE `" + SQLDatabase.DATA_TABLE + "` SET balance = ? WHERE uuid = ?;"),
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
    UPDATE_USER_NAME_LOGIN("UPDATE `" + SQLDatabase.DATA_TABLE + "` SET name = ?, lastlogin = ? WHERE uuid = ?;"),
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
    UPDATE_USER_UUID_LOGIN("UPDATE `" + SQLDatabase.DATA_TABLE + "` SET uuid = ?, lastlogin = ? WHERE name = ?;"),
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
    CREATE_USER("INSERT INTO `" + SQLDatabase.DATA_TABLE + "` (`id`, `uuid`, `name`, `balance`, `lastlogin`) VALUES (null, ?, ?, ?, ?);"),
    /**
     * Create a multiplier in the database.
     * </br>
     * <strong>Params:</strong>
     * <ul>
     * <li> server - server name</li>
     * <li> type - multiplier type</li>
     * <li> amount - integer representing the amount</li>
     * <li> minutes - integer representing the minutes</li>
     * <li> start - long representing system millis when this multiplier was enabled</li>
     * <li> queue - long representing system millis when this multiplier was queued</li>
     * <li> data_id - id of player in data table</li>
     * </ul>
     *
     * @see MultiplierType
     */
    CREATE_MULTIPLIER("INSERT INTO `" + SQLDatabase.MULTIPLIERS_TABLE + "`" +
            "(`id`, `server`, `type`, `amount`, `minutes`, `start`, `queue`, `data_id`) VALUES " +
            "(null, ?, ?, ?, ?, ?, ?, (SELECT id FROM `" + SQLDatabase.DATA_TABLE + "` WHERE uuid = ?));"),
    /**
     * Select top users from the database.
     * </br>
     * <strong>Params:</strong>
     * <ul>
     * <li> Limit of users to select</li>
     * </ul>
     */
    SELECT_TOP("SELECT uuid,name,balance FROM `" + SQLDatabase.DATA_TABLE + "` ORDER BY balance DESC LIMIT ?;"), // TODO: add regexp filter: WHERE name NOT REGEXP '(?)-.+'
    /**
     * Select a multiplier from the database by his id.
     * </br>
     * <strong>Params:</strong>
     * <ul>
     * <li> Multiplier ID</li>
     * </ul>
     */
    SELECT_MULTIPLIER_ID("SELECT * FROM " + SQLDatabase.MULTIPLIERS_TABLE + " WHERE id = ?;"),
    /**
     * Deletes a multiplier by his ID.
     * </br>
     * <strong>Params:</strong>
     * <ul>
     * <li> Multiplier ID</li>
     * </ul>
     */
    DELETE_MULTIPLIER("DELETE FROM " + SQLDatabase.MULTIPLIERS_TABLE + " WHERE id = ?;"),
    /**
     * Enables a multiplier by his ID.
     * </br>
     * <strong>Params:</strong>
     * <ul>
     * <li> Multiplier ID</li>
     * </ul>
     */
    ENABLE_MULTIPLIER("UPDATE " + SQLDatabase.MULTIPLIERS_TABLE + " SET start = ?, queue = ? WHERE id = ?;"),
    UPDATE_MULTIPLIER("UPDATE " + SQLDatabase.MULTIPLIERS_TABLE + " SET server = ?, type = ?, amount = ?, minutes = ?, start = ?, queue = ?, data_id = (SELECT id FROM " + SQLDatabase.DATA_TABLE + " WHERE uuid = ?) WHERE id = ?"),
    /**
     * Select all multipliers from the database.
     */
    SELECT_MULTIPLIERS_IDS("SELECT id FROM " + SQLDatabase.MULTIPLIERS_TABLE + ";"),
    SELECT_MULTIPLIERS_IDS_SERVER("SELECT id FROM " + SQLDatabase.MULTIPLIERS_TABLE + " WHERE server = ?;"),
    SELECT_MULTIPLIERS_IDS_SERVER_ENABLED("SELECT id FROM " + SQLDatabase.MULTIPLIERS_TABLE + " WHERE server = ? AND enabled = ?;"),
    SELECT_MULTIPLIERS_IDS_PLAYER("SELECT id FROM " + SQLDatabase.MULTIPLIERS_TABLE + " WHERE uuid = ?;"),
    SELECT_MULTIPLIERS_IDS_PLAYER_ENABLED("SELECT id FROM " + SQLDatabase.MULTIPLIERS_TABLE + " WHERE uuid = ?;"),
    SELECT_MULTIPLIERS_IDS_PLAYER_SERVER("SELECT id FROM " + SQLDatabase.MULTIPLIERS_TABLE + " WHERE uuid = ? AND server = ?;"),
    SELECT_MULTIPLIERS_IDS_PLAYER_SERVER_ENABLED("SELECT id FROM " + SQLDatabase.MULTIPLIERS_TABLE + " WHERE uuid = ? AND server = ?;");

    private final String query;

    SQLQuery(String query) {
        this.query = query;
    }

    public String getQuery() {
        return query;
    }
}
