/**
 *  Copyright (C) 2002-2011  The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.List;



public class BuildQueue<T extends BuildableType> implements Consumer {

    /** A list of Buildable items. */
    private List<T> buildQueue = new ArrayList<T>();

    private int priority = COLONY_PRIORITY;

    private Colony colony;

    public BuildQueue(Colony colony, int priority, T... items) {
        this.colony = colony;
        this.priority = priority;
        for (T type : items) {
            buildQueue.add(type);
        }
    }

    /**
     * Returns the type of building currently being built.
     *
     * @return The type of building currently being built.
     */
    public T getCurrentlyBuilding() {
        return (buildQueue.isEmpty()) ? null : buildQueue.get(0);
    }

    /**
     * Sets the current type of buildable to be built and if it is a building
     * insist that there is only one in the queue.
     *
     * @param buildable The <code>T</code> to build.
     */
    public void setCurrentlyBuilding(T buildable) {
        if (buildable instanceof BuildingType && buildQueue.contains(buildable)) {
            buildQueue.remove(buildable);
        }
        buildQueue.add(0, buildable);
    }


    public void clear() {
        buildQueue.clear();
    }

    public void add(T buildable) {
        buildQueue.add(buildable);
    }

    public List<T> getValues() {
        return buildQueue;
    }

    public void setValues(List<T> values) {
        buildQueue = values;
    }

    public void remove(int index) {
        buildQueue.remove(index);
    }

    public int size() {
        return buildQueue.size();
    }

    public boolean isEmpty() {
        return buildQueue.isEmpty();
    }


    // Interface Consumer

    /**
     * Returns true if this Consumer consumes the given GoodsType.
     *
     * @param goodsType a <code>GoodsType</code> value
     * @return a <code>boolean</code> value
     */
    public boolean consumes(GoodsType goodsType) {
        return getCurrentlyBuilding() != null
            && getCurrentlyBuilding().getAmountRequiredOf(goodsType) > 0;
    }

    /**
     * Returns a list of GoodsTypes this Consumer consumes.
     *
     * @return a <code>List</code> value
     */
    public List<AbstractGoods> getConsumedGoods() {
        T current = getCurrentlyBuilding();
        if (current == null) {
            return new ArrayList<AbstractGoods>();
        } else {
            return current.getGoodsRequired();
        }
    }

    /**
     * Return the <code>ProductionInfo</code> for this BuildQueue.
     *
     * @param input the goods available
     * @return the <code>ProductionInfo</code> for this BuildQueue
     */
    public ProductionInfo getProductionInfo(List<AbstractGoods> input) {
        ProductionInfo result = new ProductionInfo();
        T current = getCurrentlyBuilding();
        if (current != null) {
            // ATTENTION: this code presupposes that we will consume
            // all required goods at once
            boolean overflow = colony.getSpecification()
                .getBoolean(GameOptions.SAVE_PRODUCTION_OVERFLOW);
            List<AbstractGoods> consumption = new ArrayList<AbstractGoods>();
            for (AbstractGoods required : current.getGoodsRequired()) {
                boolean satisfied = false;
                for (AbstractGoods available : input) {
                    if (required.getType() == available.getType()
                        && required.getAmount() <= available.getAmount()) {
                        int amount = (overflow || required.getType().isStorable())
                            ? required.getAmount()
                            : available.getAmount();
                        consumption.add(new AbstractGoods(required.getType(), amount));
                        satisfied = true;
                        break;
                    }
                }
                if (!satisfied) {
                    // don't build anything
                    return result;
                }
            }
            result.setConsumption(consumption);
        }
        return result;
    }


    /**
     * The priority of this Consumer. The higher the priority, the
     * earlier will the Consumer be allowed to consume the goods it
     * requires.
     *
     * @return an <code>int</code> value
     */
    public int getPriority() {
        return priority;
    }


   /**
    * Returns whether the consumer has the ability with the given
    * id. The two abilities most relevant to consumers are
    * "consumeAllOrNothing", which implies that the consumer will not
    * consume any goods if its requirements can not be met (used by
    * the Colony when building), as well as
    * "consumeOnlySurplusProduction", which implies that the consumer
    * does not consume stored goods (used by the country and stables).
    *
    * @param id a <code>String</code> value
    * @return a <code>boolean</code> value
    */
    public boolean hasAbility(String id) {
        return "model.ability.consumeAllOrNothing".equals(id);
    }

    public String toString() {
        String result = "BuildQueue:";
        for (BuildableType item : buildQueue) {
            result += " " + item.getId();
        }
        return result;

    }

}