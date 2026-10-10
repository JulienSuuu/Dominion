package fr.umontpellier.iut.dominion.infrastructure.aop


import fr.umontpellier.iut.dominion.application.service.UiStateService
import fr.umontpellier.iut.dominion.domain.model.player.Player
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.stereotype.Component
import kotlin.coroutines.Continuation

@Aspect
@Component
class OverlayAspect(
    private val uiStateService: UiStateService
) {

    @Around("@annotation(fr.umontpellier.iut.dominion.domain.annotation.Selection_Mode)")
    fun handleSelectionOverlay(joinPoint: ProceedingJoinPoint): Any? {
        val player = (joinPoint.target as? Player) ?: (joinPoint.`this` as? Player)
        ?: return joinPoint.proceed()

        val gameId = player.game.instance.gameId
        uiStateService.setPromptActive(gameId, true)

        val args = joinPoint.args.clone()
        val lastArg = args.lastOrNull()

        if (lastArg is Continuation<*>) {
            @Suppress("UNCHECKED_CAST")
            val originalContinuation = lastArg as Continuation<Any?>

            val wrappedContinuation = Continuation(originalContinuation.context) { result ->
                try {
                    originalContinuation.resumeWith(result)
                } finally {
                    uiStateService.setPromptActive(gameId, false)
                }
            }

            args[args.size - 1] = wrappedContinuation
            return joinPoint.proceed(args)
        }

        return try {
            joinPoint.proceed()
        } finally {
            uiStateService.setPromptActive(gameId, false)
        }
    }
}