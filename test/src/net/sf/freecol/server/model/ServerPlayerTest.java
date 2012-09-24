/**
 *  Copyright (C) 2002-2012  The FreeCol Team
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

package net.sf.freecol.server.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.server.control.InGameController;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.MockPseudoRandom;


public class ServerPlayerTest extends FreeColTestCase {	

    private static final GoodsType cottonType
        = spec().getGoodsType("model.goods.cotton");
    private static final GoodsType foodType
        = spec().getPrimaryFoodType();
    private static final GoodsType musketsType
        = spec().getGoodsType("model.goods.muskets");
    private static final GoodsType silverType
        = spec().getGoodsType("model.goods.silver");

    private static final TileType plains
        = spec().getTileType("model.tile.plains");
    
    private static final UnitType colonistType
        = spec().getUnitType("model.unit.freeColonist");
    private static final UnitType wagonTrainType
        = spec().getUnitType("model.unit.wagonTrain");
    private static final UnitType caravelType
        = spec().getUnitType("model.unit.caravel");
    private static final UnitType galleonType
        = spec().getUnitType("model.unit.galleon");
    private static final UnitType privateerType
        = spec().getUnitType("model.unit.privateer");


    @Override
    public void tearDown() throws Exception {
        ServerTestHelper.stopServerGame();
        super.tearDown();
    }

    /**
     * If we wait a number of turns after selling, the market should
     * recover and finally settle back to the initial levels.  Also
     * test that selling reduces the price for other players.
     */
    public void testMarketRecovery() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        InGameController igc = ServerTestHelper.getInGameController();

        ServerPlayer french = (ServerPlayer) game.getPlayer("model.nation.french");
        ServerPlayer english = (ServerPlayer) game.getPlayer("model.nation.english");
        Market frenchMarket = french.getMarket();
        Market englishMarket = english.getMarket();
        int frenchGold = french.getGold();
        int silverPrice = spec().getInitialPrice(silverType);

        // Sell lightly in the English market to check that the good
        // is now considered "traded".
        english.sell(null, silverType, 1, new Random());
        assertTrue(englishMarket.hasBeenTraded(silverType));
        int englishAmount = englishMarket.getAmountInMarket(silverType);

        // Sell heavily in the French market, price should drop.
        french.sell(null, silverType, 200, new Random());
        assertEquals(frenchGold + silverPrice * 200, french.getGold());
        assertTrue(frenchMarket.hasBeenTraded(silverType));
        assertTrue(frenchMarket.getSalePrice(silverType, 1) < silverPrice);

        // Price might have dropped in the English market too, but
        // not as much as for the French.
        assertTrue("English silver increases due to French sales",
            englishMarket.getAmountInMarket(silverType) > englishAmount);
        assertTrue("English silver price might drop due to French sales",
            englishMarket.getSalePrice(silverType, 1) <= silverPrice);
        assertTrue("English silver price should drop less than French",
            englishMarket.getSalePrice(silverType, 1)
            >= frenchMarket.getSalePrice(silverType, 1));

        // Pretend time is passing.
        // Have to advance time as yearly goods removal is initially low.
        game.setTurn(new Turn(200));
        List<Integer> setValues = new ArrayList<Integer>();
        setValues.add(20);
        MockPseudoRandom mockRandom = new MockPseudoRandom(setValues, true);
        ServerTestHelper.setRandom(mockRandom);
        boolean frenchRecovered = false;
        boolean englishRecovered = false;
        for (int i = 0; i < 100; i++) {
            igc.yearlyGoodsAdjust((ServerPlayer) french);
            if (frenchMarket.getSalePrice(silverType, 1) >= silverPrice) {
                frenchRecovered = true;
            }
            igc.yearlyGoodsAdjust((ServerPlayer) english);
            if (englishMarket.getSalePrice(silverType, 1) >= silverPrice) {
                englishRecovered = true;
            }
        }

        // Prices should have recovered.
        assertTrue("French silver price should have recovered",
                   frenchRecovered);
        assertTrue("English silver price should have recovered",
                   englishRecovered);
    }

    public void testHasExploredTile() {
        Map map = getTestMap();
        Game game = ServerTestHelper.startServerGame(map);
        
        ServerPlayer dutch = (ServerPlayer) game.getPlayer("model.nation.dutch");
        ServerPlayer french = (ServerPlayer) game.getPlayer("model.nation.french");
        Tile tile1 = map.getTile(6, 8);
        Tile tile2 = map.getTile(8, 6);
        assertFalse("Setup error, tile1 should not be explored by dutch player",dutch.hasExplored(tile1));
        assertFalse("Setup error, tile1 should not be explored by french player",french.hasExplored(tile1));
        assertFalse("Setup error, tile2 should not be explored by dutch player",dutch.hasExplored(tile2));
        assertFalse("Setup error, tile2 should not be explored by french player",french.hasExplored(tile2));

        new ServerUnit(game, tile1, dutch, colonistType);
        new ServerUnit(game, tile2, french, colonistType);
        assertTrue("Tile1 should be explored by dutch player",dutch.hasExplored(tile1));
        assertFalse("Tile1 should not be explored by french player",french.hasExplored(tile1));
        assertFalse("Tile2 should not be explored by dutch player",dutch.hasExplored(tile2));
        assertTrue("Tile2 should be explored by french player",french.hasExplored(tile2));
    }

    public void testLoadInColony() {
        Map map = getTestMap();
        Game game = ServerTestHelper.startServerGame(map);
        InGameController igc = ServerTestHelper.getInGameController();
        
        Colony colony = getStandardColony();
        ServerPlayer dutch = (ServerPlayer) game.getPlayer("model.nation.dutch");
        Unit wagonInColony = new ServerUnit(game, colony.getTile(), dutch,
                                            wagonTrainType);
        Unit wagonNotInColony = new ServerUnit(game, map.getTile(10, 10), dutch,
                                               wagonTrainType);
        Goods cotton = new Goods(game, null, cottonType, 75);

        // Check if location null
        assertEquals(null, cotton.getTile());

        // Check that it does not work if current Location == null
        try {
            igc.moveGoods(cotton, wagonInColony);
            fail();
        } catch (IllegalStateException e) {
        }
        try {
            igc.moveGoods(cotton, wagonNotInColony);
            fail();
        } catch (IllegalStateException e) {
        }

        // Check wagon to colony
        cotton.setLocation(wagonInColony);
        igc.moveGoods(cotton, colony);
        assertEquals(cotton.getLocation(), colony);
        assertEquals(75, colony.getGoodsCount(cottonType));

        // Check from colony to wagon train
        igc.moveGoods(cotton, wagonInColony);
        assertEquals(wagonInColony, cotton.getLocation());
        assertEquals(0, colony.getGoodsCount(cottonType));

        // Check failure units not co-located
        try {
            igc.moveGoods(cotton, wagonNotInColony);
            fail();
        } catch (IllegalStateException e) {
        }

        // Check failure to non-GoodsContainer (Tile)
        try {
            igc.moveGoods(cotton, map.getTile(9, 10));
            fail();
        } catch (IllegalStateException e) {
        }

        // Check from unit to unit
        wagonInColony.setLocation(wagonNotInColony.getTile());
        igc.moveGoods(cotton, wagonNotInColony);
        assertEquals(wagonNotInColony, cotton.getLocation());
    }

    public void testLoadInEurope() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        InGameController igc = ServerTestHelper.getInGameController();

        ServerPlayer dutch = (ServerPlayer) game.getPlayer("model.nation.dutch");
        Goods cotton = new Goods(game, null, cottonType, 75);
        Europe europe = dutch.getEurope();
        Map america = game.getMap();
        Unit privateer1 = new ServerUnit(game, europe, dutch, privateerType);
        Unit privateer2 = new ServerUnit(game, europe, dutch, privateerType);

        // While source in Europe, target in Europe
        cotton.setLocation(privateer1);
        igc.moveGoods(cotton, privateer2);
        assertEquals(privateer2, cotton.getLocation());

        // Can not unload directly to Europe
        try {
            igc.moveGoods(cotton, europe);
            fail();
        } catch (IllegalStateException e) {
        }

        // While source moving from America, target in Europe
        cotton.setLocation(privateer1);
        assertEquals(europe, privateer1.getLocation());
        igc.moveTo(dutch, privateer1, america);
        try {
            igc.moveGoods(cotton, privateer2);
            fail();
        } catch (IllegalStateException e) {
        }

        // While source moving to America, target in Europe
        cotton.setLocation(privateer1);
        igc.moveTo(dutch, privateer1, europe);
        try {
            igc.moveGoods(cotton, privateer2);
            fail();
        } catch (IllegalStateException e) {
        }

        // While source in Europe, target moving to America
        privateer1.setLocation(europe);
        igc.moveTo(dutch, privateer2, america);
        cotton.setLocation(privateer1);
        try {
            igc.moveGoods(cotton, privateer2);
            fail();
        } catch (IllegalStateException e) {
        }

        // While source moving to America, target moving to America
        cotton.setLocation(privateer1);
        igc.moveTo(dutch, privateer1, america);
        try {
            igc.moveGoods(cotton, privateer2);
            fail();
        } catch (IllegalStateException e) {
        }

        // While source moving from America, target moving to America
        cotton.setLocation(privateer1);
        igc.moveTo(dutch, privateer1, europe);
        try {
            igc.moveGoods(cotton, privateer2);
            fail();
        } catch (IllegalStateException e) {
        }

        // While source in Europe, target moving from America
        privateer1.setLocation(europe);
        igc.moveTo(dutch, privateer2, europe);

        cotton.setLocation(privateer1);
        try {
            igc.moveGoods(cotton, privateer2);
            fail();
        } catch (IllegalStateException e) {
        }

        // While source moving to America, target moving from America
        cotton.setLocation(privateer1);
        igc.moveTo(dutch, privateer1, america);
        try {
            igc.moveGoods(cotton, privateer2);
            fail();
        } catch (IllegalStateException e) {
        }

        // While source moving from America, target moving from America
        cotton.setLocation(privateer1);
        igc.moveTo(dutch, privateer1, europe);
        try {
            igc.moveGoods(cotton, privateer2);
            fail();
        } catch (IllegalStateException e) {
        }
    }

    public void testCheckGameOverNoUnits() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        ServerPlayer dutch = (ServerPlayer)game.getPlayer("model.nation.dutch");

        dutch.setGold(0);
        assertEquals("Should not have units", 0, dutch.getUnits().size());
        assertEquals("Should be game over due to no carrier", -1,
                     dutch.checkForDeath());
    }

    public void testCheckNoGameOverEnoughMoney() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        ServerPlayer dutch = (ServerPlayer)game.getPlayer("model.nation.dutch");

        dutch.setGold(10000);
        assertEquals("Should not be game, enough money", 0,
                     dutch.checkForDeath());
    }

    public void testCheckNoGameOverHasColonistInNewWorld() {
        Map map = getTestMap();
        Game game = ServerTestHelper.startServerGame(map);
        ServerPlayer dutch = (ServerPlayer)game.getPlayer("model.nation.dutch");
        dutch.setGold(0);

        new ServerUnit(game, map.getTile(4, 7), dutch, colonistType);
        assertEquals("Should not be game over, has units", 0,
                     dutch.checkForDeath());
    }

    public void testCheckGameOver1600Threshold() {
        Map map = getTestMap();
        Game game = ServerTestHelper.startServerGame(map);
        ServerPlayer dutch = (ServerPlayer)game.getPlayer("model.nation.dutch");
        dutch.setGold(0);

        new ServerUnit(game, dutch.getEurope(), dutch, galleonType);
        assertEquals("Should have 1 unit", 1, dutch.getUnits().size());
        assertEquals("Should not be game over, not 1600 yet, autorecruit", 1,
                     dutch.checkForDeath());

        new ServerUnit(game, dutch.getEurope(), dutch, colonistType);
        assertEquals("Should have 2 units", 2, dutch.getUnits().size());
        assertEquals("Should not be game over, not 1600 yet", 0,
                     dutch.checkForDeath());

        game.setTurn(new Turn(1600));
        assertEquals("Should be game over, no new world presence >= 1600", -1,
                     dutch.checkForDeath());
    }

    public void testCheckGameOverUnitsGoingToEurope() {
        Map map = getTestMap(spec().getTileType("model.tile.highSeas"));
        Game game = ServerTestHelper.startServerGame(map);
        InGameController igc = ServerTestHelper.getInGameController();
        ServerPlayer dutch = (ServerPlayer)game.getPlayer("model.nation.dutch");
        dutch.setGold(0);

        Unit galleon = new ServerUnit(game, map.getTile(6, 8), dutch,
                                      galleonType);
        Unit colonist = new ServerUnit(game, galleon, dutch, colonistType);
        assertTrue("Colonist should be aboard the galleon",
                   colonist.getLocation() == galleon);
        assertEquals("Galleon should have a colonist onboard",
                     1, galleon.getUnitCount());
        igc.moveTo(dutch, galleon, dutch.getEurope());

        assertEquals("Should not be game over, units between new world and europe", 0,
                     dutch.checkForDeath());

        game.setTurn(new Turn(1600));
        assertEquals("Should be game over, no new world presence >= 1600", -1,
                     dutch.checkForDeath());
    }

    public void testCheckGameOverUnitsGoingToNewWorld() {
        Map map = getTestMap();
        Game game = ServerTestHelper.startServerGame(map);
        InGameController igc = ServerTestHelper.getInGameController();
        ServerPlayer dutch = (ServerPlayer)game.getPlayer("model.nation.dutch");
        dutch.setGold(0);

        Unit galleon = new ServerUnit(game, dutch.getEurope(), dutch,
                                      galleonType);
        Unit colonist = new ServerUnit(game, galleon, dutch, colonistType);
        assertEquals("Colonist should be aboard the galleon", galleon,
                     colonist.getLocation());
        assertEquals("Galleon should have a colonist onboard", 1,
                     galleon.getUnitCount());
        igc.moveTo(dutch, galleon, map);

        assertEquals("Should not be game over, units between new world and europe", 0,
                     dutch.checkForDeath());

        game.setTurn(new Turn(1600));
        assertEquals("Should be game over, no new world presence >= 1600", -1,
                     dutch.checkForDeath());
    }

    public void testSellingMakesPricesFall() {
        Game g = ServerTestHelper.startServerGame(getTestMap());
        ServerPlayer p = (ServerPlayer)g.getPlayer("model.nation.dutch");

        Market dm = p.getMarket();
        int previousGold = p.getGold();
        int price = spec().getInitialPrice(silverType);
        p.sell(null, silverType, 1000, new Random());

        assertEquals(previousGold + price * 1000, p.getGold());
        assertTrue(dm.getSalePrice(silverType, 1) < price);
    }

    public void testBuyingMakesPricesRaise() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        ServerPlayer player = (ServerPlayer)game.getPlayer("model.nation.dutch");

        Market dm = player.getMarket();
        player.modifyGold(1000000);
        int price = dm.getCostToBuy(foodType);
        player.buy(new GoodsContainer(game, player.getEurope()), foodType,
                   10000, new Random());

        assertEquals(1000000 - 10000 * price, player.getGold());
        assertTrue(dm.getBidPrice(foodType, 1) > price);
    }

    /**
     * Helper Method for finding out how much of a good to sell until
     * the price drops.
     */
    public int sellUntilPriceDrop(Game game, ServerPlayer player,
                                  GoodsType type) {
        Random random = new Random();

        int result = 0;

        Market market = player.getMarket();

        int price = market.getSalePrice(type, 1);

        if (price == 0)
            throw new IllegalArgumentException("Price is already 0 for selling " + type);

        while (price == market.getSalePrice(type, 1)){
            player.sell(null, type, 10, random);
            result++;
        }
        return result;
    }

    /*
     * Helper method for finding out how much to buy of a good before the prices
     * rises.
     */
    public int buyUntilPriceRise(Game game, ServerPlayer player,
                                 GoodsType type) {
        Game g = ServerTestHelper.startServerGame(getTestMap());
        Random random = new Random();

        int result = 0;

        Market market = player.getMarket();

        int price = market.getBidPrice(type, 1);

        if (price == 20)
            throw new IllegalArgumentException("Price is already 20 for buying " + type);

        GoodsContainer container = new GoodsContainer(game, player.getEurope());
        while (price == market.getBidPrice(type, 1)) {
            player.buy(container, type, 10, random);
            result++;
        }
        return result;
    }

    /**
     * Assert that the dutch nation has more stable prices than the other
     * nations
     */
    public void testDutchMarket() {

        Game game = getStandardGame();
        ServerPlayer dutch = (ServerPlayer)game.getPlayer("model.nation.dutch");
        ServerPlayer french = (ServerPlayer)game.getPlayer("model.nation.french");
        assertEquals("model.nationType.trade", dutch.getNationType().getId());
        assertFalse(dutch.getNationType().getModifierSet("model.modifier.tradeBonus").isEmpty());
        assertFalse(dutch.getModifierSet("model.modifier.tradeBonus").isEmpty());

        {// Test that the dutch can sell more goods until the price drops
            int dutchSellAmount = sellUntilPriceDrop(game, dutch, silverType);

            Game g2 = getStandardGame();
            ServerPlayer french2 = (ServerPlayer)g2.getPlayer("model.nation.french");
            int frenchSellAmount = sellUntilPriceDrop(g2, french2, silverType);

            assertTrue(dutchSellAmount > frenchSellAmount);
        }
        {// Test that the dutch can buy more goods until the price rises
            dutch.modifyGold(10000);
            french.modifyGold(10000);
            int dutchBuyAmount = buyUntilPriceRise(getStandardGame(), dutch, musketsType);

            int frenchBuyAmount = buyUntilPriceRise(getStandardGame(), french, musketsType);

            assertTrue(dutchBuyAmount > frenchBuyAmount);
        }
    }
}
