package fr.umontpellier.iut.dominion.cards.seaside;

import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Destination;
import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Card;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Carte aux trésors (Treasure Map)
 * <p>
 * Écartez ceci et une Carte aux trésors de votre main. Si vous avez écarté
 * deux Cartes aux trésors, recevez 4 Ors (Gold) sur votre pioche.
 */
public class TreasureMap extends Card {
    public TreasureMap() {
        super("Treasure Map", 4, CardType.ACTION);
    }

    @Override
    public void play(Player p) {
        p.getGame().moveToTrash(this);

        Card other = p.getCardsInHand().stream().filter(this::hasSameNameAs).findFirst().orElse(null);
        if (other != null) {
            p.getGame().moveToTrash(other);

            IntStream.range(0,4)
                    .mapToObj(c -> p.getCardFromSupply("Gold"))
                    .filter(Objects::nonNull)
                    .forEach(gold -> p.gain(gold, Destination.DRAW));
        }
    }
}
