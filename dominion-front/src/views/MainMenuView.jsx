import React, { useState, useEffect } from "react";
import StatsModal from "./statsView/StatsModal.jsx";
import "./css/MainMenuView.css";

const API_BASE_URL = "http://localhost:3232";

function MainMenuView({ user, roomCodeInput, setRoomCodeInput, setThemeUser, sendMessage }) {
    const [activeTab, setActiveTab] = useState("play");

    // Gestion de la modale et des données de statistiques
    const [isStatsOpen, setIsStatsOpen] = useState(false);
    const [stats, setStats] = useState(null);
    const [loadingStats, setLoadingStats] = useState(false);

    // États pour les thèmes
    const [allThemes, setAllThemes] = useState([]);
    const [equippedTheme, setEquippedTheme] = useState(user?.currentTheme?.code || "dominion");
    const [unlockedThemes, setUnlockedThemes] = useState(["dominion"]);
    const [loadingThemes, setLoadingThemes] = useState(true);

    // Chargement initial des thèmes
    useEffect(() => {
        fetchThemesData();
    }, []);

    useEffect(() => {
        if (user?.id) {
            setLoadingStats(true);
            fetch(`${API_BASE_URL}/api/stats/${user.id}`, { credentials: "include" })
                .then((res) => {
                    if (!res.ok) throw new Error("Erreur lors du chargement des stats");
                    return res.json();
                })
                .then((data) => {
                    setStats(data);
                })
                .catch((err) => {
                    console.error("Erreur API Stats :", err);
                })
                .finally(() => {
                    setLoadingStats(false);
                });
        }
    }, [user?.id]);

    // Récupération des thèmes (catalogue + thèmes équipés/débloqués)
    const fetchThemesData = async () => {
        try {
            setLoadingThemes(true);
            const [allRes, meRes] = await Promise.all([
                fetch(`${API_BASE_URL}/api/themes`, { credentials: "include" }),
                fetch(`${API_BASE_URL}/api/themes/me`, { credentials: "include" })
            ]);

            if (allRes.ok) {
                const themesList = await allRes.json();
                setAllThemes(themesList);
            }
            if (meRes.ok) {
                const meData = await meRes.json();
                setEquippedTheme(meData.currentThemeCode || "dominion");
                setUnlockedThemes(meData.unlockedThemeCodes || ["dominion"]);
            }
        } catch (err) {
            console.error("Erreur lors de la récupération des thèmes :", err);
        } finally {
            setLoadingThemes(false);
        }
    };

    // WebSocket Actions
    const handleCreateGame = () => {
        sendMessage(JSON.stringify({
            action: "CREATE_GAME",
            playerName: user?.pseudo,
            playerId: user?.id
        }));
    };

    const handleJoinGame = (e) => {
        e.preventDefault();
        if (roomCodeInput.trim()) {
            sendMessage(JSON.stringify({
                action: "JOIN_GAME",
                playerName: user?.pseudo,
                playerId: user?.id,
                gameId: roomCodeInput.trim().toUpperCase()
            }));
        }
    };

    // Gestion des thèmes
    const handleEquipTheme = async (themeCode) => {
        try {
            const res = await fetch(`${API_BASE_URL}/api/themes/equip/${themeCode}`, {
                method: "PATCH",
                credentials: "include"
            });
            if (res.ok) {
                setEquippedTheme(themeCode);
                const theme = allThemes.find(t => t.code === themeCode);
                setThemeUser(theme);
            } else {
                console.error(`Erreur d'équipement : ${res.status}`);
            }
        } catch (err) {
            console.error("Erreur d'équipement :", err);
        }
    };

    const handleUnlockTheme = async (themeCode) => {
        try {
            const res = await fetch(`${API_BASE_URL}/api/themes/unlock/${themeCode}`, {
                method: "POST",
                credentials: "include"
            });

            if (res.ok) {
                const data = await res.json();
                setUnlockedThemes(data.unlockedThemeCodes || []);
            } else {
                const errorData = await res.json().catch(() => ({}));
                alert(errorData.error || `Erreur ${res.status} : Impossible de débloquer le thème.`);
            }
        } catch (err) {
            console.error("Erreur de déblocage :", err);
        }
    };

    const getUserStatValue = (keyInput, detailName) => {
        if (!stats || !keyInput) return 0;

        const keyStr = typeof keyInput === "object"
            ? (keyInput.name || keyInput.key || "")
            : String(keyInput);

        let statEntry = null;
        if (Array.isArray(stats)) {
            statEntry = stats.find(s => s.key === keyStr);
        } else {
            statEntry = stats[keyStr];
        }

        if (!statEntry) return 0;

        if (detailName && statEntry.details) {
            return statEntry.details[detailName] ?? 0;
        }

        return statEntry.value ?? 0;
    };

    // Formate proprement la clé de la statistique
    const getStatLabel = (keyInput, detailName) => {
        if (detailName) return detailName;

        const keyStr = typeof keyInput === "object"
            ? (keyInput.displayName || keyInput.name || keyInput.key || "")
            : String(keyInput || "");

        const labelMap = {
            "TOTAL_CARDS_PLAYED": "cartes jouées",
            "GAMES_PLAYED": "parties jouées",
            "GAMES_WON": "victoires",
            "RESOURCES_OBTAINED": "ressources obtenues",
            "MONEY": "pièces d'or"
        };

        if (labelMap[keyStr]) return labelMap[keyStr];
        return keyStr.replaceAll("_", " ").toLowerCase();
    };

    // Affichage des exigences (Verrouillé vs Débloqué)
    const renderUnlockRequirement = (theme, isUnlocked) => {
        const unlock = theme.unlockType;
        if (!unlock) return null;

        // Si le thème est DÉBLOQUÉ : on affiche la condition validée
        if (isUnlocked) {
            switch (unlock.type) {
                case 'FREE':
                    return '✓ Gratuit par défaut';
                case 'LEVEL':
                    return `✓ Niveau ${theme.unlockValue} atteint`;
                case 'STAT': {
                    const label = getStatLabel(unlock.key, unlock.detailName);
                    return `✓ ${getUserStatValue(unlock.key, unlock.detailName)} / ${theme.unlockValue} (${label})`;
                }
                case 'PURCHASE':
                    return '✓ Acheté';
                case 'ACHIEVEMENT':
                    return `✓ Succès accompli`;
                default:
                    return '✓ Condition remplie';
            }
        }

        // Si le thème est VERROUILLÉ : on affiche la progression en cours
        switch (unlock.type) {
            case 'FREE':
                return 'Gratuit par défaut';

            case 'LEVEL': {
                const currentLevel = user?.level || 0;
                return `Niveau ${theme.unlockValue} requis (Actuel : ${currentLevel})`;
            }

            case 'PURCHASE':
                return 'Acheter dans la boutique';

            case 'STAT': {
                const currentVal = getUserStatValue(unlock.key, unlock.detailName);
                const label = getStatLabel(unlock.key, unlock.detailName);
                return `Progression : ${currentVal} / ${theme.unlockValue} (${label})`;
            }

            case 'ACHIEVEMENT':
                return `Succès requis : ${unlock.subject}`;

            default:
                return null;
        }
    };

    return (
        <div className="main-menu-hub">
            {/* Barre de profil utilisateur */}
            <header className="menu-topbar">
                <div className="user-info">
                    <div>
                        <h2 className="user-pseudo">{user?.pseudo || "Joueur"}</h2>
                    </div>
                </div>

                <nav className="menu-nav-tabs">
                    <button
                        className={`tab-btn ${activeTab === "play" ? "active" : ""}`}
                        onClick={() => setActiveTab("play")}
                    >
                        Jouer
                    </button>
                    <button
                        className="tab-btn"
                        onClick={() => setIsStatsOpen(true)}
                    >
                        Statistiques
                    </button>
                    <button
                        className={`tab-btn ${activeTab === "themes" ? "active" : ""}`}
                        onClick={() => setActiveTab("themes")}
                    >
                        Thèmes & Boutique
                    </button>
                </nav>
            </header>

            {/* Contenu principal */}
            <main className="menu-content">
                {/* ONGLET : JOUER */}
                {activeTab === "play" && (
                    <div className="hub-card play-card">
                        <h3>Multiplayer</h3>
                        <p className="card-subtitle">Créez votre propre salon ou rejoignez une partie existante.</p>

                        <button className="primary-action-btn" onClick={handleCreateGame}>
                            CRÉER UNE PARTIE
                        </button>

                        <div className="separator">
                            <span>OU</span>
                        </div>

                        <form onSubmit={handleJoinGame} className="join-form">
                            <input
                                type="text"
                                className="room-input"
                                placeholder="CODE DE PARTIE (EX : H7K9R)"
                                value={roomCodeInput}
                                onChange={(e) => setRoomCodeInput(e.target.value)}
                                maxLength={10}
                            />
                            <button
                                type="submit"
                                className="secondary-action-btn"
                                disabled={!roomCodeInput.trim()}
                            >
                                REJOINDRE
                            </button>
                        </form>
                    </div>
                )}

                {/* ONGLET : THÈMES */}
                {activeTab === "themes" && (
                    <div className="hub-card themes-card">
                        <div className="themes-header">
                            <div>
                                <h3>Personnalisation</h3>
                                <p className="card-subtitle">Débloquez et équipez des thèmes visuels uniques.</p>
                            </div>
                            <div className="unlocked-count-badge">
                                Thèmes débloqués : {unlockedThemes.length} / {allThemes.length}
                            </div>
                        </div>

                        {loadingThemes ? (
                            <p className="loading-text">Chargement des thèmes...</p>
                        ) : (
                            <div className="themes-grid">
                                {allThemes.map((theme) => {
                                    const isUnlocked = unlockedThemes.includes(theme.code);
                                    const isEquipped = equippedTheme === theme.code;

                                    return (
                                        <div
                                            key={theme.code}
                                            className={`theme-tile ${isEquipped ? "equipped" : ""} ${!isUnlocked ? "locked" : ""}`}
                                        >
                                            <div className={`theme-preview preview-${theme.code}`}>
                                                <span className="theme-badge">
                                                    {isEquipped ? "ÉQUIPÉ" : isUnlocked ? "DÉBLOQUÉ" : "VERROUILLÉ"}
                                                </span>
                                            </div>

                                            <div className="theme-details">
                                                <h4>{theme.name}</h4>
                                                <p className={`theme-req ${isUnlocked ? "completed" : ""}`}>
                                                    {renderUnlockRequirement(theme, isUnlocked)}
                                                </p>

                                                {isEquipped ? (
                                                    <button className="theme-btn active" disabled>Actif</button>
                                                ) : isUnlocked ? (
                                                    <button className="theme-btn equip" onClick={() => handleEquipTheme(theme.code)}>
                                                        Équiper
                                                    </button>
                                                ) : (
                                                    <button className="theme-btn unlock" onClick={() => handleUnlockTheme(theme.code)}>
                                                        Débloquer
                                                    </button>
                                                )}
                                            </div>
                                        </div>
                                    );
                                })}
                            </div>
                        )}
                    </div>
                )}
            </main>

            {/* Modale des statistiques réutilisée */}
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

export default MainMenuView;