import React from "react";
import CardHub from "../components/card/CardHub.tsx";
import "./css/ExtraCardSelectionView.css";

function ExtraCardSelectionView({generalState,  state = {}, sendMessage }) {
    const isWitch = generalState.view === "YOUNG_WITCH_CHOICE";
    const title = isWitch ? "La Jeune Sorcière" : "Le Passeur";
    const description = isWitch
        ? "Choisissez une carte (coût 2$ ou 3$) qui servira de Fléau (Bane) :"
        : "Choisissez une carte (coût 3$ ou 4$) à mettre de côté pour la partie :";

    const action = isWitch ? "CONFIRM_YOUNG_WITCH" : "CONFIRM_FERRYMAN";
    const themeClass = isWitch ? "witch-theme" : "ferryman-theme";

    const options = state.options || {};
    const categories = Object.entries(options);

    const sendExtraCard = (cardName) => {
        if (!sendMessage) return;
        sendMessage(JSON.stringify({
            action: action,
            card: cardName
        }));
    };

    return (
        <div className="hub-layout extra-card-overlay">
            <div className={`hub-parchment selection-modal ${themeClass}`}>
                <header className="modal-header">
                    <span className="theme-icon-badge">{isWitch ? "🧹" : "⛵"}</span>
                    <h2>{title}</h2>
                    <p className="subtitle">{description}</p>
                </header>

                <main className="selection-container scrollable">
                    {categories.length === 0 ? (
                        <p className="empty-options">Aucune carte disponible pour cette sélection.</p>
                    ) : (
                        categories.map(([expansion, cards]) => (
                            <section key={expansion} className="extension-group">
                                <h3 className="extension-title">
                                    <span>Extension : {expansion}</span>
                                    <span className="count-tag">{cards.length} cartes</span>
                                </h3>
                                <div className="selection-grid">
                                    {cards.map(cardName => (
                                        <div key={cardName} className="card-item-wrapper">
                                            <CardHub
                                                name={cardName}
                                                onClick={() => sendExtraCard(cardName)}
                                            />
                                        </div>
                                    ))}
                                </div>
                            </section>
                        ))
                    )}
                </main>

                <footer className="modal-footer">
                    <small>Cette carte constituera la 11ᵉ pile du royaume pour cette partie.</small>
                </footer>
            </div>
        </div>
    );
}

export default ExtraCardSelectionView;