import React, { useState, useEffect } from 'react';
import AnimatedShopCard from './animatedComponents/AnimatedShopCard';
import './Shop.css';
import {useGame} from "../context/GameContext";


interface ShopProps {
    isOpen : boolean;
    onClose : () => void;
}

function Shop({ isOpen, onClose} : ShopProps) {
    const {gameState, sendMessage} = useGame()
    const {shopSupply, nocturne} = gameState

    const [activeTab, setActiveTab] = useState('kingdom');

    const hasAside = shopSupply.aside && Object.keys(shopSupply.aside).length > 0;
    const hasNocturne =  nocturne?.nocturnePile && Object.keys(nocturne?.nocturnePile).length > 0;
    const hasEvents = shopSupply.events && Array.isArray(shopSupply.events) && shopSupply.events.length > 0;

    useEffect(() => {
        if (isOpen) {
            setActiveTab('kingdom');
        }
    }, [isOpen]);

    return (
        <div className={`shop-sliding-panel ${isOpen ? 'open' : ''}`}>
            <div className="shop-container">
                <div className="shop-header">
                    <h2>Shop</h2>
                    <button className="shop-close-btn" onClick={onClose}>&times;</button>
                </div>

                <div className="shop-tabs">
                    <button
                        className={`shop-tab ${activeTab === 'kingdom' ? 'active' : ''}`}
                        onClick={() => setActiveTab('kingdom')}
                    >
                        Kingdoms & Commons
                    </button>
                    {hasAside && (
                        <button
                            className={`shop-tab ${activeTab === 'aside' ? 'active' : ''}`}
                            onClick={() => setActiveTab('aside')}
                        >
                            Aside
                        </button>
                    )}
                    {hasNocturne && (
                        <button
                            className={`shop-tab ${activeTab === 'nocturne' ? 'active' : ''}`}
                            onClick={() => setActiveTab('nocturne')}
                        >
                            Nocturne
                        </button>
                    )}
                    {hasEvents && (
                        <button
                            className={`shop-tab ${activeTab === 'events' ? 'active' : ''}`}
                            onClick={() => setActiveTab('events')}
                        >
                            Event
                        </button>
                    )}
                </div>

                <div className="shop-content">
                    {/* ONGLET : ROYAUME & COMMUNES */}
                    {activeTab === 'kingdom' && (
                        <div className="shop-section shop-kingdom-section">
                            <div className="shop-grid">
                                {shopSupply.supply && shopSupply.supply.slice(0, -gameState.infos.size).map(pile => (
                                    <AnimatedShopCard
                                        key={pile.card?.id || pile.name} name={pile.name} cardData={pile.card} location="kingdom"
                                        number={pile.number} classes={['half']}
                                        messageType="SUPPLY" overlay={pile.number === 0} sendMessage={sendMessage}
                                    />
                                ))}
                            </div>
                            <h3 className="shop-divider">Cartes Communes</h3>
                            <div className="shop-grid">
                                {shopSupply.supply && shopSupply.supply.slice(-gameState.infos.size).map(pile => (
                                    <AnimatedShopCard
                                        key={pile.card?.id || pile.name} name={pile.name} cardData={pile.card}
                                        location="kingdom" number={pile.number} messageType="SUPPLY"
                                        overlay={pile.number === 0} sendMessage={sendMessage}
                                    />
                                ))}
                            </div>
                        </div>
                    )}

                    {/* ONGLET : RÉSERVE */}
                    {activeTab === 'aside' && hasAside && (
                        <div className="shop-section">
                            {Object.entries(shopSupply.aside)
                                .filter(([category]) => category.toUpperCase() !== 'EVENT')
                                .map(([category, piles]) => (
                                    <div key={category} className="shop-group">
                                        <h3>{category.toUpperCase()}</h3>
                                        <div className="shop-grid">
                                            {piles.map(pile => (
                                                <AnimatedShopCard
                                                    key={pile.card?.id || pile.name} name={pile.name} cardData={pile.card} location="aside" number={pile.number}
                                                    messageType={'NONE'}
                                                    classes={['half']} overlay={pile.number === 0}
                                                    sendMessage={sendMessage}
                                                />
                                            ))}
                                        </div>
                                    </div>
                                ))}
                        </div>
                    )}

                    {/* ONGLET : NOCTURNE */}
                    {activeTab === 'nocturne' && hasNocturne && (
                        <div className="shop-section">
                            <div className="shop-grid">
                                {Object.entries(nocturne.nocturnePile).map(([pileName, pileData]) => {
                                    const pile = Array.isArray(pileData) ? pileData[0] : pileData;
                                    return (
                                        <AnimatedShopCard
                                            key={pile.card?.id ||pile.name} name={pile.name} cardData={pile.card} location="nocturne" number={pile.number}
                                            classes={['event-style']} messageType={'NONE'}
                                            overlay={pile.number === 0} sendMessage={sendMessage}
                                        />
                                    );
                                })}
                            </div>
                        </div>
                    )}

                    {/* ONGLET : ÉVÉNEMENTS */}
                    {activeTab === 'events' && hasEvents && (
                        <div className="shop-section">
                            <div className="shop-grid">
                                {shopSupply.events.map(pile => (
                                    <AnimatedShopCard
                                        key={pile.card?.id || pile.name } name={pile.name} cardData={pile.card} number={pile.number}
                                        messageType='EVENT' classes={['event-style']}
                                        overlay={pile.number === 0} sendMessage={sendMessage}
                                    />
                                ))}
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}

export default Shop;