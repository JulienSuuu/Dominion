import React, { useEffect, useState, useRef } from 'react';
import Card from '../card/Card';
import ListOfCards from '../card/ListOfCards.jsx';
import { updateAdventureTokens } from './utils/TokenUtils.jsx';
import { AdventureTokensList, DrawPile, DiscardPile } from './PlayerSubviews.jsx';
import { AnimatedCounter } from '../animatedComponents/AnimatedCounter.jsx';
import './css/Player.css'
import {useGame} from "../../context/GameContext";
import coinIcon from '../../assets/images/Coin.svg?url';
import potionIcon from '../../assets/images/Potion.svg?url';
import DebtIcon from '../../assets/images/Debt.svg?url';
import {DeltaInfo, PlayerProps, PlayerStats} from "./interface/PlayerData";
import {Button} from "../../interfaces/Choices";
import {ItemChangeEvent} from "../../interfaces/AppState";
import {InventoryModal} from "./Inventory";


function Player({
                    data,
                    game_over,
                    sendMessage,
                }: PlayerProps) {

    const {handleRemoveItemChange, state, gameState, choices} = useGame();

    const { user, turn_player, active_player } = gameState.players;

    const isTurnPlayer = user?.client === turn_player;
    const instruction : string | null = active_player ? choices?.instruction ?? null : null;
    const playerChoice: string[] = active_player ? choices?.choices ?? [] : [];
    const buttons: Button[] | null = active_player ? choices?.buttons ?? null : null;


    const itemChange : ItemChangeEvent[] = state.itemChange

    const classes = ["player", "active", isTurnPlayer ? "turn" : ""].join(" ");
    const sortedHand = [...data.hand].sort();
    const playerTokens  = data.tokens?.playerTokens || {};
    const processedEventsRef = useRef<Set<ItemChangeEvent>>(new Set());

    const [stats, setStats] = useState<PlayerStats>({
        money: { value: data.money || 0, delta: null },
        debt: { value: data.debt || 0, delta: null },
        potion: { value: data.potion || 0, delta: null },
        coffer: { value: data.coffer || 0, delta: null },
        action: { value: data.action || 0, delta: null },
        buy: { value: data.buy || 0, delta: null },
        vp: { value: data.vt || 0, delta: null },
    });

    useEffect(() => {
        if (!itemChange || !Array.isArray(itemChange) || itemChange.length === 0) return;

        itemChange.forEach((change) => {
            if (processedEventsRef.current.has(change) || change.id !== data?.client) {
                return;
            }

            let itemKey = change.item?.toLowerCase();
            if (!itemKey) {
                processedEventsRef.current.add(change);
                if (handleRemoveItemChange) handleRemoveItemChange(change);
                return;
            }

            if (itemKey === 'coins') itemKey = 'money';
            if (itemKey === "victory_token") itemKey = "vp";

            const validKey = itemKey as keyof PlayerStats;

            processedEventsRef.current.add(change);

            setStats((prev: PlayerStats): PlayerStats => {
                if (!prev || prev[validKey] === undefined) return prev;

                return {
                    ...prev,
                    [validKey]: {
                        value: change.new,
                        delta: {
                            delta: change.delta,
                            id: Date.now() + Math.random(),
                            rawInfo: change
                        }
                    }
                };
            });

            if (handleRemoveItemChange) {
                handleRemoveItemChange(change);
            }
        });

    }, [itemChange, data?.client, handleRemoveItemChange]);

    useEffect(() => {
        setStats((prev: PlayerStats): PlayerStats => {
            const nextVt = data.vt ?? prev.vp.value;
            const diff = nextVt - prev.vp.value;

            const vpDelta: DeltaInfo | null = diff !== 0 ? {
                delta: diff,
                id: Date.now() + Math.random(),
                rawInfo: {id: "", item: 'vt', old: prev.vp.value, new: nextVt, delta: diff }
            } : prev.vp.delta;

            return {
                money: { value: data.money ?? prev.money.value, delta: prev.money.delta },
                debt: { value: data.debt ?? prev.debt.value, delta: prev.debt.delta },
                potion: { value: data.potion ?? prev.potion.value, delta: prev.potion.delta },
                coffer: { value: data.coffer ?? prev.coffer.value, delta: prev.coffer.delta },
                action: { value: data.action ?? prev.action.value, delta: prev.action.delta },
                buy: { value: data.buy ?? prev.buy.value, delta: prev.buy.delta },
                vp: {
                    value: nextVt,
                    delta: vpDelta
                }
            };
        });
    }, [data.money, data.debt, data.potion, data.coffer, data.action, data.buy, data.vt]);

    useEffect(() => {
        requestAnimationFrame(() => updateAdventureTokens(data));

        const handleResize = () => updateAdventureTokens(data);
        window.addEventListener('resize', handleResize);

        return () => window.removeEventListener('resize', handleResize);
    }, [data]);

    return (
        <div className="player-main-hud">

            <InventoryModal data={{ id: data.client, tokens: data.tokens, color: data.color }} />

            <div id="interaction-container">
                {choices.mode === false && instruction && (
                    <div className="instruction">
                        <div>{instruction}</div>
                        <div className="buttons">
                            {buttons.map((button, index) => (
                                <button key={index} onClick={() => sendMessage(JSON.stringify({gameAction: "BUTTON", message : `BUTTON:${button.value}`}))}>
                                    {button.label}
                                </button>
                            ))}
                            <div className="buttons">
                                <button
                                    onClick={() => sendMessage(JSON.stringify({gameAction: "PASS", message : ""}))}
                                    disabled={!playerChoice.includes("")}
                                >
                                    Pass
                                </button>
                            </div>
                        </div>
                    </div>
                )}
            </div>

            {/* Zone de la Taverne */}
            {data.tavern != null && (
                <div
                    id="tavern-area"
                    style={{ boxShadow: `inset 0 0 0 5px ${data.color}`, borderBottom: `15px solid ${data.color}` }}
                >
                    <div className="side-label" style={{ color: '#f1d299', fontVariant: 'small-caps', fontWeight: 'bold', marginBottom: '8px', borderBottom: '1px solid #f1d299', width: '100%', textAlign: 'center' }}>
                        Tavern
                    </div>
                    <div className="tavern-cards-container">
                        {data.tavern.map((card, idx) => (
                            <Card
                                key={`tavern-card-${card.name}-${idx}`}
                                data={card}
                                classes={["tavern-card"]}
                                messageType="TAVERN"
                                sendMessage={sendMessage}
                            />
                        ))}
                    </div>
                </div>
            )}

            <div className={classes}>
                <div id={`player-info-${data.id}`} className="player_info">
                    <div className="name">{data.name}</div>

                    {data.tokens != null && (
                        <div className="journey-container">
                            <div
                                id={`token-journey-${data.id}`}
                                className="token round"
                                data-side={playerTokens["JourneyToken"] ? 'sun' : 'moon'}
                            />
                        </div>
                    )}

                    <div className="data stats-container">

                        <div className="stat-group resources">
                            <div className="money-wrapper">
                               <span className="coins" id={`player-money-display-${data.id}`}>
                                   <img src={coinIcon} alt="coins" className="icon" />
                                   <AnimatedCounter value={stats.money.value} deltaInfo={stats.money.delta} />
                               </span>
                                <div id={`token-minus-coin-${data.id}`} className="token square minus-coin" style={{ display: 'none' }}></div>
                            </div>

                            <span className={stats.debt.value > 0 ? 'debt-active' : ''}>
                                <img src={DebtIcon} alt="Debt" className="icon" />
                                <AnimatedCounter value={stats.debt.value} deltaInfo={stats.debt.delta} />
                            </span>
                            <span className="potions">
                                <img src={potionIcon} alt="Coins" className="icon" />
                                <AnimatedCounter value={stats.potion.value} deltaInfo={stats.potion.delta} />
                            </span>
                            <span className="coffers">
                                💰 <AnimatedCounter value={stats.coffer.value} deltaInfo={stats.coffer.delta} />
                            </span>
                            <span title="victory point" className="vp"> VP <AnimatedCounter value={stats.vp.value} deltaInfo={stats.vp.delta} />
                            </span>
                        </div>

                        <div className="stat-group mechanics">
                            <span>Actions: <AnimatedCounter value={stats.action.value} deltaInfo={stats.action.delta} /></span>
                            <span>Buys: <AnimatedCounter value={stats.buy.value} deltaInfo={stats.buy.delta} /></span>
                        </div>

                        <div className="stat-group deck">
                            <DrawPile length={data.draw} id={data.id} isMini={false} />
                            <DiscardPile length={data.discard} isMini={false} />
                        </div>
                    </div>
                </div>

                {!game_over && <ListOfCards classes={["in_play"]} cards={data.in_play} sendMessage={sendMessage} />}

                <ListOfCards
                    classes={!game_over ? "hand" : "hand-basic"}
                    cards={sortedHand}
                    messageType="HAND"
                    sendMessage={sendMessage}
                />
            </div>
        </div>
    );
}

export default Player;