import React, { useState, useEffect, useRef } from 'react';

function MessagePrompt({ socket }) {
    const handleKeyDown = (event) => {
        if (event.key === "Enter" && event.target.value.trim() !== "") {
            if (socket && socket.readyState === WebSocket.OPEN) {
                socket.send(JSON.stringify({
                    action: "Chat",
                    message: event.target.value
                    }
                ));
                console.log(`Message envoyé: "${event.target.value}"`);
                event.target.value = "";
            } else {
                console.warn("Le WebSocket n'est pas connecté.");
            }
        }
    };

    return (
        <div id="message">
            <input
                id="message-input"
                type="text"
                placeholder="Discuter / message..."
                onKeyDown={handleKeyDown}
            />
        </div>
    );
}

function Log({ log = [], socket, sendMessage }) {
    const ref = useRef(null);
    const [scrollLog, setScrollLog] = useState(true);
    const [isOpen, setIsOpen] = useState(true);

    // Auto-scroll vers le bas lorsque le log change
    useEffect(() => {
        if (ref.current && scrollLog && isOpen) {
            ref.current.scrollTop = ref.current.scrollHeight;
        }
    }, [log, isOpen]);

    useEffect(() => {
        const handleKeyPress = (event) => {
            if (document.activeElement.tagName === 'INPUT' || document.activeElement.tagName === 'TEXTAREA') {
                return;
            }

            if (event.key === " " || event.key === "Spacebar") {
                event.preventDefault();
                if (typeof sendMessage === 'function') {
                    sendMessage("");
                }
            }
        };

        window.addEventListener("keypress", handleKeyPress);
        return () => window.removeEventListener("keypress", handleKeyPress);
    }, [sendMessage]);

    const handleScroll = () => {
        if (ref.current) {
            const { scrollTop, clientHeight, scrollHeight } = ref.current;
            setScrollLog(scrollTop + clientHeight >= scrollHeight - 10);
        }
    };

    return (
        <div className={`log-floating-container ${isOpen ? 'open' : 'closed'}`}>
            <div className="log-header" onClick={() => setIsOpen(!isOpen)}>
                <span>Journal de bord</span>
                <button className="toggle-btn">{isOpen ? '−' : '+'}</button>
            </div>

            {isOpen && (
                <>
                    <div id="log" ref={ref} onScroll={handleScroll}>
                        {log.map((item, index) => {
                            const text = typeof item === 'string' ? item : item.text;
                            const type = typeof item === 'object' && item.type ? item.type.toLowerCase() : 'info';
                            const playerName = item.playerName ? item.playerName.toLowerCase() : '';
                            // Génère une clé unique
                            const key = item.id || item.timestamp || index;

                            return (
                                <div key={key} className={`log-entry log-${type}`}>
                                    <span className="log-text">{playerName} {text}</span>
                                </div>
                            );
                        })}
                    </div>
                    <MessagePrompt socket={socket} />
                </>
            )}
        </div>
    );
}

export default Log;