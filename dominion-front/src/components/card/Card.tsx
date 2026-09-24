
import React, {useEffect, useState} from 'react';
import { createPortal } from 'react-dom';
import Token from '../player/Token.jsx';
import {useGame} from "../../context/GameContext";
import {getCardImageUrl} from "../../PreLoader";
import {CardData, isFullCard, renderPrice} from "../../interfaces/Card";
import {useModal} from "./useModal";


export interface CardProps {
    data?: CardData;
    name?: string;
    number?: number | string;
    classes?: string[];
    messageType?: string;
    overlay?: boolean;
    style?: React.CSSProperties;
    sendMessage?: (message: string) => void;
    isList?: boolean;
}

interface CardToken {
    key: string;
    color: string;
    pid: number | string;
}




function Card({
                  data,
                  name,
                  number,
                  classes = [],
                  messageType,
                  overlay,
                  style = {},
                  sendMessage,
                  isList = false
              } : CardProps) {

    const {state, gameState} = useGame()
    const nameToUse = data?.name ? data.name : name ? name : ""
    const opponentsList = Object.values(gameState.players.opponents || {});
    const userPlayer = gameState.players.user ? [gameState.players.user] : [];
    const playerList = [...opponentsList, ...userPlayer];

    let tokensOnThisCard : CardToken[] = [];

    if(!isList){
        playerList.forEach(p => {
            const supplyTokens = p.tokens?.supplyTokens || {};
            Object.keys(supplyTokens).forEach(key => {
                if (supplyTokens[key] === nameToUse) {
                    tokensOnThisCard.push({
                        key: key,
                        color: p.color,
                        pid: p.id
                    });
                }
            });
        });
    }

    const send = (message : String) => {
        sendMessage(JSON.stringify({
            gameAction: messageType,
            message: message,
        }));
    }


    const short_name = nameToUse.replace(/[^A-Za-z]/g, '');

    const imageUrl = data?.faceDown
        ? "/src/assets/images/Cardback.png"
        : getCardImageUrl(short_name);


    const combinedStyle = {
        ...(style || {}),
        backgroundImage: imageUrl ? `url("${imageUrl}")` : null
    };

    const {showModal, closeModal, handleContextMenu} = useModal();

    const handleClick = () => {
        if (nameToUse !== "" && messageType && typeof send === 'function') {
            send(`${messageType}:${nameToUse}`);
        }
    };


    return (
        <>
            <div
                className={['card', ...(classes || [])].join(' ')}
                data-name={nameToUse}
                style={combinedStyle}
                onClick={handleClick}
                onContextMenu={handleContextMenu}
            >
                <div className="token-anchor">
                    {tokensOnThisCard.map(t => (
                        <Token
                            key={`card-${t.key}-${t.pid}`}
                            id={`token-${t.key}-${t.pid}`}
                            name={t.key}
                            playerColor={t.color}
                            sendMessage = {sendMessage}
                            classes={["adventure-token", "anchored"]}
                        />
                    ))}
                </div>

                {renderPrice(data)}

                {number ? <div className="number">{number}</div> : null}
                {overlay && <div className="overlay"></div>}
            </div>

            {showModal && createPortal(
                <div className="card-modal-overlay" onClick={closeModal}>
                    <div className="card-modal-content" onClick={(e) => e.stopPropagation()}>
                        <button className="card-modal-close" onClick={closeModal}>
                            &times;
                        </button>

                        <div className="card-modal-body">
                            <h3>{nameToUse}</h3>

                            <img
                                src={imageUrl}
                                alt={nameToUse}
                                className="card-modal-image"
                            />

                            {renderPrice(data, "card-modal-footer")}
                        </div>
                    </div>
                </div>,
                document.body
            )}
        </>
    );
}

export default Card;
