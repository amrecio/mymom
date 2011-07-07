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

package net.sf.freecol.server.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.server.ai.mission.BuildColonyMission;
import net.sf.freecol.server.ai.mission.CashInTreasureTrainMission;
import net.sf.freecol.server.ai.mission.Mission;
import net.sf.freecol.server.ai.mission.PioneeringMission;
import net.sf.freecol.server.ai.mission.ScoutingMission;
import net.sf.freecol.server.ai.mission.TransportMission;
import net.sf.freecol.server.ai.mission.UnitWanderHostileMission;
import net.sf.freecol.server.ai.mission.WishRealizationMission;


/**
 * Objects of this class contains AI-information for a single {@link
 * net.sf.freecol.common.model.Player} and is used for controlling
 * this Player.
 *
 * @deprecated Currently unused, outdated copy of {@link AIPlayer}. Eventual
 * specialization of AI should extend AIPlayer, see {@link ColonialAIPlayer}.
 *
 * The method {@link #startWorking} gets called by the
 * {@link AIInGameInputHandler} when it is this player's turn.
 */
public class REFAIPlayer extends EuropeanAIPlayer {

    private static final Logger logger = Logger.getLogger(REFAIPlayer.class.getName());


    /**
     *
     * Tells this <code>AIPlayer</code> to make decisions. The
     * <code>AIPlayer</code> is done doing work this turn when this method
     * returns.
     */
    public void startWorking() {
        logger.fine("Entering AI code for: " + getPlayer()
                    + ", year " + getGame().getTurn());
        sessionRegister.clear();
        clearAIUnits();
        if (!getPlayer().isWorkForREF()) return;
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

        // Create a datastructure for the worker wishes:
        java.util.Map<UnitType, ArrayList<Wish>> workerWishes = new HashMap<UnitType, ArrayList<Wish>>();
        if (getPlayer().isEuropean()) {
            Iterator<AIColony> aIterator = getAIColonyIterator();
            while (aIterator.hasNext()) {
                Iterator<Wish> wIterator = aIterator.next().getWishIterator();
                while (wIterator.hasNext()) {
                    Wish w = wIterator.next();
                    if (w instanceof WorkerWish && w.getTransportable() == null) {
                        UnitType unitType = ((WorkerWish) w).getUnitType();
                        ArrayList<Wish> wishes = workerWishes.get(unitType);
                        if (wishes == null) {
                            wishes = new ArrayList<Wish>();
                            workerWishes.put(unitType, wishes);
                        }
                        wishes.add(w);
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

            EquipmentType toolsType = getGame().getSpecification().getEquipmentType("model.equipment.tools");
            if (unit.canCarryTreasure()) {
                aiUnit.setMission(new CashInTreasureTrainMission(getAIMain(), aiUnit));
            } else if (unit.hasAbility("model.ability.scoutIndianSettlement") &&
                       ScoutingMission.isValid(aiUnit)) {
                aiUnit.setMission(new ScoutingMission(getAIMain(), aiUnit));
            } else if ((unit.isOffensiveUnit() || unit.isDefensiveUnit())
                       && (!unit.isColonist() || unit.hasAbility("model.ability.expertSoldier") ||
                           getGame().getTurn().getNumber() > 5)) {
                giveMilitaryMission(aiUnit);
            } else if (unit.getEquipment().containsKey(toolsType)
                       && PioneeringMission.isValid(aiUnit)) {
                aiUnit.setMission(new PioneeringMission(getAIMain(), aiUnit));
            } else if (unit.isColonist()) {
                /*
                 * Motivated by (speed) performance: This map stores the
                 * distance between the unit and the destination of a Wish:
                 */
                HashMap<Location, Integer> distances = new HashMap<Location, Integer>(121);
                for (ArrayList<Wish> al : workerWishes.values()) {
                    for (Wish w : al) {
                        if (!distances.containsKey(w.getDestination())) {
                            distances.put(w.getDestination(), unit.getTurnsToReach(w.getDestination()));
                        }
                    }
                }

                // Check if this unit is needed as an expert (using:
                // "WorkerWish"):
                ArrayList<Wish> wishList = workerWishes.get(unit.getType());
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
                    for (List<Wish> wishes : workerWishes.values()) {
                        for (int j = 0; j < wishes.size(); j++) {
                            WorkerWish ww = (WorkerWish) wishes.get(j);
                            if (ww.getTransportable() != null) {
                                wishes.remove(j);
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

                    boolean isUnitOnCarrier = aiUnit.getUnit().isOnCarrier();
                    if (isUnitOnCarrier) {
                        AIUnit carrier = getAIUnit((Unit) aiUnit.getUnit().getLocation());

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
        return false;
    }
}
