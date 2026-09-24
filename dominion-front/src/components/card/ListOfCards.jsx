import React from 'react';
import Card from './Card.tsx';

function ListOfCards({ cards, classes, messageType, sendMessage}) {
    const validCards = Array.isArray(cards) ? cards.filter(c => c !== null) : [];
    const classList = Array.isArray(classes) ? classes : [classes || ""];

    if (validCards.length === 0) {
        return <div className={classList.join(" ")} />;
    }

    const getCardName = (card) => (typeof card === 'object' ? card?.name : card);

    const total = validCards.length;
    const isHand = classList.includes("hand") || classList.includes("hand-basic");

    const MAX_FAN_CARDS = 8;
    const shouldGroup = isHand && total > MAX_FAN_CARDS;

    // --- MODE 1 : RENDU GROUPÉ ---
    if (shouldGroup) {
        // Regroupement par nom et conservation d'une carte de référence
        const grouped = validCards.reduce((acc, card) => {
            const name = getCardName(card);
            if (!acc[name]) {
                acc[name] = { cardData: card, count: 0 };
            }
            acc[name].count += 1;
            return acc;
        }, {});

        return (
            <div className="hand-grouped">
                {Object.entries(grouped).map(([cardName, { cardData, count }], i) => (
                    <div key={`pile-${cardName}-${i}`} className="card-pile-container">
                        <Card
                            key={cardData.id || `card-${cardName}-${i}`}
                            data={cardData}
                            classes={["card-pile"]}
                            messageType={messageType}
                            sendMessage={sendMessage}
                            isList={true}
                        />
                        {count > 1 && (
                            <span className="card-count-badge">x{count}</span>
                        )}
                    </div>
                ))}
            </div>
        );
    }

    // --- MODE 2 : RENDU NORMAL (ÉVENTAIL ET INPLAY) ---
    return (
        <div
            className={classList.join(" ")}
            style={{ '--total': total }}
        >
            {validCards.map((card, i) => {
                const currentName = getCardName(card);
                const nextName = getCardName(validCards[i + 1]);

                // Vérifie si la carte suivante a le même nom
                const isDuplicate = Boolean(nextName && currentName === nextName);
                const cardStyle = isHand ? { '--index': i } : {};

                return (
                    <Card
                        key={card.id || `card-${i}`}
                        data={card}
                        classes={isDuplicate ? ['duplicate'] : []}
                        messageType={messageType}
                        style={cardStyle}
                        sendMessage={sendMessage}
                        isList={true}
                    />
                );
            })}
        </div>
    );
}

export default ListOfCards;