import React from 'react';
import './css/GameEnd.css';
import {useGame} from "../context/GameContext.tsx";

export function GameEnd() {
    const { state, user,  resetGame, sendMessage, handleLeavingGame } = useGame();

    const classement = state?.ranking || [];

    const handleReturnMenu = (gameId, playerId) => {
        handleLeavingGame(gameId, playerId);
        resetGame();
    };

    const endTitle = user.id === classement[0]?.client?.id ? "Victory !" : "Defeat !"



    return (
        <div className="game-end-container">
            <div className="game-end-card">
                <h1 className="game-end-title">{endTitle}</h1>
                <p className="game-end-subtitle">La partie est terminée</p>

                {/* Podium Top 3 */}
                <div className="podium-container">
                    {/* 2ème Place */}
                    {classement[1] && (
                        <div className="podium-step rank-2">
                            <div className="avatar">🥈</div>
                            <div className="player-name">{classement[1].client?.name || classement[1].client?.pseudo || "Joueur 2"}</div>
                            <div className="score">{classement[1].points} PTS</div>
                            <div className="pedestal">2</div>
                        </div>
                    )}

                    {/* 1ère Place */}
                    {classement[0] && (
                        <div className="podium-step rank-1">
                            <div className="crown">👑</div>
                            <div className="avatar">🥇</div>
                            <div className="player-name">{classement[0].client?.name || classement[0].client?.pseudo || "Vainqueur"}</div>
                            <div className="score">{classement[0].points} PTS</div>
                            <div className="pedestal">1</div>
                        </div>
                    )}

                    {/* 3ème Place */}
                    {classement[2] && (
                        <div className="podium-step rank-3">
                            <div className="avatar">🥉</div>
                            <div className="player-name">{classement[2].client?.name || classement[2].client?.pseudo || "Joueur 3"}</div>
                            <div className="score">{classement[2].points} PTS</div>
                            <div className="pedestal">3</div>
                        </div>
                    )}
                </div>

                {/* Tableau complet du classement */}
                <div className="scoreboard-section">
                    <h3>Tableau des scores</h3>
                    <table className="scoreboard-table">
                        <thead>
                        <tr>
                            <th>Rang</th>
                            <th>Joueur</th>
                            <th>Points de Victoire</th>
                        </tr>
                        </thead>
                        <tbody>
                        {classement.map((item, index) => (
                            <tr key={index} className={index === 0 ? "winner-row" : ""}>
                                <td className="rank-cell">#{index + 1}</td>
                                <td className="player-cell">
                                    {item.client?.name || item.client?.pseudo || `Joueur ${index + 1}`}
                                </td>
                                <td className="points-cell">{item.points} pts</td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>

                {/* Action de retour */}
                <button className="btn-return-menu" onClick={() => handleReturnMenu(state.gameId, user.id)}>
                    Retour au Menu Principal
                </button>
            </div>
        </div>
    );
}