
export function updateAdventureTokens(playerData) {
    if (!playerData || !playerData.tokens) return;

    const pid = playerData.id;
    const playerColor = playerData.color;
    const pTokens = playerData.tokens.playerTokens;

    const taxCard = document.getElementById(`token-minus-card-${pid}`);
    if (taxCard) {
        if (pTokens["MinusOneCardToken"] || pTokens["Tax Card Token"]) {
            const anchor = document.getElementById(`draw-pile-${pid}`);
            moveTokenTo(taxCard, anchor, playerColor);
        } else {
            taxCard.style.display = "none";
        }
    }

    const taxCoin = document.getElementById(`token-minus-coin-${pid}`);
    if (taxCoin) {
        if (pTokens["MinusOneCoinToken"] || pTokens["Tax token"]) {
            const anchor = document.getElementById(`player-money-display-${pid}`);
            moveTokenTo(taxCoin, anchor, playerColor);
        } else {
            taxCoin.style.display = "none";
        }
    }

    const journeyEl = document.getElementById(`token-journey-${pid}`);
    if (journeyEl && pTokens["JourneyToken"] !== undefined) {
        journeyEl.dataset.side = pTokens["JourneyToken"] ? 'sun' : 'moon';
        journeyEl.style.borderColor = playerColor;
        journeyEl.style.display = "block";
    }
}

function moveTokenTo(el, target, color) {
    if (!el || !target) return;

    el.style.position = "fixed";
    el.style.zIndex = "10000";
    el.style.display = "block";

    const targetRect = target.getBoundingClientRect();

    const x = targetRect.left + (targetRect.width / 2) - 12;
    const y = targetRect.top + (targetRect.height / 2) - 12;

    el.style.left = `${x}px`;
    el.style.top = `${y}px`;
    el.style.borderColor = color;
}