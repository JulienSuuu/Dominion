import React, { useState, useEffect, useRef } from 'react';
import { preloadAllCards } from './PreLoader';
import {Choices} from "./interfaces/Choices";
import {AppState} from "./interfaces/AppState";
import {GameState} from "./interfaces/GameState";
import {LobbyState} from "./interfaces/LobbyState";
import {User} from "./interfaces/User";
import {AuthUserData, ServerResponse} from "./interfaces/ServerResponse";

const INITIAL_GAME_STATE : GameState = {
    gameId: null,
    players: { active_player: false, turn_player: null, opponents: [], user: null },
    shopSupply: { supply: [], aside: {}, events: [] },
    nocturne: null,
    infos: { size: 0 }
};

const INITIAL_CHOICES : Choices = {
    instruction: null,
    choices: [],
    buttons: [],
    selection_cards: [],
    mode: null
};

const INITIAL_STATE : AppState ={
    view: "MAIN_MENU",
    gameId: null,
    itemChange: [],
    onCardChange: [],
    logChange: [],
    ranking: []
};

const INITIAL_LOBBY : LobbyState = {
    gameId: null,
    users: undefined,
    availableCards: undefined,
    availableEvents: undefined,
    selectedCards: undefined,
    selectedEvents: undefined,
    presets: undefined,
    options: undefined,
}

export function useSocket(
    user: User,
    state: AppState,
    setUser: React.Dispatch<React.SetStateAction<User>>,
    setGameState: React.Dispatch<React.SetStateAction<GameState>>,
    setChoices: React.Dispatch<React.SetStateAction<Choices>>,
    setState: React.Dispatch<React.SetStateAction<AppState>>,
    setLobbyState: React.Dispatch<React.SetStateAction<LobbyState>>
) {
    const [socket, setSocket] = useState<WebSocket | null>(null);
    const isConnectingRef = useRef<boolean>(false);
    const currentViewRef = useRef<string>(state.view);


    useEffect(() => {
        currentViewRef.current = state.view;
    }, [state.view]);

    const connectToServer = (data? : AuthUserData) => {
        if (data) {
            setUser(prev => ({
                ...prev,
                id: data.id ?? prev.id,
                pseudo: data.pseudo ?? prev.pseudo,
                isConnected: data.isConnected ?? prev.isConnected ?? false,
                currentTheme: data.theme ?? prev.currentTheme,
            }));
        }

        const ws = new WebSocket(`ws://localhost:3232/ws`);

        ws.onopen = () => {
            console.log(`WebSocket connecté pour ${data.pseudo}`);
            setUser(prev => ({ ...prev, isConnected: true }));
            setSocket(ws);
        };

        ws.onmessage = (event : MessageEvent<string>) => {
            const responseData: ServerResponse = JSON.parse(event.data);
            console.log("---MESSAGE RECU--- ", responseData);
            const hasGame = responseData.game !== null && responseData.game !== undefined;

            const currentView = responseData.view || currentViewRef.current;

            const isActive = responseData.active_player ?? false;

            if (hasGame && currentView !== "GAME_OVER") {
                setGameState(prevGame => ({
                    ...prevGame,
                    gameId: responseData.gameId ?? prevGame.gameId,
                    players: {
                        ...prevGame.players,
                        active_player: responseData.active_player ?? prevGame.players.active_player,
                        turn_player: responseData.game.turn_player ?? prevGame.players.turn_player,
                        opponents: responseData.game.players ?? prevGame.players.opponents,
                        user: responseData.game.client ?? prevGame.players.user
                    },
                    shopSupply: {
                        ...prevGame.shopSupply,
                        supply: responseData.game.supply ?? prevGame.shopSupply.supply,
                        aside: responseData.game.aside ?? prevGame.shopSupply.aside,
                        events: responseData.game.events ?? prevGame.shopSupply.events
                    },
                    nocturne: responseData.game.nocturne ?? prevGame.nocturne,
                    empires: responseData.game.empires ?? prevGame.empires,
                    infos: {
                        ...prevGame.infos,
                        size: responseData.game.size ?? prevGame.infos.size,
                    }
                }));
            }

            if(isActive && currentView !== "GAME_OVER"){
            setChoices(prevState => ({
                ...prevState,
                instruction: responseData.instruction ?? prevState.instruction,
                choices: responseData.choices ?? prevState.choices,
                buttons: responseData.buttons ?? prevState.buttons,
                selection_cards: responseData.selection_cards ?? prevState.selection_cards,
                mode: responseData.mode ?? prevState.mode
            }));}



            if(currentView === "LOBBY" || currentView === "FERRYMAN_CHOICE" || currentView === "YOUNG_WITCH_CHOICE"){
                setLobbyState(prevState => ({
                    ...prevState,
                    gameId: responseData.gameId ?? prevState.gameId,
                    users: responseData.players ?? prevState.users,
                    availableCards: responseData.availableCards ?? prevState.availableCards,
                    availableEvents: responseData.availableEvents ?? prevState.availableEvents,
                    selectedCards: responseData.selectedCards ?? prevState.selectedCards,
                    selectedEvents: responseData.selectedEvents ?? prevState.selectedEvents,
                    presets: responseData.presets ?? prevState.presets,
                    options: responseData.options ?? prevState.options
                }));
            }

            setState(prevState => {
                const viewToSet = responseData.view ?? prevState.view;

                return {
                    ...prevState,
                    view: viewToSet,
                    gameId: responseData.gameId ?? (viewToSet === "MAIN_MENU" ? null : prevState.gameId),
                    itemChange: viewToSet === "MAIN_MENU" ? [] : (responseData.itemChange ? [...(prevState.itemChange || []), responseData.itemChange] : (prevState.itemChange || [])),
                    onCardChange: viewToSet === "MAIN_MENU" ? [] : responseData.onCardChange ? [...(prevState.onCardChange || []), responseData.onCardChange] : (prevState.onCardChange || []),
                    logChange: viewToSet === "MAIN_MENU" ? [] : (responseData.logChange ? (Array.isArray(responseData.logChange) ? [...(prevState.logChange || []), ...responseData.logChange] : [...(prevState.logChange || []), responseData.logChange]) : (prevState.logChange || [])),
                    ranking: responseData.classement ? responseData.classement : (viewToSet === "GAME_OVER" ? prevState.ranking : [])
                };
            });
        };
    };

    useEffect(() => {
        const checkSessionAndReconnect = async () => {
            try {
                const response = await fetch('http://localhost:3232/api/auth/me', {
                    method: 'GET',
                    credentials: 'include'
                });
                if (response.ok) {
                    const data = await response.json();
                    preloadAllCards();
                    connectToServer(data);
                }
            } catch (err) {
                console.log("Aucune session active ou erreur de réseau.");
            }
        };

        if (!user.isConnected && !isConnectingRef.current) {
            isConnectingRef.current = true;
            checkSessionAndReconnect();
        }
    }, []);

    useEffect(() => {
        return () => {
            if (socket) socket.close();
        };
    }, [socket]);

    const sendMessage = (message: string) => {
        if (socket && socket.readyState === WebSocket.OPEN) {
            console.log("MESSAGE ENVOYEE :", message);
            socket.send(message);
        } else {
            console.warn("WebSocket déconnecté, impossible d'envoyer :", message);
        }
    };

    const resetGame = () => {
        setGameState(INITIAL_GAME_STATE);
        setChoices(INITIAL_CHOICES);
        setState(INITIAL_STATE);
        setLobbyState(INITIAL_LOBBY);
    };
    return { socket, sendMessage, connectToServer, resetGame};
}