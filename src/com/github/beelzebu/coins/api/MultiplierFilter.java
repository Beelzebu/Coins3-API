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
package com.github.beelzebu.coins.api;

import java.util.function.Predicate;

/**
 * @author Beelzebu
 */
public enum MultiplierFilter {

    SERVER(multiplier -> multiplier.getData().getType().equals(MultiplierType.SERVER)),
    SERVER_AND_ENABLER_PLAYER(multiplier -> multiplier.getData().getType().equals(MultiplierType.SERVER) && multiplier.getData().getEnablerUUID() != null && CoinsAPI.isindb(multiplier.getData().getEnablerUUID())),
    GLOBAL(multiplier -> multiplier.getData().getType().equals(MultiplierType.GLOBAL)),
    GLOBAL_AND_ENABLER_PLAYER(multiplier -> multiplier.getData().getType().equals(MultiplierType.GLOBAL) && multiplier.getData().getEnablerUUID() != null && CoinsAPI.isindb(multiplier.getData().getEnablerUUID()));

    private final Predicate<Multiplier> predicate;

    MultiplierFilter(Predicate<Multiplier> predicate) {
        this.predicate = predicate;
    }

    public Predicate<Multiplier> getPredicate() {
        return predicate;
    }
}
