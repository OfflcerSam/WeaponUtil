package offlcersam.weaponfoundry;

/**
 * One item opted into market registration: its full database id, plus the produce/consume flags resolved from its MarketOptions.
 * Produced by WeaponRegistrar/AmmoRegistrar, consumed by MarketRegistrar so it doesn't need to know about WeaponDefinition/AmmoDefinition directly.
 */
public record MarketListing(int databaseId, boolean produce, boolean consume) {
}