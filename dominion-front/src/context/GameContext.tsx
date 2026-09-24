import React, {createContext, ReactNode, useContext} from 'react';
import {AppState, ItemChangeEvent, OnCardChangeEvent} from "../interfaces/AppState";
import {GameState} from "../interfaces/GameState";
import {User} from "../interfaces/User";
import {Choices} from "../interfaces/Choices";

export interface GameContextType {
    state?: AppState;
    gameState?: GameState;
    choices?: Choices;
    user?: User;
    socket?: WebSocket | null;
    sendMessage?: (message: string) => void;
    resetGame?: () => void;
    handleLeavingGame?: (gameId: string, playerId: string | number) => Promise<void>;
    handleRemoveEvent?: (e: OnCardChangeEvent) => void;
    handleRemoveItemChange?: (e: ItemChangeEvent) => void;
}

const GameContext = createContext<GameContextType | null>(null);

interface GameProviderProps {
    children: ReactNode;
    value: GameContextType;
}

export function GameProvider({ children, value }: GameProviderProps) {
    return <GameContext.Provider value={value}>{children}</GameContext.Provider>;
}

export function useGame(): GameContextType {
    const context = useContext(GameContext);
    if (!context) {
        throw new Error("useGame doit être utilisé à l'intérieur d'un GameProvider");
    }
    return context;
}