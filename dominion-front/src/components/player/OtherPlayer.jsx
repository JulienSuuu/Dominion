import React, { useEffect, useState, useRef } from 'react';
import Card from '../card/Card.tsx';
import { AdventureTokensList, DrawPile, DiscardPile } from './PlayerSubviews.jsx';
import { AnimatedCounter } from '../animatedComponents/AnimatedCounter.jsx';
import { createPortal } from 'react-dom';
import './css/OtherPlayer.css';
import OpponentActionBanner from "../animatedComponents/OpponentActionBanner.jsx";
import {useGame} from "../../context/GameContext.tsx";
import ListOfCards from "../card/ListOfCards.jsx";
import coinIcon from '../../assets/images/Coin.svg?url';


function OtherPlayer({ data, is_turn_player, state, updateAdventureTokens, itemChange = [], handleCardChange }) {
    const classes = ["player", "opponent-hud", is_turn_player ? "turn" : ""].join(" ");
    const {handleRemoveItemChange, gameState} = useGame();
    const [showDetailsModal, setShowDetailsModal] = useState(false);
    const lastProcessedIndexRef = useRef(0);

    const [stats, setStats] = useState({
        money: { value: data.money || 0, delta: null },
        action: { value: data.actions || 0, delta: null },
        buy: { value: data.buys || 0, delta: null },
    });

    useEffect(() => {
        if (!itemChange || !Array.isArray(itemChange) || itemChange.length === 0) return;

        for (let i = lastProcessedIndexRef.current; i < itemChange.length; i++) {
            const change = itemChange[i];
            if (!change) continue;

            if (change.id === data?.playerId) {
                let itemKey = change.item?.toLowerCase();
                if (!itemKey) {
                    if (handleRemoveItemChange) handleRemoveItemChange(change);
                    continue;
                }

                if (!stats || stats[itemKey] === undefined) {
                    if (handleRemoveItemChange) {
                        handleRemoveItemChange(change);
                    }
                    continue;
                }

                if (itemKey === 'coins') itemKey = 'money';

                setStats(prev => {
                    if (!prev || prev[itemKey] === undefined) return prev;

                    return {
                        ...prev,
                        [itemKey]: {
                            value: change.new,
                            delta: {
                                delta: change.delta,
                                id: Date.now() + Math.random(),
                                rawInfo: change,
                            }
                        }
                    };
                });
            }
        }

        lastProcessedIndexRef.current = itemChange.length;
    }, [itemChange, data?.client]);

    useEffect(() => {
        setStats(prev => ({
            money: { value: data.money ?? prev.money.value, delta: prev.money.delta },
            action: { value: data.action ?? prev.action.value, delta: prev.action.delta },
            buy: { value: data.buy ?? prev.buy.value, delta: prev.buy.delta },
        }));
    }, [data.money, data.actions, data.buys]);

    useEffect(() => {
        if (typeof updateAdventureTokens === 'function') {
            requestAnimationFrame(() => updateAdventureTokens(data));
        }
    }, [data, updateAdventureTokens]);

    useEffect(() => {
        if (!showDetailsModal) return;

        const handleEscape = (e) => {
            if (e.key === 'Escape') {
                e.preventDefault();
                e.stopPropagation();
                e.stopImmediatePropagation();

                if (e.type === 'keydown') {
                    setShowDetailsModal(false);
                }
            }
        };

        window.addEventListener('keydown', handleEscape, true);
        window.addEventListener('keyup', handleEscape, true);

        return () => {
            window.removeEventListener('keydown', handleEscape, true);
            window.removeEventListener('keyup', handleEscape, true);
        };
    }, [showDetailsModal]);

    const handleContextMenu = (e) => {
        e.preventDefault();
        setShowDetailsModal(true);
    };

    return (
        <>
            <div className="opponent-wrapper" onContextMenu={handleContextMenu}>
                {/* Jetons Aventure */}
                <AdventureTokensList
                    data={data}
                    isOpponent={true}
                    activePlayerId={gameState?.active_player}
                    state={gameState}
                />

                <div className={classes} id={`player-info-${data.id}`}>
                    <div className="hud-header">
                        <div className="data stats-container mini">
                            <div className="name">{data.name}</div>

                            <div className="stat-group resources mini-stats">
                                <span className="coins mini" title="Money">
                                    <img src={coinIcon} alt="Coins" className="icon mini" />
                                    <AnimatedCounter value={stats.money.value} deltaInfo={stats.money.delta} mini={true} />
                                </span>
                                <span title="Actions">
                                    Actions: <AnimatedCounter value={stats.action.value} deltaInfo={stats.action.delta} mini={true} />
                                </span>
                                <span title="Buys">
                                    Buys: <AnimatedCounter value={stats.buy.value} deltaInfo={stats.buy.delta} mini={true} />
                                </span>
                            </div>
                        </div>
                        <div className="stat-group deck mini-deck">
                            <DrawPile length={data.draw} id={data.id} isMini={true} />
                            <DiscardPile length={data.discard} isMini={true} />
                        </div>
                    </div>

                    <div className="opponent-deck">
                        <div className="opponent-inplay-preview">
                            <ListOfCards
                                cards={data.in_play}
                                classes={["inplay-mini"]}
                            />
                        </div>
                    </div>
                </div>

                {/*PLACE DU BANNER D'ACTION */}
                <OpponentActionBanner
                    playerId={data?.playerId}
                    opponentId={data?.id}
                />
            </div>

            {/* Modal Clic Droit */}
            {showDetailsModal && createPortal(
                <div className="card-modal-overlay" onClick={() => setShowDetailsModal(false)}>
                    <div className="card-modal-content opponent-modal" onClick={(e) => e.stopPropagation()}>
                        <button className="card-modal-close" onClick={() => setShowDetailsModal(false)}>
                            &times;
                        </button>

                        <h2>Informations : {data.name}</h2>

                        <div className="opponent-modal-stats">
                            <div className="stat-box">Coins : <strong>{stats.money.value}</strong></div>
                            <div className="stat-box">Actions : <strong>{stats.action.value}</strong></div>
                            <div className="stat-box">Buys : <strong>{stats.buy.value}</strong></div>
                            <div className="stat-box">Coffer : <strong>{data.coffer ?? 0}</strong></div>
                            <div className="stat-box">Debt : <strong>{data.debt ?? 0}</strong></div>
                            <div className="stat-box">Potions : <strong>{data.potion ?? 0}</strong></div>
                            <div className="stat-box">Victory Point: <strong>{data.vt}</strong></div>
                            <div className="stat-box">Hand : <strong>{data.hand ? data.hand : (data.handSize || 0)} cartes</strong></div>
                        </div>

                        {data.tavern && data.tavern.length > 0 && (
                            <div className="opponent-modal-section">
                                <h3>Cartes en Taverne ({data.tavern.length})</h3>
                                <div className="modal-cards-grid">
                                    {data.tavern.map((card, idx) => (
                                        <Card key={`modal-tavern-${card}-${idx}`} name={card} classes={["mini-card"]} messageType="None" />
                                    ))}
                                </div>
                            </div>
                        )}

                        {data.in_play && data.in_play.length > 0 && (
                            <div className="opponent-modal-section">
                                <h3>Cartes en Jeu ({data.in_play.length})</h3>
                                <ListOfCards
                                    cards={data.in_play}
                                    classes={["modal-cards-grid"]}


                                />
                            </div>
                        )}
                    </div>
                </div>, document.body
            )}
        </>
    );
}

function getCardName(card) {
    return (typeof card === 'object' ? card?.name : card);
}


export default OtherPlayer;