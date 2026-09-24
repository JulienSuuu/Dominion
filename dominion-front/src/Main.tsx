import React, {useEffect, useState} from 'react';
import './App.css';
import './Game.css';
import './components/player/css/Player.css';

import { GameProvider } from './context/GameContext';
import PauseMenu from './views/PauseMenu.jsx';
import AuthForm from './views/AuthForm';

import MainMenuView from './views/MainMenuView.jsx';
import ExtraCardSelectionView from './views/ExtraCardSelectionView.jsx';
import LobbySetupView from './views/LobbySetupView.jsx';
import GameBoardView from './views/GameBoardView';
import {GameEnd} from "./views/GameEnd.jsx";

import {useSocket} from "./UseSocket";
import {User} from "./interfaces/User";
import {AppState, ItemChangeEvent, OnCardChangeEvent} from "./interfaces/AppState";
import {GameState} from "./interfaces/GameState";
import {Choices} from "./interfaces/Choices";
import {LobbyState} from "./interfaces/LobbyState";
import {Theme} from "./interfaces/Theme";


export const initialUser: User = {
    id: "",
    pseudo: "",
    isConnected: false,
    isRegistering: false,
    currentTheme: null
};

export const initialAppState: AppState = {
    gameId: null,
    view: null,
    itemChange: [],
    onCardChange: [],
    logChange: [],
    ranking: []
};



export default function Main() {
    const [roomCodeInput, setRoomCodeInput] = useState("");

    const [user, setUser] = useState<User>(initialUser);
    const [state, setState] = useState<AppState>(initialAppState);

    const [gameState, setGameState] = useState<GameState>({ gameId: null, players: { opponents: [], turn_player: null, active_player: null, user: null }, shopSupply: { supply: null, aside: {}, events: null }, infos: { size: null }, nocturne: null, empires: null });
    const [choices, setChoices] = useState<Choices>({ instruction: null, choices: null, buttons: null, selection_cards: null, mode: null });

    const[lobbyState, setLobbyState] = useState<LobbyState>({
        gameId: null,
        availableCards: undefined,
        availableEvents: undefined,
        selectedCards: undefined,
        selectedEvents: undefined,
        users: [],
        presets: undefined,
        options: undefined,
    });

    const { socket, sendMessage, connectToServer, resetGame} = useSocket(user, state,  setUser, setGameState, setChoices, setState, setLobbyState);

    const handleLeavingGame = async (gameId: string, playerId: string | number): Promise<void> => {
        try {
            await fetch(`http://localhost:3232/api/game/leaving/${gameId}?playerId=${playerId}`, {
                method: 'POST',
                credentials: 'include'
            });
        } catch (error) {
            console.error("Erreur lors du départ de la partie :", error);
        }
    };

    const handleLogout = async () => {
        try {
            await fetch('http://localhost:3232/api/auth/logout', { method: 'POST', credentials: 'include' });
            setUser(prev => ({ ...prev, isConnected: false, currentTheme: null }));
            if (socket) socket.close();
        } catch (err) {
            console.error("Erreur de déconnexion", err);
        }
    };

    const setThemeUser = (theme : Theme) => {
        setUser(prevState => {
            return { ...prevState, currentTheme : theme ?? prevState.currentTheme };
        })
    }
    const themeCode = user?.currentTheme?.code || "dominion";

    useEffect(() => {
        if (themeCode) {
            document.documentElement.setAttribute('data-theme', themeCode);
        }
    }, [themeCode]);

    if (!user.isConnected) {
        return <AuthForm user={user} setUser={setUser} connectToServer={connectToServer} />;
    }

    return (
        <>
            <PauseMenu user={user} onLogout={handleLogout} />

            {state.view === "MAIN_MENU" && (
                <MainMenuView user={user} roomCodeInput={roomCodeInput} setRoomCodeInput={setRoomCodeInput} setThemeUser={setThemeUser} sendMessage={sendMessage} />
            )}

            {(state.view === "FERRYMAN_CHOICE" || state.view === "YOUNG_WITCH_CHOICE") && (
                <ExtraCardSelectionView generalState={state} state={lobbyState} sendMessage={sendMessage} />
            )}

            {state.view === "LOBBY" && (
                <LobbySetupView state={lobbyState} sendMessage={sendMessage} />
            )}

            {state.view === "GAME" && (
                <GameProvider value={{
                    state,
                    gameState,
                    choices,
                    sendMessage: gameState.players.active_player ? sendMessage : (() => {}),
                    socket,
                    handleRemoveEvent: (e : OnCardChangeEvent) => setState(p => ({ ...p, onCardChange: p.onCardChange.filter(item => item !== e) })),
                    handleRemoveItemChange: (e : ItemChangeEvent) => setState(p => ({ ...p, itemChange: p.itemChange.filter(item => item !== e) })),
                }}>
                    <GameBoardView />
                </GameProvider>
            )}

            {state.view === "GAME_OVER" &&
                <GameProvider value={{
                    user,
                    state,
                    resetGame: resetGame,
                    sendMessage: sendMessage,
                    handleLeavingGame: handleLeavingGame
                }}>
                    <GameEnd />
                </GameProvider>
            }
        </>
    );
}
