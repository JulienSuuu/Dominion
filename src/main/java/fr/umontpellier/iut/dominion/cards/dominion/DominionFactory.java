package fr.umontpellier.iut.dominion.cards.dominion;

import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Destination;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.CardUtil;
import fr.umontpellier.iut.dominion.cards.RegistryPrice;
import fr.umontpellier.iut.dominion.cards.component.OnPlayComponent;

import java.util.Optional;

public class DominionFactory {
    public static Card Artisan(){
        Card artisan = new Card("Artisan", RegistryPrice.DominionPrice(6), CardType.ACTION);
        artisan.addComponent(OnPlayComponent.class, player -> {

            CardUtil.executeIfSelected(
                    () -> CardUtil.gainFromSupply(player, "Choisi une carte coûtant au maximum 5", c -> c.getCost()<6, Destination.HAND, false),
                    card -> player.log(String.format("Action %s : %s gagne %s", artisan.getName().toUpperCase(), player.getName(), card.getName().toUpperCase()))
            );

            CardUtil.executeIfSelected(
                    () ->  player.chooseCardFromHand("Défausse une carte", false),
                    card -> {
                        player.log(String.format("Action %s : %s remet en pioche %s", artisan.getName().toUpperCase(), player.getName(), card.getName().toUpperCase()));
                        player.moveTo(card, Destination.DRAW);
                    }
            );
        });

        return artisan;
    }

    public static Card Bandit(){
        Card bandit = new Card("Bandit", RegistryPrice.DominionPrice(5), CardType.ACTION, CardType.ATTACK);

        bandit.addComponent(OnPlayComponent.class, player ->
            CardUtil.execute(
                    () -> CardUtil.executeIfSelected(
                            () -> CardUtil.gainFromSupply(player, "Gold", Destination.DISCARD, false),
                            card ->player.log(String.format("Action %s : %s gagne %s", bandit.getName().toUpperCase(), player.getName(), card.getName().toUpperCase()))
                    ),

                    () -> player.getGame().processAttackWithReveal(player,
                            bandit,
                            2,
                            card -> card.hasName("Copper"),
                            (attacker, victim, options) -> attacker.getGame().chooseACard(victim, options))
            ));
        return bandit;
    }

    public static Card Bureaucrat(){
        Card br = new Card("Bureaucrat", RegistryPrice.DominionPrice(4), CardType.ACTION, CardType.ATTACK);
        br.addComponent(OnPlayComponent.class, player -> {
            CardUtil.execute(
                    () -> CardUtil.executeIfSelected(
                            () -> CardUtil.gainFromSupply(player, "Silver", Destination.DRAW, false),
                            card -> player.log(String.format("Action %s : %s gagne %s", br.getName().toUpperCase(), player.getName(), card.getName().toUpperCase()))
                    ),
                    () -> player.getGame().checkHandOrShow(
                            player,
                            br,
                            card -> card.hasType(CardType.VICTORY),
                            (vi, cards) -> Optional.ofNullable(vi.chooseCardFromButtons("Choisie une carte victoire à défausser", cards, false)),
                            Destination.DRAW
                    )
            );
        });
        return br;
    }

    public static Card Cellar(){
        Card cellar = new Card("Cellar", RegistryPrice.DominionPrice(2),CardType.ACTION);
        cellar.addComponent(OnPlayComponent.class, player -> {
            CardUtil.TriggerEffect(player, 0, 1, 0, 0, "Effect", cellar);
            int count = 0;
            while(true){
                Card c = player.discard();
                if(c == null) break;
                count++;
            }

            if(count > 0){
                player.draw(count);
                player.log("Action Cellar : " + count + " cartes défaussées, " + count + " cartes piochées.");
            }
        });
        return cellar;
    }


}
