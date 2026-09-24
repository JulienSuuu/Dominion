import React from 'react';
import Token from './Token.jsx';
import {useGame} from "../../context/GameContext.tsx";

// 1. Composant pour le rendu des jetons d'Aventure filtrés
export function AdventureTokensList({ data, id, isOpponent, activePlayerId, state }) {
    const {sendMessage} = useGame()
    const tokenKeys = [
        'card_reduction_token', 'estate_token', 'one_action_token',
        'one_buy_token', 'one_card_token', 'one_money_token', 'trashing_token'
    ];

    return (
        <>
            {tokenKeys
                .filter(key => {
                    const supplyTokens = data.tokens?.supplyTokens || {};
                    const isPlaced = supplyTokens[key];
                    if (isOpponent) {
                        if (state?.players?.active_player === false) return false;
                        return isPlaced;
                    }
                    return !isPlaced;
                })
                .map(key => (
                    <Token
                        key={`tavern-${key}-${data.id}`}
                        id={`token-${key}-${data.id}`}
                        name={key}
                        playerColor={data.color}
                        classes={["adventure-token"]}
                        data={data}
                        sendMessage={sendMessage}
                    />
                ))}
        </>
    );
}

// 2. Composant pour le rendu visuel de la Pioche (Draw Pile)
export function DrawPile({ length, id, isMini }) {
    const maxDisplayedCards = 5;
    const multiplier = isMini ? 1 : 3;

    return (
        <div className="card-pile" id={`draw-pile-${id}`}>
            <div id={`token-minus-card-${id}`} className="token square minus-card" style={{ display: 'none' }}></div>
            <div className="pile-label">Draw: {length}</div>

            <div className={isMini ? "cards-container mini" : "cards-container"}>
                {Array(Math.min(length, maxDisplayedCards))
                    .fill(0)
                    .map((_, i) => (
                        <img
                            key={`draw-${i}`}
                            src="../src/assets/images/Cardback.png"
                            className={`card-back ${!isMini ? 'animate-card' : ''}`}
                            style={{
                                bottom: `${i * multiplier}px`,
                                left: `${i * multiplier}px`,
                                transitionDelay: !isMini ? `${i * 85}ms` : undefined
                            }}
                            alt="Cardback"
                        />
                    ))}
            </div>
        </div>
    );
}

// 3. Composant pour le rendu de la Défausse (Discard Pile)
export function DiscardPile({ length, isMini }) {
    return (
        <div className="card-pile">
            <div className="pile-label">Discard: {length}</div>
            <div className={isMini ? "cards-container mini" : "cards-container"}>
                {length > 0 ? (
                    <img src="../src/assets/images/Cardback.png" className="card-back" alt="Cardback" />
                ) : (
                    <div className="empty-slot"></div>
                )}
            </div>
        </div>
    );
}