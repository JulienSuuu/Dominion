package fr.umontpellier.iut.dominion.gui;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class OverlayAspect {

    private final UiStateService uiStateService;

    public OverlayAspect(UiStateService uiStateService) {
        this.uiStateService = uiStateService;
    }

    @Around("@annotation(fr.umontpellier.iut.dominion.Annotation.Selection_Mode)")
    public Object handleSelectionOverlay(ProceedingJoinPoint joinPoint) throws Throwable {
        uiStateService.setPromptActive(true);
        try {
            return joinPoint.proceed();
        } finally {
            uiStateService.setPromptActive(false);
        }
    }
}
