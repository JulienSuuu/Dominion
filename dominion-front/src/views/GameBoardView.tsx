import React, {useEffect, useState} from "react";
import Log from "../components/Logs.jsx";
import Player from "../components/player/Player";
import OtherPlayer from "../components/player/OtherPlayer.jsx";
import Shop from "../components/Shop";
import SelectionOverlay from "./SelectionOverlay.jsx";
import {useGame} from "../context/GameContext";

function GameBoardView() {
    const [isShopOpen, setIsShopOpen] = useState<boolean>(false);
    const {state, gameState, choices, sendMessage, socket, handleRemoveEvent } = useGame();

    useEffect(() => {
        const handleKeyDown = (event : KeyboardEvent) => {
            const isInput = ['INPUT', 'TEXTAREA'].includes(document.activeElement.tagName);
            if (isInput) return;

            if (event.key === 'b' || event.key === 'B') {
                event.preventDefault();
                setIsShopOpen((prev) => !prev);
            }
        };

        window.addEventListener('keydown', handleKeyDown);

        return () => {
            window.removeEventListener('keydown', handleKeyDown);
        };
    }, []);

    return (
        <div id="main">
            {/* Overlay de sélection si une action est requise */}
            {choices.selection_cards && gameState.players.active_player && (
                <SelectionOverlay
                    sendMessage={sendMessage}
                    onSelect={(choice : string) => sendMessage(JSON.stringify({
                            gameState: "SELECTION_CARD",
                            message: choice
                    }))}
                />
            )}

            {/* Bouton boutique accroché en haut au centre */}
            <button
                className={`open-shop-btn ${gameState.players.active_player ? 'glow' : ''}`}
                onClick={() => setIsShopOpen(!isShopOpen)}
            >
                {isShopOpen ? "Close Shop" : "Open Shop"}
            </button>

            {/* Volet coulissant de la boutique */}
            <Shop
                isOpen={isShopOpen}
                onClose={() => setIsShopOpen(false)}
            />

            <div id="game">
                <div id="players">
                    {/* Conteneur des autres joueurs (en haut, face au joueur) */}
                    <div className="opponents-row">
                        {gameState.players.opponents && gameState.players.opponents.map((player_data, index) => {
                            const is_turn_player = player_data.playerId === gameState.players.turn_player;
                            return (
                                <OtherPlayer
                                    key={player_data.playerId || player_data.name || index}
                                    data={player_data}
                                    is_turn_player={is_turn_player}
                                    state={state}
                                    itemChange={state.itemChange}
                                    handleCardChange={handleRemoveEvent}
                                />
                            );
                        })}
                    </div>

                    {gameState.players.user && (
                        <Player
                            key={gameState.players.user.client ?? "user_player"}
                            data={gameState.players.user}
                            game_over={state.view === "GAME_OVER"}
                            sendMessage={sendMessage}
                        />
                    )}
                </div>
            </div>

            {/* Journal de bord & zoom */}
            <Log log={state.logChange} socket={socket} sendMessage={sendMessage} />
        </div>
    );
}

export default GameBoardView;