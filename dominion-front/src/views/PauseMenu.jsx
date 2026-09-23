import React, { useState, useEffect } from 'react';
import StatsModal from './statsView/StatsModal.jsx';
import './css/PauseMenu.css';

export default function PauseMenu({ user, onLogout }) {
    const [isOpen, setIsOpen] = useState(false);
    const [isStatsOpen, setIsStatsOpen] = useState(false);
    const [stats, setStats] = useState(null);
    const [loadingStats, setLoadingStats] = useState(false);

    useEffect(() => {
        const handleKeyDown = (e) => {
            if (e.key === 'Escape' && user?.isConnected) {
                if (isStatsOpen) {
                    setIsStatsOpen(false);
                } else {
                    setIsOpen((prev) => !prev);
                }
            }
        };

        window.addEventListener('keydown', handleKeyDown);
        return () => window.removeEventListener('keydown', handleKeyDown);
    }, [user?.isConnected, isStatsOpen]);

    useEffect(() => {
        if (isStatsOpen && user?.id) {
            setLoadingStats(true);
            fetch(`http://localhost:3232/api/stats/${user.id}`, { credentials: "include" })
                .then((res) => {
                    if (!res.ok) throw new Error('Erreur lors du chargement des stats');
                    return res.json();
                })
                .then((data) => {
                    setStats(data);
                    setLoadingStats(false);
                })
                .catch((err) => {
                    console.error('Erreur API Stats :', err);
                    setLoadingStats(false);
                });
        }
    }, [isStatsOpen, user?.id]);

    if (!isOpen || !user?.isConnected) return null;

    return (
        <div className="pause-menu-overlay">
            <div className="pause-menu-modal">
                <h2>Menu</h2>
                <p>You : <strong>{user.pseudo}</strong></p>

                <div className="pause-menu-actions">
                    <button
                        className="btn-resume"
                        onClick={() => setIsOpen(false)}
                    >
                        Return
                    </button>

                    <button
                        className="btn-stats"
                        onClick={() => setIsStatsOpen(true)}
                    >
                        Stats
                    </button>

                    <button
                        className="btn-logout"
                        onClick={() => {
                            setIsStatsOpen(false);
                            setIsOpen(false);
                            onLogout();
                        }}
                    >
                        Disconnect You
                    </button>
                </div>
            </div>

            {/* Modale indépendante des statistiques */}
            {isStatsOpen && (
                <StatsModal
                    stats={stats}
                    loading={loadingStats}
                    onClose={() => setIsStatsOpen(false)}
                />
            )}
        </div>
    );
}