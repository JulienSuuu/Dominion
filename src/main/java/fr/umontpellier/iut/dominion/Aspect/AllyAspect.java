package fr.umontpellier.iut.dominion.Aspect;

import fr.umontpellier.iut.dominion.Player.Player;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.factories.Ally.AllyLogic;
import fr.umontpellier.iut.dominion.cards.factories.Ally.AllyLogicRegistry;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;


@Aspect
@Component
public class AllyAspect {
    private final AllyLogicRegistry registry;
    private static final ThreadLocal<Boolean> isProcessing = ThreadLocal.withInitial(() -> false);

    public AllyAspect(AllyLogicRegistry registry) {
        this.registry = registry;
    }

    // --- TRIGGER APRES ---
    @AfterReturning("@annotation(fr.umontpellier.iut.dominion.Annotation.AfterAllyTrigger)")
    public void afterTrigger(JoinPoint joinPoint) {
        process(joinPoint, "on");
    }

    // --- TRIGGER AVANT ---
    @Before("@annotation(fr.umontpellier.iut.dominion.Annotation.BeforeAllyTrigger)")
    public void beforeTrigger(JoinPoint joinPoint) {
        process(joinPoint, "before");
    }

    private void process(JoinPoint joinPoint, String prefix) {
        if (isProcessing.get()) return;

        Player player = (Player) joinPoint.getTarget();
        Card activeAlly = player.getGame().getCurrentAlly();
        if (activeAlly == null) return;

        AllyLogic logic = registry.getLogicFor(activeAlly.getName());
        if (logic != null) {
            try {
                isProcessing.set(true);
                String hookName = prefix + capitalize(joinPoint.getSignature().getName());
                logic.executeHook(hookName, player, joinPoint.getArgs());
            } finally {
                isProcessing.set(false);
            }
        }
    }

    private String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}