package net.sf.freecol.common.model;

import java.util.Locale;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.util.test.FreeColTestCase;

public class GoodsTest extends FreeColTestCase {

    public static final String COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    public static boolean updateLocale = true;

    public void setUp() {
        if (updateLocale) {
            updateLocale = false;
            Messages.setMessageBundle(Locale.US);
        }
    }

    public void testGoodsGameLocationIntInt() {

        Map map = getTestMap(Tile.PLAINS);

        Unit wagon = new Unit(getGame(), map.getTile(9, 10), getGame().getPlayer(Player.DUTCH), Unit.WAGON_TRAIN,
                Unit.ACTIVE);

        Goods g = new Goods(getGame(), wagon, Goods.COTTON, 75);

        assertEquals(wagon, g.getLocation());
        assertEquals(Goods.COTTON, g.getType());
        assertEquals(75, g.getAmount());
    }

    public void testSetOwner() {

        try {
            Map map = getTestMap(Tile.PLAINS);

            Unit wagon = new Unit(getGame(), map.getTile(9, 10), getGame().getPlayer(Player.DUTCH), Unit.WAGON_TRAIN,
                    Unit.ACTIVE);

            Goods g = new Goods(getGame(), wagon, Goods.COTTON, 75);

            g.setOwner(getGame().getCurrentPlayer());

            fail("Should not allow setOwner");
        } catch (UnsupportedOperationException e) {
            // Okay to throw exception.
        }
    }

    public void testToString() {

        Messages.setMessageBundle(Locale.ENGLISH);

        Map map = getTestMap(Tile.PLAINS);

        Unit wagon = new Unit(getGame(), map.getTile(9, 10), getGame().getPlayer(Player.DUTCH), Unit.WAGON_TRAIN,
                Unit.ACTIVE);

        Goods g = new Goods(getGame(), wagon, Goods.COTTON, 75);

        assertEquals("75 Cotton", g.toString());
    }

    public void testGetName() {

        Locale.setDefault(Locale.ENGLISH);

        Goods g = new Goods(Goods.COTTON);

        assertEquals("Cotton", g.getName());

        assertEquals("Cotton (boycotted)", g.getName(false));

        assertEquals("Cotton", g.getName(true));

        // Same as getName(int, boolean)
        assertEquals(g.getName(), Goods.getName(Goods.COTTON));
        assertEquals(g.getName(false), Goods.getName(Goods.COTTON, false));
        assertEquals(g.getName(true), Goods.getName(Goods.COTTON, true));

    }

    public void testSetName() {
        // TODO Goods should not be Nameable, only Named
    }

    public void testGetTile() {
        Game game = getStandardGame();
        Player dutch = getGame().getPlayer(Player.DUTCH);
        Map map = getTestMap(Tile.PLAINS);
        game.setMap(map);

        // Check in a colony
        map.getTile(5, 8).setBonus(true);
        map.getTile(5, 8).setExploredBy(dutch, true);
        map.getTile(6, 8).setExploredBy(dutch, true);

        Unit soldier = new Unit(game, map.getTile(6, 8), dutch, Unit.VETERAN_SOLDIER, Unit.ACTIVE, true, false, 0,
                false);

        Colony colony = new Colony(game, dutch, "New Amsterdam", soldier.getTile());
        soldier.setWorkType(Goods.FOOD);
        soldier.buildColony(colony);

        // Create goods
        Goods cotton = new Goods(getGame(), null, Goods.COTTON, 75);

        // Check if location null
        assertEquals(null, cotton.getTile());

        // Check in colony
        cotton.setLocation(colony);
        assertEquals(colony.getTile(), cotton.getTile());
        assertEquals(75, colony.getGoodsCount(Goods.COTTON));

        // Check in a wagon
        Unit wagon = new Unit(getGame(), map.getTile(9, 10), dutch, Unit.WAGON_TRAIN, Unit.ACTIVE);
        cotton.setLocation(wagon);
        assertEquals(map.getTile(9, 10), cotton.getTile());
    }

    public void testGetRawMaterial() {
        assertEquals(-1, Goods.getRawMaterial(Goods.COTTON));
        assertEquals(Goods.COTTON, Goods.getRawMaterial(Goods.CLOTH));
    }

    public void testGetManufactoredGoods() {
        assertEquals(-1, Goods.getManufactoredGoods(Goods.CLOTH));
        assertEquals(Goods.CLOTH, Goods.getManufactoredGoods(Goods.COTTON));
    }

    public void testIsFarmedGoods() {
        assertEquals(false, Goods.isFarmedGoods(Goods.BELLS));
        assertEquals(true, Goods.isFarmedGoods(Goods.COTTON));
        assertEquals(false, Goods.isFarmedGoods(Goods.CLOTH));
    }

    public void testSetGetLocation() {
        Game game = getStandardGame();
        Player dutch = getGame().getPlayer(Player.DUTCH);
        Map map = getTestMap(Tile.PLAINS, true);
        game.setMap(map);
        Colony colony = getStandardColony();

        // Check in Colony
        Goods cotton = new Goods(getGame(), null, Goods.COTTON, 75);
        cotton.setLocation(colony);
        assertEquals(colony, cotton.getLocation());
        assertEquals(75, colony.getGoodsCount(Goods.COTTON));

        // Check in a wagon
        Unit wagon = new Unit(getGame(), map.getTile(9, 10), dutch, Unit.WAGON_TRAIN, Unit.ACTIVE);
        cotton.setLocation(wagon);
        assertEquals(wagon, cotton.getLocation());

        // Can only add to GoodsContainers
        try {
            cotton.setLocation(map.getTile(9, 10));
            fail();
        } catch (IllegalArgumentException e) {
            // Okay to throw exception.
        }
    }

    public void testGetTakeSpace() {
        Map map = getTestMap(Tile.PLAINS, true);

        Unit wagon = new Unit(getGame(), map.getTile(9, 10), getGame().getPlayer(Player.DUTCH), Unit.WAGON_TRAIN,
                Unit.ACTIVE);

        Goods cotton = new Goods(getGame(), wagon, Goods.COTTON, 75);

        assertEquals(1, cotton.getTakeSpace());
    }

    public void testSetGetAmount() {
        Map map = getTestMap(Tile.PLAINS, true);

        Unit wagon = new Unit(getGame(), map.getTile(9, 10), getGame().getPlayer(Player.DUTCH), Unit.WAGON_TRAIN,
                Unit.ACTIVE);

        Goods cotton = new Goods(getGame(), wagon, Goods.COTTON, 75);

        assertEquals(75, cotton.getAmount());

        cotton.setAmount(-10);

        assertEquals(-10, cotton.getAmount());

        cotton.setAmount(100000);

        assertEquals(100000, cotton.getAmount());

    }

    public void testAdjustAmount() {

        Map map = getTestMap(Tile.PLAINS, true);

        Unit wagon = new Unit(getGame(), map.getTile(9, 10), getGame().getPlayer(Player.DUTCH), Unit.WAGON_TRAIN,
                Unit.ACTIVE);

        Goods cotton = new Goods(getGame(), wagon, Goods.COTTON, 75);

        assertEquals(75, cotton.getAmount());

        cotton.adjustAmount();

        cotton.setAmount(-10);

        assertEquals(-10, cotton.getAmount());

        cotton.setAmount(100000);

        assertEquals(100000, cotton.getAmount());

    }

    public void testLoadOnto() {
        Game game = getStandardGame();
        Map map = getTestMap(Tile.PLAINS, true);
        game.setMap(map);
        Colony colony = getStandardColony();

        Unit wagonInColony = new Unit(getGame(), colony.getTile(), getGame().getPlayer(Player.DUTCH), Unit.WAGON_TRAIN,
                Unit.ACTIVE);
        Unit wagonNotInColony = new Unit(getGame(), map.getTile(10, 10), getGame().getPlayer(Player.DUTCH),
                Unit.WAGON_TRAIN, Unit.ACTIVE);

        // Check that it does not work if current Location == null
        Goods cotton = new Goods(getGame(), null, Goods.COTTON, 75);
        try {
            cotton.loadOnto(wagonInColony);
            fail();
        } catch (IllegalStateException e) {
        }
        try {
            cotton.loadOnto(wagonNotInColony);
            fail();
        } catch (IllegalStateException e) {
        }

        // Check from colony to wagon train
        cotton.setLocation(colony);
        cotton.loadOnto(wagonInColony);
        assertEquals(wagonInColony, cotton.getLocation());

        try {
            cotton.loadOnto(wagonNotInColony);
            fail();
        } catch (IllegalStateException e) {
        }

        // Check from unit to unit
        wagonInColony.setLocation(wagonNotInColony.getTile());
        cotton.loadOnto(wagonNotInColony);
        assertEquals(wagonNotInColony, cotton.getLocation());

        {// Check with Europe
            Europe europe = getGame().getPlayer(Player.DUTCH).getEurope();
            Unit privateer1 = new Unit(getGame(), europe, getGame().getPlayer(Player.DUTCH), Unit.PRIVATEER,
                    Unit.ACTIVE);
            Unit privateer2 = new Unit(getGame(), europe, getGame().getPlayer(Player.DUTCH), Unit.PRIVATEER,
                    Unit.ACTIVE);

            // While source in Europe, target in Europe
            cotton.setLocation(privateer1);
            cotton.loadOnto(privateer2);
            assertEquals(privateer2, cotton.getLocation());

            // While source moving from America, target in Europe
            cotton.setLocation(privateer1);
            privateer1.moveToAmerica();
            try {
                cotton.loadOnto(privateer2);
                fail();
            } catch (IllegalStateException e) {
            }

            // While source moving to America, target in Europe
            cotton.setLocation(privateer1);
            privateer1.moveToEurope();
            try {
                cotton.loadOnto(privateer2);
                fail();
            } catch (IllegalStateException e) {
            }

            // While source in Europe, target moving to America
            privateer1.setLocation(europe);
            privateer2.moveToAmerica();

            cotton.setLocation(privateer1);
            try {
                cotton.loadOnto(privateer2);
                fail();
            } catch (IllegalStateException e) {
            }

            // While source moving to America, target moving to America
            cotton.setLocation(privateer1);
            privateer1.moveToAmerica();
            try {
                cotton.loadOnto(privateer2);
                fail();
            } catch (IllegalStateException e) {
            }

            // While source moving from America, target moving to America
            cotton.setLocation(privateer1);
            privateer1.moveToEurope();
            try {
                cotton.loadOnto(privateer2);
                fail();
            } catch (IllegalStateException e) {
            }

            // While source in Europe, target moving from America
            privateer1.setLocation(europe);
            privateer2.moveToEurope();

            cotton.setLocation(privateer1);
            try {
                cotton.loadOnto(privateer2);
                fail();
            } catch (IllegalStateException e) {
            }

            // While source moving to America, target moving from America
            cotton.setLocation(privateer1);
            privateer1.moveToAmerica();
            try {
                cotton.loadOnto(privateer2);
                fail();
            } catch (IllegalStateException e) {
            }

            // While source moving from America, target moving from America
            cotton.setLocation(privateer1);
            privateer1.moveToEurope();
            try {
                cotton.loadOnto(privateer2);
                fail();
            } catch (IllegalStateException e) {
            }
        }
    }

    public void testUnload() {
        Game game = getStandardGame();
        Map map = getTestMap(Tile.PLAINS, true);
        game.setMap(map);
        Colony colony = getStandardColony();

        Unit wagonInColony = new Unit(getGame(), colony.getTile(), getGame().getPlayer(Player.DUTCH), Unit.WAGON_TRAIN,
                Unit.ACTIVE);
        Unit wagonNotInColony = new Unit(getGame(), map.getTile(10, 10), getGame().getPlayer(Player.DUTCH),
                Unit.WAGON_TRAIN, Unit.ACTIVE);

        Goods cotton = new Goods(getGame(), null, Goods.COTTON, 75);

        // Unload in Colony
        cotton.setLocation(wagonInColony);
        cotton.unload();
        assertEquals(colony, cotton.getLocation());

        // Unload outside of colony does not work
        cotton.setLocation(wagonNotInColony);
        try {
            cotton.unload();
            fail();
        } catch (IllegalStateException e) {
        }

        // Not allowed to unload in Europe
        Unit privateer = new Unit(getGame(), getGame().getPlayer(Player.DUTCH).getEurope(), getGame().getPlayer(
                Player.DUTCH), Unit.PRIVATEER, Unit.ACTIVE);
        cotton.setLocation(privateer);
        try {
            cotton.unload();
            fail();
        } catch (IllegalStateException e) {
        }

        // While moving from America
        cotton.setLocation(privateer);
        privateer.moveToAmerica();
        try {
            cotton.unload();
            fail();
        } catch (IllegalStateException e) {
        }

        // While moving to America
        cotton.setLocation(privateer);
        privateer.moveToEurope();
        try {
            cotton.unload();
            fail();
        } catch (IllegalStateException e) {
        }
    }
}
