package fr.umontpellier.iut.dominion.cards.dominion;

import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.FactorySupplyPile.PileConfig;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class DominionSet {


    private static final Map<String, PileConfig> PILE_DOMINION = Map.<String, PileConfig>ofEntries(
            Map.entry("Artisan", PileConfig.kingdom(DominionFactory::Artisan)),
            Map.entry("Bandit", PileConfig.kingdom(DominionFactory::Bandit)),
            Map.entry("Bureaucrat", PileConfig.kingdom(DominionFactory::Bureaucrat)),
            Map.entry("Cellar", PileConfig.kingdom(DominionFactory::Cellar)),
            Map.entry("Chapel",PileConfig.kingdom(DominionFactory::Chapel)),
            Map.entry("Council Room", PileConfig.kingdom(DominionFactory::Council_Room)),
            Map.entry("Festival", PileConfig.kingdom(DominionFactory::Festival)),
            Map.entry("Gardens", PileConfig.kingdom(DominionFactory::Gardens)),
            Map.entry("Harbinger", PileConfig.kingdom(DominionFactory::Harbinger)),
            Map.entry("Laboratory", PileConfig.kingdom(DominionFactory::Laboratory)),
            Map.entry("Library", PileConfig.kingdom(DominionFactory::Library)),
            Map.entry("Market", PileConfig.kingdom(DominionFactory::Market)),
            Map.entry("Merchant", PileConfig.kingdom(DominionFactory::Merchant)),
            Map.entry("Militia", PileConfig.kingdom(DominionFactory::Militia)),
            Map.entry("Mine", PileConfig.kingdom(DominionFactory::Mine)),
            Map.entry("Moat", PileConfig.kingdom(DominionFactory::Moat)),
            Map.entry("MoneyLender", PileConfig.kingdom(DominionFactory::MoneyLender)),
            Map.entry("Poacher", PileConfig.kingdom(DominionFactory::Poacher)),
            Map.entry("Remodel", PileConfig.kingdom(DominionFactory::Remodel)),
            Map.entry("Sentry", PileConfig.kingdom(DominionFactory::Sentry)),
            Map.entry("Smithy", PileConfig.kingdom(DominionFactory::Smithy)),
            Map.entry("Throne Room", PileConfig.kingdom(DominionFactory::Throne_Room)),
            Map.entry("Vassal", PileConfig.kingdom(DominionFactory::Vassal)),
            Map.entry("Village", PileConfig.kingdom(DominionFactory::Village)),
            Map.entry("Witch", PileConfig.kingdom(DominionFactory::Witch)),
            Map.entry("Workshop", PileConfig.kingdom(DominionFactory::Workshop)),
            Map.entry("Adventurer", PileConfig.kingdom(DominionFactory::Adventurer)),
            Map.entry("Chancellor", PileConfig.kingdom(DominionFactory::Chancellor)),
            Map.entry("Feast", PileConfig.kingdom(DominionFactory::Feast)),
            Map.entry("Spy", PileConfig.kingdom(DominionFactory::Spy)),
            Map.entry("Thief", PileConfig.kingdom(DominionFactory::Thief)),
            Map.entry("Woodcutter", PileConfig.kingdom(DominionFactory::WoodCutter))

    );
    public static Map<String, PileConfig> get() {
        return PILE_DOMINION;
    }
}
