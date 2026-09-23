import React, {useState, useEffect, JSX} from 'react';
import Card from '../card/Card';
import './css/AnimatedShopCard.css';
import {useGame} from "../../context/GameContext";
import {CardData} from "../../interfaces/Card";

interface AShopProps {
    cardData : CardData;
    location? : string;
    name : string;
    number? : number;
    classes?: (string | undefined)[];
    messageType?: string;
    overlay?: boolean;
    sendMessage?: (message: string) => void;
}


function AnimatedShopCard({cardData, location, ...props } : AShopProps): JSX.Element  {
    const {state, gameState, handleRemoveEvent} = useGame()
    const {players} = gameState;
    const {user} = players;
    const [animationClass, setAnimationClass] = useState('');
    const currentClientId = user?.client;
    const changeEvents = state.onCardChange
    useEffect(() => {
        if (!changeEvents || !Array.isArray(changeEvents) || changeEvents.length === 0) return;

        const lastChange = changeEvents[changeEvents.length - 1];

        const isSameCard = lastChange && lastChange.supplyName === props.name;
        const isSameLocation = !lastChange.location || lastChange.location === location;

        if (isSameCard && isSameLocation) {
            const animType = lastChange.type === 'CARD_ADD' ? 'anim-card-add' : 'anim-card-remove';
            setAnimationClass(animType);

            const timer = setTimeout(() => {
                setAnimationClass('');

                if (handleRemoveEvent) {
                    handleRemoveEvent(lastChange);
                }
            }, 600);

            return () => clearTimeout(timer);
        }
    }, [changeEvents]);

    const combinedClasses = [...(props.classes || []), animationClass].filter(Boolean);

    return (
        <div className={`shop-card-wrapper ${animationClass}`}>
            <Card
                {...props}
                data={cardData}
                classes={combinedClasses}
            />
        </div>
    );
}

export default AnimatedShopCard;