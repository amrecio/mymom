/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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

package net.sf.freecol.server.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.PlayerType;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.ai.mission.BuildColonyMission;
import net.sf.freecol.server.ai.mission.CashInTreasureTrainMission;
import net.sf.freecol.server.ai.mission.Mission;
import net.sf.freecol.server.ai.mission.PioneeringMission;
import net.sf.freecol.server.ai.mission.ScoutingMission;
import net.sf.freecol.server.ai.mission.TransportMission;
import net.sf.freecol.server.ai.mission.UnitWanderHostileMission;
import net.sf.freecol.server.ai.mission.WishRealizationMission;

import org.w3c.dom.Element;

/**
 *
 * Objects of this class contains AI-information for a single {@link Player} and
 * is used for controlling this getPlayer().
 *
 * <br />
 * <br />
 *
 * The method {@link #startWorking} gets called by the
 * {@link AIInGameInputHandler} when it is this player's turn.
 */
public abstract class ColonialAIPlayer extends EuropeanAIPlayer {

    private static final Logger logger = Logger.getLogger(ColonialAIPlayer.class.getName());

    /**
     *
     * Tells this <code>AIPlayer</code> to make decisions. The
     * <code>AIPlayer</code> is done doing work this turn when this method
     * returns.
     */
    public void startWorking() {
        logger.fine("Entering AI code for: " + getPlayer().getNationAsString());
        sessionRegister.clear();
        clearAIUnits();
        cheat();
        determineStances();
        rearrangeWorkersInColonies();
        abortInvalidAndOneTimeMissions();
        ensureCorrectMissions();
        giveNavalMissions();
        secureSettlements();
        giveNormalMissions();
        createAIGoodsInColonies();
        createTransportLists();
        doMissions();
        rearrangeWorkersInColonies();
        abortInvalidMissions();
        // Some of the mission might have been invalidated by a another mission.
        giveNormalMissions();
        doMissions();
        rearrangeWorkersInColonies();
        abortInvalidMissions();
        ensureCorrectMissions();
        clearAIUnits();
    }

    /**
     * Gives a mission to non-naval units.
     */
    private void giveNormalMissions() {
        logger.finest("Entering method giveNormalMissions");

        int numberOfUnits = FreeCol.getSpecification().numberOfUnitTypes();
        // Create a datastructure for the worker wishes:
        ArrayList<ArrayList<Wish>> workerWishes = new ArrayList<ArrayList<Wish>>(numberOfUnits);
        for (int i = 0; i < numberOfUnits; i++) {
            workerWishes.add(new ArrayList<Wish>());
        }
        if (getPlayer().isEuropean()) {
            Iterator<AIColony> aIterator = getAIColonyIterator();
            while (aIterator.hasNext()) {
                Iterator<Wish> wIterator = aIterator.next().getWishIterator();
                while (wIterator.hasNext()) {
                    Wish w = wIterator.next();
                    if (w instanceof WorkerWish && w.getTransportable() == null) {
                        workerWishes.get(((WorkerWish) w).getUnitType().getIndex()).add(w);
                    }
                }
            }
        }

        final boolean fewColonies = hasFewColonies();
        Iterator<AIUnit> aiUnitsIterator = getAIUnitIterator();
        while (aiUnitsIterator.hasNext()) {
            AIUnit aiUnit = aiUnitsIterator.next();

            if (aiUnit.hasMission()) {
                continue;
            }

            Unit unit = aiUnit.getUnit();

            if (unit.isUninitialized()) {
                logger.warning("Trying to assign a mission to an uninitialized object: " + unit.getId());
                continue;
            }

            if (unit.canCarryTreasure()) {
                aiUnit.setMission(new CashInTreasureTrainMission(getAIMain(), aiUnit));
            } else if (unit.hasAbility("model.ability.scoutIndianSettlement") &&
                       ScoutingMission.isValid(aiUnit)) {
                aiUnit.setMission(new ScoutingMission(getAIMain(), aiUnit));
            } else if ((unit.isOffensiveUnit() || unit.isDefensiveUnit())
                       && (!unit.isColonist() || unit.hasAbility("model.ability.expertSoldier") ||
                           getGame().getTurn().getNumber() > 5)) {
                giveMilitaryMission(aiUnit);
            } else if (unit.getEquipment().contains(toolsType)
                       && PioneeringMission.isValid(aiUnit)) {
                aiUnit.setMission(new PioneeringMission(getAIMain(), aiUnit));
            } else if (unit.isColonist()) {
                /*
                 * Motivated by (speed) performance: This map stores the
                 * distance between the unit and the destination of a Wish:
                 */
                HashMap<Location, Integer> distances = new HashMap<Location, Integer>(121);
                for (ArrayList<Wish> al : workerWishes) {
                    for (Wish w : al) {
                        if (!distances.containsKey(w.getDestination())) {
                            distances.put(w.getDestination(), unit.getTurnsToReach(w.getDestination()));
                        }
                    }
                }

                // Check if this unit is needed as an expert (using:
                // "WorkerWish"):
                ArrayList<Wish> wishList = workerWishes.get(unit.getType().getIndex());
                WorkerWish bestWish = null;
                int bestTurns = Integer.MAX_VALUE;
                for (int i = 0; i < wishList.size(); i++) {
                    WorkerWish ww = (WorkerWish) wishList.get(i);
                    if (ww.getTransportable() != null) {
                        wishList.remove(i);
                        i--;
                        continue;
                    }
                    int turns = distances.get(ww.getDestination());
                    if (turns == Integer.MAX_VALUE) {
                        if (ww.getDestination().getTile() == null) {
                            turns = 5;
                        } else {
                            turns = 10;
                        }
                    } else if (turns > 5) {
                        turns = 5;
                    }
                    if (bestWish == null
                        || ww.getValue() - (turns * 2) > bestWish.getValue() - (bestTurns * 2)) {
                        bestWish = ww;
                        bestTurns = turns;
                    }
                }
                if (bestWish != null) {
                    bestWish.setTransportable(aiUnit);
                    aiUnit.setMission(new WishRealizationMission(getAIMain(), aiUnit, bestWish));
                    continue;
                }
                // Find a site for a new colony:
                Tile colonyTile = null;
                if (getPlayer().canBuildColonies()) {
                    colonyTile = BuildColonyMission.findColonyLocation(aiUnit.getUnit());
                }
                if (colonyTile != null) {
                    bestTurns = unit.getTurnsToReach(colonyTile);
                }

                // Check if we can find a better site to work than a new colony:
                if (!fewColonies || colonyTile == null || bestTurns > 10) {
                    for (int i = 0; i < workerWishes.size(); i++) {
                        wishList = workerWishes.get(i);
                        for (int j = 0; j < wishList.size(); j++) {
                            WorkerWish ww = (WorkerWish) wishList.get(j);
                            if (ww.getTransportable() != null) {
                                wishList.remove(j);
                                j--;
                                continue;
                            }
                            int turns = distances.get(ww.getDestination());
                            if (turns == Integer.MAX_VALUE) {
                                if (ww.getDestination().getTile() == null) {
                                    turns = 5;
                                } else {
                                    turns = 10;
                                }
                            } else if (turns > 5) {
                                turns = 5;
                            }
                            // TODO: Choose to build colony if the value of the
                            // wish is low.
                            if (bestWish == null
                                || ww.getValue() - (turns * 2) > bestWish.getValue() - (bestTurns * 2)) {
                                bestWish = ww;
                                bestTurns = turns;
                            }
                        }
                    }
                }
                if (bestWish != null) {
                    bestWish.setTransportable(aiUnit);
                    aiUnit.setMission(new WishRealizationMission(getAIMain(), aiUnit, bestWish));
                    continue;
                }
                // Choose to build a new colony:
                if (colonyTile != null) {
                    Mission mission = new BuildColonyMission(getAIMain(),
                                                             aiUnit,
                                                             colonyTile,
                                                             getPlayer().getColonyValue(colonyTile));
                    aiUnit.setMission(mission);

                    boolean isUnitOnCarrier = aiUnit.getUnit().getLocation() instanceof Unit;
                    if (isUnitOnCarrier) {
                        AIUnit carrier = (AIUnit) getAIMain().getAIObject(
                                                                          (FreeColGameObject) aiUnit.getUnit().getLocation());

                        //make verification of carrier mission
                        Mission carrierMission = carrier.getMission();

                        boolean isCarrierMissionToTransport = carrierMission instanceof TransportMission;
                        if(!isCarrierMissionToTransport){
                            throw new IllegalStateException("Carrier carrying unit not on a transport mission");
                        }
                        //transport unit to carrier destination (is this what is truly wanted?)
                        ((TransportMission) carrierMission).addToTransportList(aiUnit);
                    }
                    continue;
                }
            }
            if (!aiUnit.hasMission()) {
                aiUnit.setMission(new UnitWanderHostileMission(getAIMain(), aiUnit));
            }
        }
    }

    /**
     * Cheats for the AI :-)
     */
    private void cheat() {
        logger.finest("Entering method cheat");
        // TODO-AI-CHEATING: REMOVE WHEN THE AI IS GOOD ENOUGH:
        for (GoodsType goodsType : FreeCol.getSpecification().getGoodsTypeList()) {
            getPlayer().resetArrears(goodsType);
        }
        if (getAIMain().getFreeColServer().isSingleplayer() && getPlayer().isAI()
            && getPlayer().getPlayerType() == PlayerType.COLONIAL) {
            Europe europe = getPlayer().getEurope();
            List<UnitType> unitTypes = FreeCol.getSpecification().getUnitTypeList();

            if (getRandom().nextInt(10) == 1) {
                int price = 0;
                UnitType unitToTrain = null;
                for (UnitType unitType : unitTypes) {
                    if (unitType.hasPrice()) {
                        int unitPrice = europe.getUnitPrice(unitType);
                        if (unitToTrain == null || unitPrice < price) {
                            unitToTrain = unitType;
                            price = unitPrice;
                        }
                    }
                }
                Unit unit = null;
                if (unitToTrain != null) {
                    getPlayer().modifyGold(price);
                    unit = this.trainAIUnitInEurope(unitToTrain).getUnit();
                }
                if (unit != null && unit.isColonist()) {
                    // no need to equip artillery units with muskets or horses
                    getPlayer().modifyGold(getPlayer().getMarket().getBidPrice(Goods.MUSKETS, 50));
                    getPlayer().modifyGold(getPlayer().getMarket().getBidPrice(Goods.HORSES, 50));

                    Element clearSpecialityElement = Message.createNewRootElement("clearSpeciality");
                    clearSpecialityElement.setAttribute("unit", unit.getId());
                    sendAndWaitSafely(clearSpecialityElement);
                    Element equipMusketsElement = Message.createNewRootElement("equipUnit");
                    equipMusketsElement.setAttribute("unit", unit.getId());
                    equipMusketsElement.setAttribute("type", "model.equipment.muskets");
                    equipMusketsElement.setAttribute("amount", Integer.toString(50));
                    sendAndWaitSafely(equipMusketsElement);
                    Element equipHorsesElement = Message.createNewRootElement("equipUnit");
                    equipHorsesElement.setAttribute("unit", unit.getId());
                    equipHorsesElement.setAttribute("type", "model.equipment.horses");
                    equipHorsesElement.setAttribute("amount", Integer.toString(50));
                    sendAndWaitSafely(equipHorsesElement);
                }
            }
            if (getRandom().nextInt(40) == 21) {
                int total = 0;
                ArrayList<UnitType> navalUnits = new ArrayList<UnitType>();
                for (UnitType unitType : unitTypes) {
                    if (unitType.hasAbility("model.ability.navalUnit") && unitType.hasPrice()) {
                        navalUnits.add(unitType);
                        total += europe.getUnitPrice(unitType);
                    }
                }

                UnitType unitToPurchase = null;
                int random = getRandom().nextInt(total);
                total = 0;
                for (UnitType unitType : navalUnits) {
                    total += unitType.getPrice();
                    if (random < total) {
                        unitToPurchase = unitType;
                        break;
                    }
                }
                getPlayer().modifyGold(europe.getUnitPrice(unitToPurchase));
                this.trainAIUnitInEurope(unitToPurchase);
            }
        }
    }

    /**
     * Selects the most useful founding father offered.
     *
     * @param foundingFathers The founding fathers on offer.
     * @return The founding father selected.
     */
    public FoundingFather selectFoundingFather(List<FoundingFather> foundingFathers) {
        // TODO: improve choice
        int age = getGame().getTurn().getAge();
        FoundingFather bestFather = null;
        int bestWeight = -1;
        for (FoundingFather father : foundingFathers) {
            if (father == null) continue;
            int weight = father.getWeight(age);
            if (weight > bestWeight) {
                bestWeight = weight;
                bestFather = father;
            }
        }
        return bestFather;
    }

    /**
     * Decides whether to accept the monarch's tax raise or not.
     *
     * @param tax The new tax rate to be considered.
     * @return <code>true</code> if the tax raise should be accepted.
     */
    public boolean acceptTax(int tax) {
        Goods toBeDestroyed = getPlayer().getMostValuableGoods();
        if (toBeDestroyed == null) {
            return false;
        }

        GoodsType goodsType = toBeDestroyed.getType();
        if (goodsType.isFoodType() || goodsType.isBreedable()) {
            // we should be able to produce food and horses ourselves
            return false;
        } else if (goodsType.isMilitaryGoods() ||
                   goodsType.isTradeGoods() ||
                   goodsType.isBuildingMaterial()) {
            if (getGame().getTurn().getAge() == 3) {
                // by this time, we should be able to produce
                // enough ourselves
                return false;
            } else {
                return true;
            }
        } else {
            int averageIncome = 0;
            int numberOfGoods = 0;
            List<GoodsType> goodsTypes = FreeCol.getSpecification().getGoodsTypeList();
            for (GoodsType type : goodsTypes) {
                if (type.isStorable()) {
                    averageIncome += getPlayer().getIncomeAfterTaxes(type);
                    numberOfGoods++;
                }
            }
            averageIncome = averageIncome / numberOfGoods;
            if (getPlayer().getIncomeAfterTaxes(toBeDestroyed.getType()) > averageIncome) {
                // this is a more valuable type of goods
                return false;
            } else {
                return true;
            }
        }
    }

    /**
     * Decides whether to accept an Indian demand, or not.
     *
     * @param unit The unit making demands.
     * @param colony The colony where demands are being made.
     * @param goods The goods demanded.
     * @param gold The amount of gold demanded.
     * @return <code>true</code> if this <code>AIPlayer</code> accepts the
     *         indian demand and <code>false</code> otherwise.
     */
    public boolean acceptIndianDemand(Unit unit, Colony colony, Goods goods, int gold) {
        // TODO: make a better choice
        if (goods.getType().isMilitaryGoods()) {
            return false;
        } else {
            return true;
        }
    }

}