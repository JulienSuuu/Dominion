import React, { useState } from "react";
import CardHub from "../components/card/CardHub.tsx";
import './css/LobbySetupView.css';
import { createPortal } from 'react-dom';
import {useModal} from "../components/card/useModal.tsx";

function PresetsSidebar({ presets = {}, sendMessage }) {
    const { showModal, closeModal, handleContextMenu } = useModal();
    const [selectedPreset, setSelectedPreset] = useState(null);

    const handleContextMenuToUse = (e, presetData) => {
        e.preventDefault();
        setSelectedPreset(presetData);
        handleContextMenu(e);
        e.stopPropagation();
    };

    const sendPresets = (chosen) => {
        sendMessage(JSON.stringify({ action: "CHOOSE_PRESET", preset: chosen }));
    };

    return (
        <aside className="presets-sidebar">
            <h3 className="sidebar-title">Sets Recommandés</h3>

            <div className="presets-vertical-list">
                {Object.entries(presets).map(([expansion, sets]) => (
                    <div key={expansion} className="preset-group">
                        <h4 className="preset-group-title">{expansion}</h4>

                        {Object.entries(sets).map(([setName, cards]) => (
                            <div key={setName} className="preset-item">
                                <button
                                    className="preset-btn preset-tag"
                                    onClick={() => sendPresets(cards)}
                                    onContextMenu={(e) => handleContextMenuToUse(e, { setName, expansion, cards })}
                                >
                                    {setName}
                                </button>
                            </div>
                        ))}
                    </div>
                ))}
            </div>

            <button
                className="clear-btn"
                onClick={() => sendMessage(JSON.stringify({ action: "CLEAR_CARDS" }))}
            >
                CLEAR
            </button>

            {/* MODAL PORTAL */}
            {showModal && selectedPreset && createPortal(
                <div className="modal-overlay" onClick={closeModal}>
                    <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                        <button className="modal-close-btn" onClick={closeModal}>
                            &times;
                        </button>

                        <div className="modal-header">
                            <h3 className="modal-title">{selectedPreset.setName}</h3>
                            <span className="modal-expansion">{selectedPreset.expansion}</span>
                        </div>

                        <hr className="modal-divider" />

                        <div className="modal-cards-grid">
                            {selectedPreset.cards.map((card, index) => (
                                <span key={index} className="modal-card-badge">
                                    {card}
                                </span>
                            ))}
                        </div>

                        <div className="modal-footer">
                            <button
                                className="modal-select-btn"
                                onClick={() => {
                                    sendPresets(selectedPreset.cards);
                                    closeModal();
                                }}
                            >
                                Choisir ce set
                            </button>
                        </div>
                    </div>
                </div>,
                document.body
            )}
        </aside>
    );
}

function GridSelectionSection({ title, availableItems = {}, selectedItems = [], maxLimit, actionType, sendMessage, cardStyle, isLandscape = false }) {
    const selectedCount = selectedItems.length;

    const toggleItem = (item) => {
        sendMessage(JSON.stringify({
            action: actionType,
            card: item
        }));
    };

    return (
        <div className="selection-area">
            <div className="selection-header">
                <h3>{title}</h3>
                <span className={`counter ${selectedCount >= maxLimit ? 'ready' : ''}`}>
                    {selectedCount} / {maxLimit}
                </span>
            </div>

            <div className="hub-scroll-container">
                {Object.entries(availableItems).map(([extension, items]) => (
                    <div key={extension} className="extension-group">
                        <h4 className="extension-title">{extension}</h4>

                        {/* 2. Classe dynamique landscape-grid si isLandscape est true */}
                        <div className={`selection-grid ${isLandscape ? 'landscape-grid' : ''}`}>
                            {items.map(itemName => {
                                const isSelected = selectedItems.includes(itemName);
                                const isExtra = isSelected && selectedItems.indexOf(itemName) >= maxLimit;

                                return (
                                    <div key={itemName} className={isExtra ? 'card-extra-glow' : ''}>
                                        <CardHub
                                            name={itemName}
                                            isSelected={isSelected}
                                            onClick={() => {
                                                if (!isSelected && selectedCount >= maxLimit) return;
                                                toggleItem(itemName);
                                            }}
                                            classes={cardStyle}
                                        />
                                    </div>
                                );
                            })}
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}

// --- VUE PRINCIPALE ---
export default function LobbySetupView({ state, sendMessage }) {
    // État pour gérer les onglets Cartes vs Événements
    const [activeTab, setActiveTab] = useState("cards");

    // Extraction des données avec valeurs par défaut
    const players = state.users || [];
    const allReady = players.length > 0 && players.every(p => p.isReady);

    const toggleReady = () => {
        const message = allReady? "START_GAME" : "READY"
        sendMessage(JSON.stringify({ action: message}));
    };

    return (
        <div className="lobby-layout">

            {/* EN-TÊTE : Joueurs et Bouton Prêt au-dessus */}
            <header className="lobby-topbar">
                <h1>Game: {state.gameId}</h1>
                <div className="players-list">
                    {players.map((p, index) => (
                        <div key={index} className={`player-badge ${p.isReady ? 'is-ready' : ''}`}>
                            <span className="player-name">{p.pseudo || "Joueur"}</span>
                        </div>
                    ))}
                </div>

                <div className="lobby-actions">
                    <button
                        className={`ready-button ${allReady ? 'all-ready' : ''}`}
                        onClick={toggleReady}
                    >
                        {allReady ? "LANCER LA PARTIE" : "SE METTRE PRÊT"}
                    </button>
                </div>
            </header>

            <div className="lobby-body">

                <PresetsSidebar
                    presets={state.presets}
                    sendMessage={sendMessage}
                />

                {/* 3/4 - Sélections avec Onglets */}
                <main className="main-selection-panel">

                    {/* Boutons d'onglets */}
                    <div className="tabs-container">
                        <button
                            className={`tab-btn ${activeTab === 'cards' ? 'active' : ''}`}
                            onClick={() => setActiveTab('cards')}
                        >
                            Cartes de Royaume
                        </button>
                        <button
                            className={`tab-btn ${activeTab === 'events' ? 'active' : ''}`}
                            onClick={() => setActiveTab('events')}
                        >
                            Événements / Projets
                        </button>
                    </div>

                    {activeTab === "cards" ? (
                        <GridSelectionSection
                            title="Sélectionnez 10 cartes de Royaume"
                            availableItems={state.availableCards}
                            selectedItems={state.selectedCards}
                            maxLimit={10}
                            actionType="CHOOSE_CARD"
                            sendMessage={sendMessage}
                        />
                    ) : (
                        <GridSelectionSection
                            title="Sélectionnez jusqu'à 2 Événements/Projets"
                            availableItems={state.availableEvents}
                            selectedItems={state.selectedEvents}
                            maxLimit={2}
                            actionType="CHOOSE_EVENT"
                            sendMessage={sendMessage}
                            cardStyle={['event-style']}
                            isLandscape={true}
                        />
                    )}
                </main>
            </div>
        </div>
    );
}