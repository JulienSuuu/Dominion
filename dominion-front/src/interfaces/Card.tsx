import React from "react";

export interface BaseCard {
    id: string;
    name: string;
    faceDown: boolean;
}

export interface CardPrice {
    cost: number;
    debt?: number;
    potion?: number;
}

export interface CardSummary extends BaseCard {
    type: "SUMMARY";
}

export interface CardShadowZone extends BaseCard {
    type: "SHADOW";
}



export interface Card extends BaseCard {
    type: "FULL";
    price: CardPrice;
    types: string[];
    description: string;
    location: string | null;
}

export type CardData = Card | CardSummary | CardShadowZone;

export const isFullCard = (card ?: CardData): card is Card => {
    return card?.type === "FULL";
};

export function renderPrice(card?: CardData, className: string = "card-footer"): React.ReactNode {
    if (!card || !isFullCard(card) || !card.price) return null;

    const { cost, potion, debt } = card.price;

    const hasCost = typeof cost === 'number' && cost > 0;
    const hasPotion = typeof potion === 'number' && potion > 0;
    const hasDebt = typeof debt === 'number' && debt > 0;

    if (!hasCost && !hasPotion && !hasDebt) return null;

    return (
        <div className={className}>
            {hasCost && <div className="cost">{cost}</div>}
            {hasPotion && <div className="potion">{potion}</div>}
            {hasDebt && <div className="debt">{debt}</div>}
        </div>
    );
}