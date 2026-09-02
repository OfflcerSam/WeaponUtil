package offlcersam.weaponfoundry;

import com.sector.bridge.SSFMLLogger;
import game.markets.Market;
import game.markets.MarketDatabase;
import game.markets.MarketItem;
import illuminatus.core.datastructures.List;
import items.Item;
import java.lang.reflect.Field;

/*
 * Registers weapons and ammo that opted into market listings through their JSON data.
 */
public final class MarketRegistrar {

    private static boolean registered;

    private MarketRegistrar() {
    }

    public static void registerMarkets() {
        if (registered) {
            return;
        }
        registered = true;

        int[] weapons = WeaponRegistrar.getMarketWeaponDatabaseIDs();
        int[] ammo = AmmoRegistrar.getMarketAmmoDatabaseIDs();

        if (weapons.length == 0 && ammo.length == 0) {
            SSFMLLogger.log("[WeaponFoundry] No custom weapons or ammo opted into market registration");
            return;
        }

        int updatedMarkets = 0;
        int addedItems = 0;

        List<Market> markets = getMarkets();

        if (markets != null) {
            for (int marketIndex = 0; marketIndex < markets.size(); marketIndex++) {
                Market market = markets.getChecked(marketIndex);

                if (market == null) {
                    continue;
                }

                // Check MarketList for addStationIndices.
                if (market.stationMatches(502) || market.stationMatches(512)) {
                    addedItems += addListings(market, weapons);
                    addedItems += addListings(market, ammo);

                    MarketDatabase.setMarket(marketIndex, market);
                    updatedMarkets++;
                }
            }
        }

        SSFMLLogger.log(
                "[WeaponFoundry] Added "
                        + addedItems
                        + " custom weapon/ammo listings to "
                        + updatedMarkets
                        + " markets"
        );
    }

    private static int addListings(Market market, int[] itemDatabaseIds) {
        int added = 0;

        for (int itemID : itemDatabaseIds) {
            MarketItem listing = new MarketItem(itemID, MarketItem.BUY_AND_SELL_ALWAYS);

            if (listing.item != null) {
                Item.markAsMarketItem(listing.item);
            }

            market.addChecked(listing);
            added++;
        }

        return added;
    }

    @SuppressWarnings("unchecked")
    private static List<Market> getMarkets() {
        try {
            Field field = MarketDatabase.class.getDeclaredField("markets");
            field.setAccessible(true);
            return (List<Market>) field.get(null);
        } catch (ReflectiveOperationException exception) {
            SSFMLLogger.log("[WeaponFoundry] Could not access MarketDatabase markets: " + exception);
            return null;
        }
    }
}