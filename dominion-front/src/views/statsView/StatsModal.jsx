import React, { useState } from 'react';
import StatCategoryGroup from './StatCategoryGroup';
import '../css/StatsModal.css';

export default function StatsModal({ stats, loading, onClose }) {
    const [expandedKey, setExpandedKey] = useState(null);

    const toggleExpand = (key) => {
        setExpandedKey(prev => prev === key ? null : key);
    };

    const getCardImageUrl = (cardName) => {
        const formattedName = cardName.replace(/[^A-Za-z]/g, '');
        return new URL(`../../assets/cards/${formattedName}.jpg`, import.meta.url).href;
    };

    const statsArray = Array.isArray(stats)
        ? stats
        : (stats && typeof stats === 'object' ? Object.values(stats) : []);

    const groupedStats = statsArray.reduce((acc, stat) => {
        if (!stat) return acc;
        const cat = stat.category || 'Général';
        if (!acc[cat]) acc[cat] = [];
        acc[cat].push(stat);
        return acc;
    }, {});

    return (
        <div className="stats-overlay" onClick={onClose}>
            <div className="stats-modal" onClick={(e) => e.stopPropagation()}>

                <div className="stats-header">
                    <h2>Statistiques du Joueur</h2>
                    <button className="btn-close" onClick={onClose} aria-label="Fermer">✕</button>
                </div>

                <div className="stats-content">
                    {loading ? (
                        <div className="stats-state">Chargement des données...</div>
                    ) : statsArray.length === 0 ? (
                        <div className="stats-state">Aucune statistique enregistrée</div>
                    ) : (
                        <div className="categories-wrapper">
                            {Object.entries(groupedStats).map(([categoryName, categoryStats]) => (
                                <StatCategoryGroup
                                    key={categoryName}
                                    categoryName={categoryName}
                                    categoryStats={categoryStats}
                                    expandedKey={expandedKey}
                                    onToggle={toggleExpand}
                                    getCardImageUrl={getCardImageUrl}
                                />
                            ))}
                        </div>
                    )}
                </div>

                <div className="stats-footer">
                    <button className="btn-secondary" onClick={onClose}>Close</button>
                </div>

            </div>
        </div>
    );
}