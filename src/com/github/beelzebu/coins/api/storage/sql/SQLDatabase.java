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
import com.github.beelzebu.coins.api.CoinsResponse;
import com.github.beelzebu.coins.api.CoinsUser;
import com.github.beelzebu.coins.api.Multiplier;
import com.github.beelzebu.coins.api.MultiplierBuilder;
import com.github.beelzebu.coins.api.MultiplierData;
import com.github.beelzebu.coins.api.MultiplierType;
import com.github.beelzebu.coins.api.plugin.CoinsPlugin;
import com.github.beelzebu.coins.api.storage.StorageProvider;
import com.github.beelzebu.coins.api.storage.StorageType;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;

/**
 * @author Beelzebu
 */
public abstract class SQLDatabase implements StorageProvider {

    protected final CoinsPlugin plugin;
    protected final String prefix;
    protected final String dataTable;
    protected final String multipliersTable;
    protected HikariDataSource ds;

    public SQLDatabase(CoinsPlugin plugin) {
        this.plugin = plugin;
        prefix = plugin.getStorageProvider().getStorageType().equals(StorageType.SQLITE) ? "" : plugin.getConfig().getString("MySQL.Prefix");
        dataTable = prefix + plugin.getConfig().getString("MySQL.Data Table", "data");
        multipliersTable = prefix + plugin.getConfig().getString("MySQL.Multipliers Table", "multipliers");
    }

    @Override
    public void shutdown() {
        if (ds != null && ds.isRunning()) {
            ds.close();
        }
    }

    @Override
    public final CoinsResponse createPlayer(@Nonnull UUID uuid, @Nonnull String name, double balance) {
        try (Connection c = getConnection()) {
            if (_isindb(c, uuid) || _isindb(c, name)) {
                updatePlayer(uuid, name);
                return CoinsResponse.SUCCESS;
            }
            try (PreparedStatement ps = DatabaseUtils.prepareStatement(c, SQLQuery.CREATE_USER, uuid, name, balance, System.currentTimeMillis())) {
                plugin.debug("Creating data for player: " + name + " in the database.");
                ps.executeUpdate();
                plugin.debug("An entry in the database was created for: " + name);
            }
        } catch (SQLException ex) {
            plugin.log("An internal error has occurred while creating the player " + name + " in the database, check the logs for more info.");
            plugin.debug(ex);
            return new CoinsResponse(CoinsResponse.CoinsResponseType.FAILED, "Errors.Database");
        }
        return CoinsResponse.SUCCESS;
    }

    @Override
    public CoinsResponse updatePlayer(@Nonnull UUID uuid, @Nonnull String name) {
        name = name.toLowerCase();
        try (Connection c = getConnection()) {
            if (CoinsAPI.getPlugin().getCache().getCoins(uuid).isPresent() || _isindb(c, uuid) || _isindb(c, name)) {
                String oldName = getName(c, uuid);
                if (!Objects.equals(oldName, name)) {
                    try (PreparedStatement ps = DatabaseUtils.prepareStatement(c, SQLQuery.UPDATE_USER_NAME_LOGIN, name, System.currentTimeMillis(), uuid)) {
                        ps.executeUpdate();
                        plugin.debug("Updated the name for '" + uuid + "' (" + name + ")");
                        plugin.debug("Old name: %s, new name %s", oldName, name);
                    }
                }
                UUID oldUUID = getUUID(c, name);
                if (oldUUID != uuid) {
                    try (PreparedStatement ps = DatabaseUtils.prepareStatement(c, SQLQuery.UPDATE_USER_UUID_LOGIN, uuid, System.currentTimeMillis(), name)) {
                        ps.executeUpdate();
                        plugin.debug("Updated the UUID for '" + name + "' (" + uuid + ")");
                        plugin.debug("Old UUID: %s, new UUID %s", oldUUID, uuid);
                    }
                }
            } else if (plugin.getBootstrap().isOnline(name) && !CoinsAPI.isindb(name)) {
                plugin.debug(name + " isn't in the database, but is online and a plugin is requesting his balance.");
                CoinsAPI.createPlayer(name, uuid);
            } else {
                plugin.debug("Tried to update a player that isn't in the database and is offline. UUID: %s, Name: %s", uuid, name);
            }
        } catch (SQLException ex) {
            plugin.log("An internal error has occurred updating the data for player '" + name + "', check the logs for more info.");
            plugin.debug(ex);
        }
        return CoinsResponse.SUCCESS;
    }

    @Override
    public UUID getUUID(String name) {
        try (Connection c = getConnection()) {
            return getUUID(c, name);
        } catch (SQLException ex) {
            plugin.log("Something was wrong getting the uuid for the name '" + name + "'");
            plugin.debug(ex);
        }
        return null;
    }

    @Override
    public String getName(UUID uuid) {
        try (Connection c = getConnection()) {
            return getName(c, uuid);
        } catch (SQLException ex) {
            plugin.log("Something was wrong getting the name for the uuid '" + uuid + "'");
            plugin.debug(ex);
        }
        return null;
    }

    @Override
    public final double getCoins(UUID uuid) {
        double coins = -1;
        try (Connection c = getConnection(); PreparedStatement ps = DatabaseUtils.prepareStatement(c, SQLQuery.SELECT_BALANCE, uuid); ResultSet res = ps.executeQuery()) {
            if (res.next()) {
                coins = res.getDouble("balance");
            } else if (plugin.getBootstrap().isOnline(uuid)) {
                coins = plugin.getConfig().getDouble("General.Starting Coins", 0);
                createPlayer(uuid, plugin.getName(uuid, false).toLowerCase(), coins);
            }
        } catch (SQLException ex) {
            plugin.log("An internal error has occurred creating the data for player: " + uuid);
            plugin.debug(ex);
        }
        return coins;
    }

    @Override
    public final CoinsResponse setCoins(UUID uuid, double amount) {
        CoinsResponse response;
        try (Connection c = getConnection()) {
            if (CoinsAPI.getCoins(uuid) > -1 || CoinsAPI.isindb(uuid)) {
                DatabaseUtils.prepareStatement(c, SQLQuery.UPDATE_COINS, amount, uuid).executeUpdate();
                response = CoinsResponse.SUCCESS;
            } else {
                response = new CoinsResponse(CoinsResponse.CoinsResponseType.FAILED, "Errors.Unknown player", "%target%", uuid.toString());
            }
        } catch (SQLException ex) {
            response = new CoinsResponse(CoinsResponse.CoinsResponseType.FAILED, "Errors.Database");
            plugin.log("An internal error has occurred setting coins to the player: " + uuid);
            plugin.debug(ex);
        }
        return response;
    }

    @Override
    public final boolean isindb(UUID uuid) {
        try (Connection c = getConnection()) {
            return _isindb(c, uuid);
        } catch (SQLException ex) {
            plugin.log("An internal error has occurred cheking if the player: " + uuid + " exists in the database.");
            plugin.debug(ex);
        }
        return false;
    }

    @Override
    public final boolean isindb(String name) {
        try (Connection c = getConnection()) {
            return _isindb(c, name);
        } catch (SQLException ex) {
            plugin.log("An internal error has occurred cheking if the player: " + name + " exists in the database.");
            plugin.debug(ex);
        }
        return false;
    }

    @Override
    public LinkedHashSet<CoinsUser> getTopPlayers(int top) {
        LinkedHashSet<CoinsUser> topplayers = new LinkedHashSet<>();
        try (Connection c = getConnection(); PreparedStatement ps = DatabaseUtils.prepareStatement(c, SQLQuery.SELECT_TOP, top); ResultSet res = ps.executeQuery()) {
            while (res.next()) {
                UUID uuid = UUID.fromString(res.getString("uuid"));
                String name = res.getString("name");
                double coins = res.getDouble("balance");
                topplayers.add(new CoinsUser(uuid, name, coins));
            }
        } catch (SQLException ex) {
            plugin.log("An internal error has occurred generating the toplist");
            plugin.debug(ex);
        }
        return topplayers;
    }

    @Override
    public void saveMultiplier(Multiplier multiplier) {
        // UUID uuid, int amount, int minutes, String server, MultiplierType type
        try (Connection c = getConnection()) {
            DatabaseUtils.prepareStatement(Statement.RETURN_GENERATED_KEYS, c, SQLQuery.CREATE_MULTIPLIER, multiplier.getServer(), multiplier.getData().getEnablerUUID(), multiplier.getData().getType(), multiplier.getData().getAmount(), multiplier.getData().getMinutes(), multiplier.isEnabled(), multiplier.isQueue());
            MultiplierBuilder.setId(multiplier, c.createStatement().executeQuery("LAST_INSERT_ID()").getInt(1));
        } catch (SQLException ex) {
            plugin.log("Something was wrong when creating a multiplier for " + plugin.getName(multiplier.getData().getEnablerUUID(), false));
            plugin.debug(multiplier.toJson().toString());
            plugin.debug(ex);
        }
    }

    @Override
    public Multiplier getMultiplier(int id) {
        try (Connection c = getConnection(); PreparedStatement ps = DatabaseUtils.prepareStatement(c, SQLQuery.SELECT_MULTIPLIER, id); ResultSet res = ps.executeQuery()) {
            if (res.next()) {
                return MultiplierBuilder.newBuilder(res.getString("server"), getDataFromResultSet(res))
                        .setID(res.getInt("id"))
                        .setEnabled(res.getBoolean("enabled"))
                        .setQueue(res.getBoolean("queue"))
                        .build(false);
            }
        } catch (SQLException ex) {
            plugin.log("An error has occurred getting the multiplier with the id #" + id + " from the database.");
            plugin.debug(ex);
        }
        return null;
    }

    @Override
    public Set<Multiplier> getMultipliers(UUID uuid) {
        Set<Multiplier> multipliers = new LinkedHashSet<>();
        try (Connection c = getConnection(); PreparedStatement ps = DatabaseUtils.prepareStatement(c, SQLQuery.SELECT_ALL_MULTIPLIERS_PLAYER, uuid); ResultSet res = ps.executeQuery()) {
            while (res.next()) {
                multipliers.add(getMultiplier(res.getInt("id")));
            }
        } catch (SQLException ex) {
            plugin.log("An error has occurred getting all the multipliers for " + uuid);
            plugin.debug(ex);
        }
        return multipliers;
    }

    @Override
    public Set<Multiplier> getMultipliers(UUID uuid, String server) {
        Set<Multiplier> multipliers = new LinkedHashSet<>();
        try (Connection c = getConnection(); PreparedStatement ps = DatabaseUtils.prepareStatement(c, SQLQuery.SELECT_ALL_MULTIPLIERS_SERVER, uuid, server); ResultSet res = ps.executeQuery()) {
            while (res.next()) {
                multipliers.add(getMultiplier(res.getInt("id")));
            }
        } catch (SQLException ex) {
            plugin.log("An error has occurred getting all the multipliers for " + uuid + " in server " + server);
            plugin.debug(ex);
        }
        return multipliers;
    }

    @Override
    public Set<Multiplier> getMultipliers() {
        Set<Multiplier> multipliers = new LinkedHashSet<>();
        try (Connection c = getConnection(); PreparedStatement ps = DatabaseUtils.prepareStatement(c, SQLQuery.SELECT_ALL_MULTIPLIERS); ResultSet res = ps.executeQuery()) {
            while (res.next()) {
                multipliers.add(getMultiplier(res.getInt("id")));
            }
        } catch (SQLException ex) {
            plugin.log("An error has occurred getting all the multipliers");
            plugin.debug(ex);
        }
        return multipliers;
    }

    @Override
    public void enableMultiplier(Multiplier multiplier) {
        try (Connection c = getConnection()) {
            DatabaseUtils.prepareStatement(c, SQLQuery.ENABLE_MULTIPLIER, multiplier.getId()).executeUpdate();
        } catch (SQLException ex) {
            plugin.log("An error has occurred enabling the multiplier #" + multiplier.getId());
            plugin.debug(ex);
        }
    }

    @Override
    public void deleteMultiplier(Multiplier multiplier) {
        try (Connection c = getConnection()) {
            DatabaseUtils.prepareStatement(c, SQLQuery.DELETE_MULTIPLIER, multiplier.getId()).executeUpdate();
        } catch (SQLException ex) {
            plugin.log("An error has occurred while deleting the multiplier #" + multiplier.getId());
            plugin.debug(ex);
        }
    }

    @Override
    public LinkedHashMap<String, Double> getAllPlayers() {
        LinkedHashMap<String, Double> data = new LinkedHashMap<>();
        try (Connection c = getConnection(); PreparedStatement ps = DatabaseUtils.prepareStatement(c, SQLQuery.SELECT_ALL_PLAYERS); ResultSet res = ps.executeQuery()) {
            while (res.next()) {
                data.put(res.getString("name") + "," + res.getString("uuid"), res.getDouble("balance"));
            }
        } catch (SQLException ex) {
            plugin.log("An error has occurred getting all the players from the database, check the logs for more info.");
            plugin.debug(ex);
        }
        return data;
    }

    protected abstract void updateDatabase();

    private Connection getConnection() throws SQLException {
        if (ds != null && !ds.isClosed() && ds.isRunning()) {
            return ds.getConnection();
        } else {
            plugin.debug("Connection is invalid, trying to reconnect.");
            shutdown();
            setup();
        }
        return ds.getConnection();
    }

    private boolean _isindb(Connection c, UUID uuid) throws SQLException {
        try (PreparedStatement ps = DatabaseUtils.prepareStatement(c, SQLQuery.SELECT_NAME, uuid); ResultSet res = ps.executeQuery()) {
            return res.next();
        }
    }

    private boolean _isindb(Connection c, String name) throws SQLException {
        try (PreparedStatement ps = DatabaseUtils.prepareStatement(c, SQLQuery.SELECT_UUID, name); ResultSet res = ps.executeQuery()) {
            return res.next();
        }
    }

    String getDataTable() {
        return dataTable;
    }

    String getMultipliersTable() {
        return multipliersTable;
    }

    private UUID getUUID(Connection c, String name) throws SQLException {
        try (PreparedStatement ps = DatabaseUtils.prepareStatement(c, SQLQuery.SELECT_UUID, name); ResultSet res = ps.executeQuery()) {
            if (res.next()) {
                return UUID.fromString(res.getString("uuid"));
            }
        }
        return null;
    }

    private String getName(Connection c, UUID uuid) throws SQLException {
        try (PreparedStatement ps = DatabaseUtils.prepareStatement(c, SQLQuery.SELECT_NAME, uuid); ResultSet res = ps.executeQuery()) {
            if (res.next()) {
                return res.getString("name");
            }
        }
        return null;
    }

    private MultiplierData getDataFromResultSet(ResultSet res) throws SQLException {
        return new MultiplierData(UUID.fromString(res.getString("uuid")),
                plugin.getName(UUID.fromString(res.getString("uuid")), false),
                res.getInt("amount"), res.getInt("minutes"),
                MultiplierType.valueOf(res.getString("type")));
    }

    private CoinsUser getUser(Connection c, UUID uuid) throws SQLException {
        try (PreparedStatement ps = DatabaseUtils.prepareStatement(c, SQLQuery.SELECT_USER_UUID, uuid); ResultSet res = ps.executeQuery()) {
            if (res.next()) {
                return getUserFromResultSet(res);
            }
        }
        return null;
    }

    private CoinsUser getUser(Connection c, String name) throws SQLException {
        try (PreparedStatement ps = DatabaseUtils.prepareStatement(c, SQLQuery.SELECT_USER_NAME, name); ResultSet res = ps.executeQuery()) {
            if (res.next()) {
                return getUserFromResultSet(res);
            }
        }
        return null;
    }

    private CoinsUser getUserFromResultSet(ResultSet res) throws SQLException {
        return new CoinsUser(UUID.fromString(res.getString("uuid")), res.getString("name"), res.getDouble("balance"));
    }
}
