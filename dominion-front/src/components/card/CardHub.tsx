import React, {useEffect, useState} from 'react';
import {useModal} from "./useModal";
import {createPortal} from "react-dom";
import {renderPrice} from "../../interfaces/Card";
import {getCardImageUrl} from "../../PreLoader";

interface CardHubProps {
    name : string;
    isSelected : boolean;
    onClick : () => void;
    classes : React.CSSProperties[]
}



function CardHub({ name, isSelected, onClick, classes = [] } : CardHubProps) {
    const short_name = name ? name.replace(/[^A-Za-z]/g, '') : '';
    const {showModal, closeModal, handleContextMenu} = useModal();

    const cardStyle = name ? { backgroundImage: `url(../src/assets/cards/${short_name}.jpg)` } : {};

    return (
        <>
            <div
                className={`card ${isSelected ? 'selected' : ''} ${(classes || []).join(' ')}`}
                style={cardStyle}
                onClick={onClick}
                onContextMenu={handleContextMenu}
            >
                {isSelected && (
                    <div className="selected-check">
                        <span>✓</span>
                    </div>
                )}
            </div>


            {showModal && createPortal(
                <div className="card-modal-overlay" onClick={closeModal}>
                    <div className="card-modal-content" onClick={(e) => e.stopPropagation()}>
                        <button className="card-modal-close" onClick={closeModal}>
                            &times;
                        </button>

                        <div className="card-modal-body">
                            <h3>{name}</h3>

                            <img
                                src={`../src/assets/cards/${short_name}.jpg`}
                                alt={name}
                                className="card-modal-image"
                            />
                        </div>
                    </div>
                </div>,
                document.body
            )}
        </>
    );
}

export default CardHub;