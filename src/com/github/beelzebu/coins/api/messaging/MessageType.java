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
package com.github.beelzebu.coins.api.messaging;

/**
 * All message types that we send, used to identify messages when we need to
 * handle them.
 *
 * @author Beelzebu
 */
enum MessageType {
    /**
     * Send user coins update
     */
    USER_UPDATE,
    /**
     * Request other servers to send executors
     */
    EXECUTOR_REQUEST,
    /**
     * Send executors to other servers
     */
    EXECUTOR_SEND,
    /**
     * Request multipliers from other servers
     */
    MULTIPLIER_REQUEST,
    /**
     * Send an updated multiplier to other servers
     */
    MULTIPLIER_UPDATE,
    /**
     * Send a multiplier enable notification to other servers
     */
    MULTIPLIER_ENABLE,
    /**
     * Disable a multiplier and
     */
    MULTIPLIER_DISABLE
}
