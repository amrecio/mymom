package net.sf.freecol.common.model;

import net.sf.freecol.common.Specification;
import net.sf.freecol.util.test.FreeColTestCase;

public class MarketTest extends FreeColTestCase {

	/*
	 * We should make sure that the selling a lot of small amounts yields the
	 * same prices as selling once in a big bunch.
	 * 
	 * Okay, maybe this test is not really good and we should split big
	 * transactions in one's of at most 100 units.
	 * 
	 */
	public void testSellingInLargeAndInLittle() {

		GoodsType silver = spec().getGoodsType("model.goods.Silver");

		int goldEarned;
		{
			Game g = getStandardGame();
			Player p = g.getPlayer("model.nation.dutch");
			Market dm = p.getMarket();

			int previousGold = p.getGold();

			dm.sell(silver, 1000, p);

			goldEarned = p.getGold() - previousGold;
		}

		{
			Game g = getStandardGame();
			Player p = g.getPlayer("model.nation.dutch");
			Market dm = p.getMarket();

			int previousGold = p.getGold();

			for (int i = 0; i < 1000; i++) {
				dm.sell(silver, 1, p);
			}

			assertEquals(goldEarned, p.getGold() - previousGold);
		}
	}

	public void testSellingMakesPricesFall() {
		Game g = getStandardGame();

		Player p = g.getPlayer("model.nation.dutch");

		Market dm = p.getMarket();

		Specification s = spec();

		GoodsType silver = s.getGoodsType("model.goods.Silver");

		int previousGold = p.getGold();

		int price = silver.getInitialSellPrice();

		dm.sell(silver, 1000, p);

		assertEquals(previousGold + price * 1000, p.getGold());

		assertTrue(dm.getSalePrice(silver, 1) < price);
	}

	public void testBuyingMakesPricesRaise() {
		Game g = getStandardGame();

		Player p = g.getPlayer("model.nation.dutch");

		Market dm = p.getMarket();

		Specification s = spec();

		GoodsType food = s.getGoodsType("model.goods.Food");

		p.setGold(1000000);

		int price = food.getInitialBuyPrice();

		dm.buy(food, 10000, p);

		assertEquals(1000000 - 10000 * price, p.getGold());

		assertTrue(dm.getBidPrice(food, 1) > price);
	}

	/**
	 * If we wait a number of turns, the market should recover and finally
	 * settle back to the initial levels.
	 */
	public void testMarketRecovery() {
		
		Game g = getStandardGame();
		g.setMap(getEmptyMap());

		Player p = g.getPlayer("model.nation.dutch");

		Market dm = p.getMarket();

		Specification s = spec();

		GoodsType silver = s.getGoodsType("model.goods.Silver");

		int previousGold = p.getGold();

		int price = silver.getInitialSellPrice();

		dm.sell(silver, 1000, p);

		assertEquals(previousGold + price * 1000, p.getGold());

		assertTrue(dm.getSalePrice(silver, 1) < price);
		
		// After 100 turns the prices should have recovered.
		for (int i = 0; i < 100; i++){
			g.newTurn();
		}
		
		assertTrue(dm.getSalePrice(silver, 1) >= price);
	}

	/**
	 * Helper Method for finding out how much of a good to sell until the price drops.
	 */
	public int sellUntilPriceDrop(Game game, Player player, GoodsType type){

		int result = 0;
		
		Market dutchMarket = player.getMarket();

		int price = dutchMarket.getSalePrice(type, 1);
		
		if (price == 0)
			throw new IllegalArgumentException("Price is already 0 for selling " + type);
		
		while (price == dutchMarket.getSalePrice(type, 1)){
			dutchMarket.sell(type, 1, player);
			result++;
		}
		return result;
	}
	
	/*
	 * Helper method for finding out how much to buy of a good before the prices
	 * rises.
	 */
	public int buyUntilPriceRise(Game game, String nation, GoodsType type) {

		int result = 0;

		Player player = game.getPlayer(nation);

		Market dutchMarket = player.getMarket();

		int price = dutchMarket.getBidPrice(type, 1);

		if (price == 20)
			throw new IllegalArgumentException("Price is already 20 for buying " + type);

		while (price == dutchMarket.getBidPrice(type, 1)) {
			dutchMarket.buy(type, 1, player);
			result++;
		}
		return result;
	}
	
	/**
	 * Assert that the dutch nation has more stable prices than the other
	 * nations
	 */
	public void testDutchMarket() {

		{// Test that the dutch can sell more goods until the price drops
			GoodsType silver = spec().getGoodsType("model.goods.Silver");
			Game g = getStandardGame();
			int dutchSellAmount = sellUntilPriceDrop(g, g
				.getPlayer("model.nation.dutch"), silver);

			Game g2 = getStandardGame();
			int frenchSellAmount = sellUntilPriceDrop(g2, g2
				.getPlayer("model.nation.french"), silver);

			assertTrue(dutchSellAmount > frenchSellAmount);
		}
		{// Test that the dutch can buy more goods until the price rises
			GoodsType muskets = spec().getGoodsType("model.goods.Muskets");
			int dutchSellAmount = buyUntilPriceRise(getStandardGame(),
				"model.nation.dutch", muskets);

			int frenchSellAmount = buyUntilPriceRise(getStandardGame(),
				"model.nation.french", muskets);

			assertTrue(dutchSellAmount > frenchSellAmount);
		}
	}

	/**
	 * Make sure that the initial prices are correctly taken from the
	 * specification
	 */
	public void testInitialMarket() {

		Game g = getStandardGame();

		Player p = g.getPlayer("model.nation.dutch");

		Market dm = p.getMarket();

		Specification s = spec();

		for (GoodsType good : s.getGoodsTypeList()) {
			assertEquals(good.getInitialBuyPrice(), dm.costToBuy(good));
			assertEquals(good.getInitialSellPrice(), dm.paidForSale(good));
		}
	}

	/*
	 * Test that buying goods raise the price for all players, and that selling
	 * the good will make the prices fall for everybody.
	 */
	public void testSharedMarket() {
		
		Game g = getStandardGame();

		Player english = g.getPlayer("model.nation.english");
		Player french = g.getPlayer("model.nation.french");

		Market englishMarket = english.getMarket();
		Market frenchMarket = french.getMarket();

		Specification s = spec();

		GoodsType silver = s.getGoodsType("model.goods.Silver");

		int previousGold = english.getGold();

		int price = silver.getInitialSellPrice();

		englishMarket.sell(silver, 1000, english);

		assertEquals(previousGold + price * 1000, english.getGold());

		// Both prices should drop
		assertTrue(englishMarket.getSalePrice(silver, 1) < price);
		assertTrue(frenchMarket.getSalePrice(silver, 1) < price);

		// The french market should drop no more than the english:
		assertTrue(englishMarket.getSalePrice(silver, 1) <= frenchMarket.getSalePrice(silver, 1));

		// After 100 turns the prices should have recovered.
		for (int i = 0; i < 100; i++){
			g.newTurn();

			// The french market should also recover 
			assertTrue(englishMarket.getSalePrice(silver, 1) <= frenchMarket.getSalePrice(silver, 1));
		}
		
		assertTrue(englishMarket.getSalePrice(silver, 1) >= price);
		assertTrue(frenchMarket.getSalePrice(silver, 1) >= price);
	}

	/**
	 * Serialization and deserialization?
	 */
	public void testSerialization() {
		fail();
	}

	/**
	 * Do the transaction listeners work?
	 */
	public void testTransactionListeners() {
		fail("Not yet implemented");
	}
}
