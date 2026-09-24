import React, { useState, useEffect, useRef } from 'react';
import './css/OpponentActionBanner.css';
import { useGame } from "../../context/GameContext.tsx";

function OpponentActionBanner({ playerId, opponentId }) {
    const { state } = useGame();
    const event = state.logChange;

    const [currentEvent, setCurrentEvent] = useState(null);
    const queueRef = useRef([]);
    const isProcessingRef = useRef(false);
    const processedIdsRef = useRef(new Set());
    const timeoutRef = useRef(null);

    const mountTimestampRef = useRef(Date.now());

    const processQueue = () => {
        if (isProcessingRef.current || queueRef.current.length === 0) return;

        isProcessingRef.current = true;
        const nextEvent = queueRef.current.shift();

        setCurrentEvent(nextEvent);

        if (timeoutRef.current) clearTimeout(timeoutRef.current);

        timeoutRef.current = setTimeout(() => {
            setCurrentEvent(null);

            timeoutRef.current = setTimeout(() => {
                isProcessingRef.current = false;
                processQueueRef.current();
            }, 200);

        }, 2500);
    };

    const processQueueRef = useRef(processQueue);
    useEffect(() => {
        processQueueRef.current = processQueue;
    });

    const getEventKey = (evt) => {
        if (!evt) return null;
        if (evt.id) return evt.id;
        return `${evt.playerId || 'unknown'}-${evt.timestamp || ''}-${evt.text || ''}`;
    };

    useEffect(() => {
        if (!event) return;
        const events = Array.isArray(event) ? event : [event];

        const newEventsForThisOpponent = [];

        events.forEach(evt => {
            if (!evt) return;

            // 💡 FIX 1 : Si l'événement s'est produit AVANT le montage du composant (F5), on l'ignore !
            if (evt.timestamp && evt.timestamp < mountTimestampRef.current) {
                const key = getEventKey(evt);
                if (key) processedIdsRef.current.add(key); // Marquer comme vu sans l'afficher
                return;
            }

            const evtKey = getEventKey(evt);
            if (!evtKey || processedIdsRef.current.has(evtKey)) {
                return;
            }

            processedIdsRef.current.add(evtKey);

            const targetId = evt.playerId;
            const isThisOpponent = targetId && (
                String(targetId) === String(playerId) ||
                String(targetId) === String(opponentId)
            );

            if (isThisOpponent) {
                evt._uniqueKey = evtKey;
                newEventsForThisOpponent.push(evt);
            }
        });

        if (newEventsForThisOpponent.length > 0) {
            queueRef.current.push(...newEventsForThisOpponent);
            processQueueRef.current();
        }
    }, [event, playerId, opponentId]);

    useEffect(() => {
        return () => {
            if (timeoutRef.current) clearTimeout(timeoutRef.current);
        };
    }, []);

    if (!currentEvent) return null;

    return (
        <div key={currentEvent._uniqueKey} className="opponent-action-banner">
            <span className="banner-text">{currentEvent.text}</span>
        </div>
    );
}

export default OpponentActionBanner;