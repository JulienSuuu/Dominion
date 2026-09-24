import React, {useEffect, useState} from "react";

export function useModal() {
    const [showModal, setShowModal] = useState(false);

    const closeModal = () => {
        setShowModal(false);
    };

    const handleContextMenu = (e : React.MouseEvent<HTMLDivElement>) => {
        if (e) e.stopPropagation();
        e.preventDefault();
        setShowModal(true);
    };

    useEffect(() => {
        if (!showModal) return;

        const handleEscape = (e : KeyboardEvent) => {
            if (e.key === 'Escape') {
                e.preventDefault();
                e.stopPropagation();
                e.stopImmediatePropagation();

                if (e.type === 'keydown') {
                    closeModal();
                }
            }
        };

        window.addEventListener('keydown', handleEscape, true);
        window.addEventListener('keyup', handleEscape, true);

        return () => {
            window.removeEventListener('keydown', handleEscape, true);
            window.removeEventListener('keyup', handleEscape, true);
        };
    }, [showModal]);

    return {showModal, closeModal, handleContextMenu};
}