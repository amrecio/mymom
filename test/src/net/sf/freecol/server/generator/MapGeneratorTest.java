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

package net.sf.freecol.server.generator;

import java.util.Iterator;
import java.util.Vector;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.Specification;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.option.SelectOption;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.MockModelController;

public class MapGeneratorTest extends FreeColTestCase {

	public void testWithNoIndians() {

		Game g = new Game(new MockModelController());
		Specification s = FreeCol.getSpecification();

		// A new game does not have a map yet
		assertEquals(null, g.getMap());

		MapGenerator gen = new MapGenerator();

		for (Nation n : s.getNations()) {
			if (n.getType().isEuropean() && !n.getType().isREF()) {
				g.addPlayer(new Player(g, n.getType().getName(), false, n));
			}
		}

		gen.createMap(g);

		// Check that the map is created at all
		assertNotNull(g.getMap());
	}

	public void testSinglePlayerOnSmallMap() {

		Game g = new Game(new MockModelController());

		// A new game does not have a map yet
		assertEquals(null, g.getMap());

		MapGenerator gen = new MapGenerator();
		((SelectOption) gen.getMapGeneratorOptions().getObject(
			MapGeneratorOptions.MAP_SIZE))
			.setValue(MapGeneratorOptions.MAP_SIZE_SMALL);

		Nation nation = FreeCol.getSpecification().getNation(
			"model.nation.dutch");

		g.addPlayer(new Player(g, nation.getType().getName(), false, nation));

		gen.createMap(g);

		// Check that the map is created at all
		assertNotNull(g.getMap());

		assertEquals(gen.getMapGeneratorOptions().getWidth(), g.getMap()
			.getWidth());
		assertEquals(gen.getMapGeneratorOptions().getHeight(), g.getMap()
			.getHeight());

	}

	public void testMapGenerator() {

		Game g = new Game(new MockModelController());

		// A new game does not have a map yet
		assertEquals(null, g.getMap());

		MapGenerator gen = new MapGenerator();

		Vector<Player> players = new Vector<Player>();

		for (Nation n : FreeCol.getSpecification().getNations()) {
			Player p;
			if (n.getType().isEuropean() && !n.getType().isREF()){
				p = new Player(g, n.getType().getName(), false, n);
			} else {
				p = new Player(g, n.getType().getName(), false, true, n);
			}
			g.addPlayer(p);
			players.add(p);
		}

		gen.createMap(g);
		
		// Check that the map is created at all
		assertNotNull(g.getMap());

		// Map of correct size?
		Map m = g.getMap();
		assertEquals(m.getWidth(), gen.getMapGeneratorOptions().getWidth());
		assertEquals(m.getHeight(), gen.getMapGeneratorOptions().getHeight());

		// Sufficient land?
		Iterator<Position> it = m.getWholeMapIterator();
		int land = 0;
		int total = 0;
		while (it.hasNext()) {
			Position p = it.next();
			Tile t = m.getTile(p);
			if (t.isLand())
				land++;
			total++;
		}
		// Land Mass requirement fulfilled?
		assertTrue(100 * land / total >= gen.getMapGeneratorOptions()
			.getLandMass());

		// Does the wholeMapIterator visit all fields?
		assertEquals(gen.getMapGeneratorOptions().getWidth()
			* gen.getMapGeneratorOptions().getHeight(), total);
	}

	/**
	 * Make sure that each tribe has exactely one capital
	 * 
	 */
	public void testIndianCapital() {

		Game g = new Game(new MockModelController());

		MapGenerator gen = new MapGenerator();

		Vector<Player> players = new Vector<Player>();

		for (Nation n : FreeCol.getSpecification().getNations()) {
			Player p;
			if (n.getType().isEuropean() && !n.getType().isREF()){
				p = new Player(g, n.getType().getName(), false, n);
			} else {
				p = new Player(g, n.getType().getName(), false, true, n);
			}
			g.addPlayer(p);
			players.add(p);
		}

		gen.createMap(g);

		// Check that the map is created at all
		assertNotNull(g.getMap());

		for (Player p : players) {
			if (!p.isIndian())
				continue;

			Iterator<Settlement> i = p.getIndianSettlementIterator();

			// Check that every indian player has exactely one capital if s/he
			// has at least one settlement.
			if (i.hasNext()) {
				int found = 0;
				while (i.hasNext()) {
					IndianSettlement s = (IndianSettlement) i.next();
					if (s.isCapital())
						found++;
				}

				assertEquals(1, found);
			}
		}
	}

}
