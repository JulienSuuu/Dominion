import React, { useState, useEffect, useRef } from 'react';
import './css/AnimatedCounter.css';
import { useGame } from "../../context/GameContext.tsx";

export const AnimatedCounter = ({ value, deltaInfo, mini = false}) => {
    const [displayValue, setDisplayValue] = useState(value);
    const [badge, setBadge] = useState(null);
    const { handleRemoveItemChange } = useGame();

    const accumulatedDeltaRef = useRef(0);
    const badgeTimerRef = useRef(null);
    const lastProcessedIdRef = useRef(null);

    const pendingEventsRef = useRef([]);

    useEffect(() => {
        if (displayValue === value) return;

        const diff = Math.abs(value - displayValue);
        const speed = Math.max(20, Math.floor(150 / diff));

        const step = value > displayValue ? 1 : -1;
        const timer = setInterval(() => {
            setDisplayValue(prev => {
                const next = prev + step;
                if (next === value) clearInterval(timer);
                return next;
            });
        }, speed);

        return () => clearInterval(timer);
    }, [value, displayValue]);

    useEffect(() => {
        if (!deltaInfo || !deltaInfo.id || deltaInfo.id === lastProcessedIdRef.current) {
            return;
        }

        lastProcessedIdRef.current = deltaInfo.id;

        pendingEventsRef.current.push(deltaInfo);

        if (badgeTimerRef.current) {
            clearTimeout(badgeTimerRef.current);
        }

        const newDelta = deltaInfo.delta;
        const prevTotal = accumulatedDeltaRef.current;

        if ((prevTotal > 0 && newDelta < 0) || (prevTotal < 0 && newDelta > 0)) {
            accumulatedDeltaRef.current = newDelta;
        } else {
            accumulatedDeltaRef.current += newDelta;
        }

        const currentTotal = accumulatedDeltaRef.current;

        let badgeType = 'neutral';
        let formattedText = '0';

        if (currentTotal > 0) {
            badgeType = 'positive';
            formattedText = `+${currentTotal}`;
        } else if (currentTotal < 0) {
            badgeType = 'negative';
            formattedText = `${currentTotal}`;
        }

        setBadge({
            text: formattedText,
            type: badgeType,
            key: deltaInfo.id
        });

        badgeTimerRef.current = setTimeout(() => {
            setBadge(null);
            accumulatedDeltaRef.current = 0;

            if (handleRemoveItemChange && pendingEventsRef.current.length > 0) {
                pendingEventsRef.current.forEach(evt => {
                    handleRemoveItemChange(evt.rawInfo);
                });
                pendingEventsRef.current = [];
            }
        }, 1500);

    }, [deltaInfo, handleRemoveItemChange]);

    useEffect(() => {
        return () => {
            if (badgeTimerRef.current) clearTimeout(badgeTimerRef.current);
        };
    }, []);

    const isMini = mini === true ? "mini" : ""

    return (
        <span className="counter-container">
            <span className="main-value">{displayValue}</span>
            {badge && (
                <span key={badge.key} className={`delta-badge ${badge.type} ${isMini}`}>
                    {!isMini ? `(${badge.text})` : badge.text}
                </span>
            )}
        </span>
    );
};