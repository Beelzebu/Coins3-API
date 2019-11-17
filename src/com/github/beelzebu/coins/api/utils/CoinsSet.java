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
package com.github.beelzebu.coins.api.utils;

import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * @author Beelzebu
 */
public class CoinsSet <E> extends LinkedHashSet<E> {

    public CoinsSet() {
        super();
    }

    public CoinsSet(Collection<? extends E> collection) {
        super(collection);
    }

    public CoinsSet<E> getFirst(int amount) {
        CoinsSet<E> set = new CoinsSet<>();
        for (E e : this) {
            if (set.size() < amount) {
                set.add(e);
            } else {
                break;
            }
        }
        return set;
    }
}
