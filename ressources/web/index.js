const { h, render } = preact;
const { useState, useEffect, useRef } = preactHooks;
const html = htm.bind(h);
const adventureTokenKeys = [
    'card_reduction_token', 'estate_token', 'one_action_token',
    'one_buy_token', 'one_card_token', 'one_money_token', 'trashing_token'
];




const socket = new WebSocket("ws://localhost:3232");
function sendMessage(message) {
    socket.send(message);
    console.log(`Message envoyé: "${message}"`);
}

function Main() {
    const [state, setState] = useState({
        view: "LOBBY",
        supply: null,
        players: null,
        turn_player: null,
        active_player: null,
        instruction: null,
        choices: null,
        buttons: null,
        selection_cards: null,
        message_type: null,
        mode:false,
        availableCards: {},
        presets: {},
        selectedCards: null,
        aside: {},
        events: {},
        options: {}


    });

    useEffect(() => {
        socket.onmessage = (event) => {
            const data = JSON.parse(event.data);
            console.log("--- MESSAGE REÇU ---", data);
            console.log("OPTIONS REÇUES:", data.options); // Vérifie si c'est bien l'objet avec les extensions

            setState(prevState => {
                const hasGame = data.game !== null && data.game !== undefined;
                return {
                    ...prevState,
                    view: data.view || prevState.view,
                    availableCards: data.availableCards || prevState.availableCards,
                    selectedCards: data.selectedCards || prevState.selectedCards,
                    presets: data.presets || prevState.presets,
                    options: data.options || prevState.options,

                    supply: hasGame ? data.game.supply : prevState.supply,
                    aside: hasGame? data.game.aside : prevState.aside,
                    events: hasGame? data.game.events : prevState.events,
                    players: hasGame ? data.game.players : prevState.players,
                    turn_player: hasGame ? data.game.turn_player : prevState.turn_player,
                    size: hasGame ? data.game.size : prevState.size,
                    log: hasGame ? data.game.log : prevState.log,


                    active_player: data.active_player !== undefined ? data.active_player : prevState.active_player,
                    instruction: data.instruction || prevState.instruction,
                    choices: data.choices || prevState.choices,
                    buttons: data.buttons || prevState.buttons,
                    selection_cards: data.selection_cards || prevState.selection_cards,
                    mode: data.mode !== undefined ? data.mode : prevState.mode,
                };
            });
        };
        return () => { socket.onmessage = null; };
    }, []);

    if (state.view === "FERRYMAN_CHOICE" || state.view === "YOUNG_WITCH_CHOICE") {
        const isWitch = state.view === "YOUNG_WITCH_CHOICE";
        const title = isWitch ? "La Jeune Sorcière" : "Le Passeur";
        const description = isWitch
            ? "Choisissez une carte (2$ ou 3$) comme Fléau (Bane) :"
            : "Choisissez une carte (3$ ou 4$) à mettre de côté :";
        const prefix = isWitch ? "CONFIRM_YOUNG_WITCH:" : "CONFIRM_FERRYMAN:";
        const themeClass = isWitch ? "witch-theme" : "ferryman-theme";
        const categories = Object.entries(state.options || {});

        return html`
            <div class="hub-layout">
                <div class="hub-parchment selection-modal ${themeClass}">
                    <div class="modal-header">
                        <h2>${title}</h2>
                        <p class="subtitle">${description}</p>
                    </div>

                    <div class="selection-container scrollable">
                        ${categories.map(([expansion, cards]) => html`
                            <div class="extension-group">
                                <h3 class="extension-title">${expansion}</h3>
                                <div class="selection-grid">
                                    ${cards.map(cardName => html`
                                        <${CardHub}
                                                key=${cardName}
                                                name=${cardName}
                                                onClick=${() => sendMessage(prefix + cardName)}
                                        />
                                    `)}
                                </div>
                            </div>
                        `)}
                    </div>

                    <div class="modal-footer">
                        <small>Cette carte sera une pile supplémentaire (11ème pile).</small>
                    </div>
                </div>
            </div>`;
    }

    if (!state.supply) {
        return html`
            <div id="hub" class="hub-layout">
                <div class="hub-parchment">
                    <h1>Configuration du Royaume</h1>

                    <section class="presets-section">
                        <h4 class="section-subtitle">Sets Recommandés</h4>
                        <div class="presets-scroll-area">
                        ${Object.entries(state.presets || {}).map(([expansion, sets]) => {
                            const isCombo = expansion.includes("&");

                            return html`
                                <div class="set-extension-group ${isCombo ? 'combo-mode' : ''}">
                                    <div class="group-header">
                                        <h4 class="set-extension-title">${expansion}</h4>
                                        <div class="title-line"></div>
                                    </div>

                                    <div class="presets-container">
                                        ${Object.entries(sets).map(([setName, cards]) => html`
                                            <div class="preset-item">
                                                <button class="preset-tag" onClick=${() => sendMessage("SET_CARDS:" + cards.join(","))}>
                                                    ${setName}
                                                    <span class="tooltip-text">
                                                        <strong class="tooltip-title">${setName}</strong><br/>
                                                        <small class="tooltip-expansion">${expansion}</small>
                                                        <div class="tooltip-divider"></div>
                                                        <div class="tooltip-content">
                                                            ${cards.slice(0, 10).join(', ')}
                                                            ${cards.length > 10 ? html`
                                                                <div class="tooltip-extra">
                                                                    <span class="extra-label">+ EXTRA :</span> ${cards[10]}
                                                                </div>
                                                            ` : ''}
                                                        </div>
                                                    </span>
                                                </button>
                                            </div>
                                        `)}
                                    </div>
                                </div>
                            `;
                        })} 
                        </div>

                        <div class="presets-actions">
                            <button class="preset-tag clear-btn" onClick=${() => sendMessage("CLEAR_CARDS")}>
                                🗑️ Vider tout
                            </button>
                        </div>
                    </section>
                    
                    
                    
                    <section class="hub-section">
                        <div class="hub-header">
                            <h3>Sélectionnez 10 cartes de Royaume</h3>
                            <div class="counter ${state.selectedCards?.length >= 10 ? 'ready' : ''}">
                                ${state.selectedCards?.length || 0} / 10
                            </div>
                        </div>

                        <div class="hub-scroll-container">
                            ${state.availableCards && Object.entries(state.availableCards).map(([extension, cards]) => html`
                            <div class="extension-group">
                                <h2 class="extension-title">${extension}</h2>
                                <div class="selection-grid">
                                    ${cards.map(cardName => {
                                        const isSelected = state.selectedCards?.includes(cardName);
                                        const selectionIndex = state.selectedCards?.indexOf(cardName);
                                        const isExtra = isSelected && selectionIndex >= 10;
                                        
                                        return html`
                                            <div class="${isExtra ? 'card-extra-glow' : ''}">
                                                <${CardHub}
                                                        key=${cardName}
                                                        name=${cardName}
                                                        isSelected=${isSelected}
                                                        onClick=${() => {
                                                            if (!isSelected && state.selectedCards?.length >= 10) return;
                                                            sendMessage("TOGGLE:" + cardName);
                                                        }}
                                                />
                                            </div>`;
                                    })}
                                </div>
                            </div>
                        `)}
                        </div>
                    </section>

                    <div class="hub-footer">
                        <button class="start-button"
                                disabled=${state.selectedCards?.length < 10}
                                onClick=${() => sendMessage("START_GAME")}>
                            COMMENCER LA PARTIE
                        </button>
                    </div>
                </div>
            </div>`;
    }
        return html`
        <div id="main">
            ${state.selection_cards && html`
                <${SelectionOverlay}
                        instruction=${state.instruction}
                        cards=${state.selection_cards}
                        choices=${state.choices}
                        buttons=${state.buttons}
                        mode=${state.mode}
                        onSelect=${(choice) => sendMessage(choice)}
                />
            `}

            <div id="game">
                <div id="aside-supply-zone">
                    ${state.aside && Object.keys(state.aside).length > 0 && Object.entries(state.aside).filter(([category]) => category.toUpperCase() !== 'Event').map(([category, piles]) => html`
                        <div class="aside-group">
                            <div class="aside-header">${category.toUpperCase()}</div>
                            <div class="aside-row">
                                ${piles.map(pile => html`
                                    <${Card} 
                                            key=${pile.card} 
                                            name=${pile.card} 
                                            number=${pile.number} 
                                            cost=${pile.cost} 
                                            potion=${pile.potion} 
                                            debt=${pile.debt}
                                            messageType=${category.toUpperCase() === 'NONE'}
                                            classes=${['half', category.toUpperCase() === '']}
                                            overlay=${pile.number === 0}
                                    />
                                `)}
                            </div>
                        </div>
                    `)}
                </div>
                
                
                
                <div id="supply">
                    <div id="kingdom_supply">
                        ${state.supply.slice(0, -state.size).map(pile =>
            html`<${Card}
                                        key=${pile.card}
                                        name=${pile.card}
                                        number=${pile.number}
                                        cost=${pile.cost}
                                        potion=${pile.potion}
                                        debt=${pile.debt}
                                        classes=${['half']}
                                        messageType="SUPPLY"
                                        overlay=${pile.number === 0}
                                        players=${state.players}
            />`
        )}
                    </div>
                    <div id="common_supply">
                        ${state.supply.slice(-state.size).map(pile =>
            html`<${Card}
                                        key=${pile.card}
                                        name=${pile.card}
                                        number=${pile.number}
                                        cost=${pile.cost}
                                        potion=${pile.potion}
                                        debt=${pile.debt}
                                        messageType="SUPPLY"
                                        overlay=${pile.number === 0}
                                        players=${state.players}
                                />`
        )}
                    </div>
                </div>
                
                <div id="players">
                    ${state.players.map((player_data, index) => {
            const is_active = index === state.active_player;
            const is_turn_player = index === state.turn_player;
            return html`<${Player}
                                key=${player_data.name}
                                data=${player_data}
                                is_active=${is_active}
                                is_turn_player=${is_turn_player}
                                instruction=${is_active ? state.instruction : ""}
                                choices=${is_active ? state.choices : []}
                                buttons=${is_active ? state.buttons : []}
                                game_over=${state.instruction === "Game over"}
                                mode=${state.mode}
                                state=${state}
                        />`;
            
        })}
                </div>
            </div>

            <div id="events-sidebar">
                ${state.events && html`
                    <div class="aside-group">
                        <div class="aside-header">EVENTS</div>
                        <div class="aside-row">
                            ${state.events.map(pile => html`
                                <${Card} 
                                        key=${pile.card} 
                                        name=${pile.card} 
                                        number=${pile.number} 
                                        cost=${pile.cost} 
                                        potion=${pile.potion} 
                                        debt=${pile.debt}
                                        messageType='EVENT'
                                        classes=${['event-style']}
                                        overlay=${pile.number === 0}
                                />
                            `)}
                        </div>
                    </div>
                `}
            </div>
            <${Log} log=${state.log} />
            
        </div>`;

}

function CardHub({ name, isSelected, onClick }) {
    const short_name = name ? name.replace(/[^A-Za-z]/g, '') : '';

    const handleMouseEnter = () => {
        if (!name) return;
        const zoomEl = document.getElementById('zoom');
        if (zoomEl){
            zoomEl.style.backgroundImage = `url(cards/${short_name}.jpg)`;
            zoomEl.classList.add('active');
        }
    };

    const handleMouseLeave = () => {
        const zoomEl = document.getElementById('zoom');
        if (zoomEl) zoomEl.classList.remove('active');
    };

    return html`
        <div
            class="card ${isSelected ? 'selected' : ''}"
            style=${name ? { backgroundImage: `url(cards/${short_name}.jpg)` } : null}
            onClick=${onClick}
            onMouseEnter=${handleMouseEnter}
            onMouseLeave=${handleMouseLeave}
        > 
            ${isSelected && html`
                <div class="selected-check">
                    <span>✓</span>
                </div>
            `}
        </div>`;
}

function Card({ name, number, cost, potion, debt, classes, messageType, overlay, style, players = {} }) {
    const short_name = name ? name.replace(/[^A-Za-z]/g, '') : '';

    const playerList = Object.values(players || {});

    let tokensOnThisCard = [];
    playerList.forEach(p => {
        const supplyTokens = p.tokens?.supplyTokens || {};
        Object.keys(supplyTokens).forEach(key => {
            if (supplyTokens[key] === name) {
                tokensOnThisCard.push({
                    key: key,
                    color: p.color,
                    pid: p.id
                });
            }
        });
    });

    const handleMouseEnter = () => {
        if (!name) return;
        const zoomEl = document.getElementById('zoom');
        if (zoomEl){
            zoomEl.style.backgroundImage = `url(cards/${short_name}.jpg)`;
            zoomEl.classList.add('active');
            if(messageType === 'EVENT') zoomEl.classList.add('event');
        }
    };

    const handleMouseLeave = () => {
        const zoomEl = document.getElementById('zoom');
        if (zoomEl) zoomEl.classList.remove('active');
        if (zoomEl) zoomEl.classList.remove('event');
    };

    const combinedStyle = {
        ...(style || {}),
        backgroundImage: name ? `url(cards/${short_name}.jpg)` : null
    };

    return html`
        <div
                class="${['card', ...(classes || [])].join(' ')}"
                data-name="${name}"
                style=${combinedStyle}
                onMouseEnter=${handleMouseEnter}
                onMouseLeave=${handleMouseLeave}
                onClick=${name && messageType ? () => sendMessage(`${messageType}:${name}`) : null}
        >
            <div class="token-anchor">
                ${tokensOnThisCard.map(t => html`
                    <${Token}
                            key=${`card-${t.key}-${t.pid}`}
                            id="token-${t.key}-${t.pid}"
                            name=${t.key}
                            playerColor=${t.color}
                            classes=${["adventure-token", "anchored"]}
                    />
                `)}
            </div>

            <div class="card-footer">
                ${cost > 0 ? html`<div class="cost">${cost}</div>` : null}
                ${potion > 0 ? html`<div class="potion"></div>` : null}
                ${debt > 0 ? html`<div class="debt">${debt}</div>` : null}
            </div>

            ${number ? html`<div class="number">${number}</div>` : null}
            ${overlay && html`<div class="overlay"></div>`}
        </div>`;
}

function Token({ name, id, classes, playerColor, style, data = {} }) {
    const short_name = name ? name.replace(/[^A-Za-z]/g, '') : '';
    const currentTarget = data.tokens?.supplyTokens?.[key];
    style = currentTarget ? { visibility: 'hidden', pointerEvents: 'none' } : {};

    const handleMouseEnter = () => {
        if (!name) return;
        const zoomEl = document.getElementById('zoom');
        if (zoomEl) {

            zoomEl.style.backgroundImage = `url(images/${short_name}.png)`;
            zoomEl.classList.add('active');
            zoomEl.classList.add('zoom-token');
        }
    };

    const handleMouseLeave = () => {
        const zoomEl = document.getElementById('zoom');
        if (zoomEl) {
            zoomEl.classList.remove('active');
            zoomEl.classList.remove('zoom-token');
        }
    };

    const combinedStyle = {
        ...(style || {}),
        backgroundImage: name ? `url(images/${short_name}.png)` : null,
        borderColor: playerColor
    };

    return html`
        <div
            id=${id}
            class="${['token', ...(classes || [])].join(' ')}"
            style=${combinedStyle}
            onMouseEnter=${handleMouseEnter}
            onMouseLeave=${handleMouseLeave}
            onClick=${(e) => {
                if (e && e.stopPropagation) e.stopPropagation();

                if (name) sendMessage(`TOKEN:${name}`);
            }}        >
        </div>`;
}

function Player({ data, is_active, is_turn_player, instruction, choices, buttons, game_over, mode, state}) {
    const classes = ["player", is_active ? "active" : "", is_turn_player ? "turn" : ""].join(" ");
    const sortedHand = [...data.hand].sort();

    const { supplyTokens, playerTokens } = data.tokens || { supplyTokens: {}, playerTokens: {} };

    useEffect(() => {
        if (typeof updateAdventureTokens === 'function') {
            requestAnimationFrame(() => updateAdventureTokens(data));
        }
    }, [data]);


    if (!is_active) {
        return html`
                
            ${['card_reduction_token', 'estate_token', 'one_action_token',
                    'one_buy_token', 'one_card_token', 'one_money_token', 'trashing_token']
                        .filter(key => {
                            const isPlaced = data.tokens?.supplyTokens?.[key];
                            if (data.id !== state.active_player.id) return false;
                            return isPlaced;
                        })
                        .map(key => html`
        <${Token} 
            key=${`tavern-${key}-${data.id}`} 
            id="token-${key}-${data.id}" 
            name=${key} 
            playerColor=${data.color} 
            classes=${["adventure-token"]}
            data = ${data}
        />`)}
            
            <div class="${classes} opponent-hud" id="player-info-${data.id}">
                <div class="hud-header">
                    <div class="name">${data.name}</div>
                    <div class="stat-group resources">
                        <div class="money-wrapper">
                            <span class="coins" id="player-money-display-${data.id}">🟡 ${data.money}</span>
                            <div id="token-minus-coin-${data.id}" class="token square minus-coin" style="display:none"></div>
                        </div>
                        <span>⬡ ${data.debt}</span>
                        <span>⚗️ ${data.potion}</span>
                        <span>💰 ${data.coffre}</span>
                    </div>
                </div>

                <div class="data stats-container mini">
                    <div class="stat-group deck mini-deck">
                        <div class="card-pile" id="draw-pile-${data.id}">
                            <div id="token-minus-card-${data.id}" class="token square minus-card" style="display:none"></div>
                            <div class="pile-label">Draw: ${data.draw.length}</div>
                            <div class="cards-container">
                                ${Array(Math.min(data.draw.length, 5)).fill(0).map((_, i) => html`
                                    <img src="images/Cardback.png" class="card-back" style="bottom: ${i}px; left: ${i}px;" />
                                `)}
                            </div>
                        </div>

                        <div class="card-pile">
                            <div class="pile-label">Discard: ${data.discard.length}</div>
                            <div class="cards-container">
                                ${data.discard.length > 0
                                        ? html`<img src="images/Cardback.png" class="card-back" />`
                                        : html`<div class="empty-slot"></div>`}
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            
            
            
            <div class="opponent-deck">
                <div class="opponent-hand-preview">
                    <div class="hand-label">Opponent hand(${data.hand.length})</div>
                    <div class="hand-mini">
                        ${data.hand.map(card => html`
                        <${Card}
                                name=${card}
                                classes=${["mini-card", "dynamic-card"]}
                                messageType= "None"
                        />
                    `)}
                    </div>
                </div>

                <div class="opponent-inplay-preview">
                    <div class="hand-label"> Opponent In Play (${data.in_play.length})</div>
                    <div class="inplay-mini">
                        ${data.in_play.map(card => html`
                        <${Card} 
                                name=${card} 
                                classes=${["mini-card", "dynamic-card", "in-play-style"]} 
                                messageType= "None"
                        />
                    `)}
                    </div>
                </div>
                
                
            </div>
        `;
    }





    return html`
        <div id="interaction-container">
            ${instruction ? html`
                    <div class="instruction">
                        <div>${instruction}</div>
                        <div class="buttons">
                            ${buttons.map((button, index) => html`
                                <button key=${index} onClick=${() => sendMessage("BUTTON:" + button.value)}>
                                    ${button.label}
                                </button>
                            `)}
                            <button
                                    onClick=${() => sendMessage("")}
                                    disabled=${!choices.includes("")}
                            >
                                Pass
                            </button>
                        </div>
                    </div>
                ` : null}
        </div>
        
        ${data.tavern != null && html`
        <div id="tavern-area" style="box-shadow: inset 0 0 0 5px ${data.color}; border-bottom: 15px solid ${data.color};">
            <div class="side-label" style="color: #f1d299; font-variant: small-caps; font-weight: bold; margin-bottom: 8px; border-bottom: 1px solid #f1d299; width: 100%; text-align: center;">
                Tavern
            </div>
            <div class="tavern-cards-container">
                ${data.tavern && data.tavern.map(card => html`
                    <${Card} 
                        name=${card} 
                        classes=${["tavern-card"]} 
                        messageType="TAVERN" 
                    />
                `)}
            </div>
        </div>
    `}
        <div id="adventure-tokens-container-${data.id}" class="adventure-tokens-grid">
            ${['card_reduction_token', 'estate_token', 'one_action_token',
                'one_buy_token', 'one_card_token', 'one_money_token', 'trashing_token']
                    .filter(key => {
                        const supplyTokens = data.tokens?.supplyTokens || {};
                        const isPlaced = supplyTokens[key];
                        return !isPlaced;
                    })
                    .map(key => html`
                        <${Token}
                                key=${`tavern-${key}-${data.id}`}
                                id="token-${key}-${data.id}"
                                name=${key}
                                playerColor=${data.color}
                                classes=${["adventure-token"]}
                        />
                    `)}
        </div>
        <div class=${classes}>

            <div id="player-info-${data.id}" class="player_info">
                <div class="name">${data.name}</div>
            ${data.tokens != null && html`    
                <div class="journey-container">
                    <div id="token-journey-${data.id}"
                         class="token round"
                         data-side=${playerTokens["JourneyToken"] ? 'sun' : 'moon'} </div>
                </div>
                `}
                <div class="data stats-container">
                    <div class="stat-group resources">
                        <div class="money-wrapper">
                            <span class="coins" id="player-money-display-${data.id}">🟡 ${data.money}</span>
                            <div id="token-minus-coin-${data.id}" class="token square minus-coin" style="display:none"></div>
                        </div>
                        
                        <span class="${data.debt > 0 ? 'debt-active' : ''}"> ⬡ ${data.debt}</span>
                        <span class="potions">⚗️ ${data.potion}</span>
                        <span class="coffers">💰 ${data.coffre}</span>
                    </div>

                    <div class="stat-group mechanics">
                        <span>Actions: ${data.actions}</span>
                        <span>Buys: ${data.buys}</span>
                    </div>

                    <div class="stat-group deck">
                        <div class="card-pile" id="draw-pile-${data.id}">
                            <div id="token-minus-card-${data.id}" class="token square minus-card" style="display:none"></div>
                            <div class="pile-label">Draw: ${data.draw.length}</div>
                            <div class="cards-container">
                                ${Array(Math.min(data.draw.length, 5)).fill(0).map((_, i) => html`<img
                                        key=${`draw-${i}`}
                                        src="images/Cardback.png"
                                        class="card-back animate-card"
                                        style="bottom: ${i * 2}px; left: ${i * 2}px; transition-delay: ${i * 50}ms;"
                                />`)}
                            </div>
                        </div>
                        

                        <div class="card-pile" id="discard-pile">
                            <div class="pile-label">Discard: ${data.discard.length}</div>
                            <div class="cards-container">
                                ${data.discard.length > 0
                                        ? html`<img src="images/Cardback.png" class="card-back" />`
                                        : html`<div class="empty-slot"></div>`
                                }
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            ${!game_over && html`<${ListOfCards} classes=${["in_play"]} cards=${data.in_play} />`}

            <${ListOfCards}
                    classes=${!game_over && is_active ? "hand" : "hand-basic"}
                    cards=${sortedHand}
                    messageType=${is_active ? "HAND" : null}
            />
        </div>`;
}

function updateAdventureTokens(playerData) {
    if (!playerData.tokens) return;

    const pid = playerData.id;
    const playerColor = playerData.color;
    const pTokens = playerData.tokens.playerTokens;

    const taxCard = document.getElementById(`token-minus-card-${pid}`);
    if (taxCard) {
        if (pTokens["MinusOneCardToken"] || pTokens["Tax Card Token"]) {
            const anchor = document.getElementById(`draw-pile-${pid}`);
            moveTokenTo(taxCard, anchor, playerColor);
        } else {
            taxCard.style.display = "none";
        }
    }

    // Idem pour la pièce
    const taxCoin = document.getElementById(`token-minus-coin-${pid}`);
    if (taxCoin) {
        if (pTokens["MinusOneCoinToken"] || pTokens["Tax token"]) {
            const anchor = document.getElementById(`player-money-display-${pid}`);
            moveTokenTo(taxCoin, anchor, playerColor);
        } else {
            taxCoin.style.display = "none";
        }
    }

    const journeyEl = document.getElementById(`token-journey-${pid}`);
    if (journeyEl && pTokens["JourneyToken"] !== undefined) {
        journeyEl.dataset.side = pTokens["JourneyToken"] ? 'sun' : 'moon';
        journeyEl.style.borderColor = playerColor;
        journeyEl.style.display = "block";
    }
}



function moveTokenTo(el, target, color) {
    if (!el || !target) return;

    el.style.position = "fixed";
    el.style.zIndex = "10000";
    el.style.display = "block";

    const targetRect = target.getBoundingClientRect();

    const x = targetRect.left + (targetRect.width / 2) - 12;
    const y = targetRect.top + (targetRect.height / 2) - 12;

    el.style.left = `${x}px`;
    el.style.top = `${y}px`;
    el.style.borderColor = color;
}

function SelectionOverlay({ instruction, cards, choices, buttons, onSelect, mode}) {
    if (mode === false) return null;

    return html`
        <div class="selection-overlay">
            <div class="selection-window">
                <div class="selection-instruction">${instruction}</div>

                <div class="selection-grid">
                    ${(cards || []).map((fullChoice) => {
                        const cardName = fullChoice.split(":")[2];
                        const isSelectable = choices.includes(fullChoice);

                        return html`
                            <div
                                    class="selection-item ${isSelectable ? 'selectable' : 'not-selectable'}"
                                    onClick=${() => isSelectable ? onSelect(fullChoice) : null}
                            >
                                <${Card} name=${cardName} />
                            </div>
                        `;
                    })}
                </div>
                
                <div class="buttons">
                    ${buttons.map((button, index) => html`<button key=${index} onClick=${() => sendMessage("BUTTON:" + button.value)}>${button.label}</button>`)}
                    <button onClick=${() => sendMessage("")} disabled=${!choices.includes("")}>Pass</button>
                </div>
            </div>
        </div>`;
}


function ListOfCards({ cards, classes, messageType }) {
    // 1. On s'assure que cards est un tableau et on filtre les valeurs nulles
    const validCards = Array.isArray(cards) ? cards.filter(c => c !== null) : [];

    if (validCards.length === 0) {
        // Sécurité sur classes au cas où il serait undefined
        const emptyClass = Array.isArray(classes) ? classes.join(" ") : (classes || "");
        return html`<div class=${emptyClass}></div>`;
    }

    const classList = Array.isArray(classes) ? classes : [classes];
    const total = validCards.length;
    const isHand = classList.includes("hand") || classList.includes("hand-basic");

    return html`
        <div class=${classList.join(" ")} style="--total: ${total}">
            ${validCards.map((card, i) => {
                const nextCard = validCards[i + 1];
                const isDuplicate = nextCard && card === nextCard;

                return html`
                    <${Card}
                            key=${i}
                            name=${card}
                            classes=${isDuplicate ? ['duplicate'] : []}
                            messageType=${messageType}
                            style=${isHand ? { "--index": i } : {}}
                    />`;
            })}
        </div>`;
}

function MessagePrompt() {
    return html`<div id="message">
        <input id="message-input" type="text" placeholder="message" onKeyDown=${event => {
            if (event.key === "Enter") {
                socket.send(event.target.value);
                console.log(`Message envoyé: "${event.target.value}"`);
                event.target.value = "";
            }
        }}/>
    </div>`;
}

function Log({ log }) {
    const ref = useRef(null);
    const [scrollLog, setScrollLog] = useState(true);
    const [isOpen, setIsOpen] = useState(true);

    useEffect(() => {
        if (ref.current && scrollLog && isOpen) {
            ref.current.scrollTop = ref.current.scrollHeight;
        }
    }, [log, isOpen]);

    const handleScroll = () => {
        if (ref.current) {
            const { scrollTop, clientHeight, scrollHeight } = ref.current;
            setScrollLog(scrollTop + clientHeight >= scrollHeight - 10);
        }
    };

    const logText = log.map(line =>
        line.replace(/^ +/g, match => '&nbsp;'.repeat(match.length))
    ).join('<br>');

    return html`
        <div class="log-floating-container ${isOpen ? 'open' : 'closed'}">
            <div class="log-header" onClick=${() => setIsOpen(!isOpen)}>
                <span>Journal de bord</span>
                
                <button class="toggle-btn">${isOpen ? '−' : '+'}</button>
            </div>
            
            <div
                    id="log"
                    ref=${ref}
                    onScroll=${handleScroll}
                    dangerouslySetInnerHTML=${{ __html: logText || "" }}
            /><${MessagePrompt} />
        </div>
    `;
}
window.addEventListener("load", () => {
    document.addEventListener("keypress", event => {
        if (event.key === " ") {
            sendMessage("");
        }
    });
})



render(html`<${Main} />`, document.getElementById('root'));
