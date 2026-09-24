import React, { useState, useEffect, useRef } from 'react'
import { AdventureTokensList } from './PlayerSubviews.jsx'
import './css/InventoryModal.css'
import {useGame} from "../../context/GameContext";

export interface InventoryData {
    id: string | number
    tokens?: Record<string, any> | any[] | null
    color : string
}

type TabId = 'tokens' | 'tavern'

interface InventoryModalProps {
    data: InventoryData
}

export const InventoryModal: React.FC<InventoryModalProps> = ({ data }) => {
    const [isOpen, setIsOpen] = useState<boolean>(false)
    const{gameState} = useGame()

    const initialTab: TabId = data.tokens != null ? 'tokens' : 'tokens'
    const [activeTab, setActiveTab] = useState<TabId>(initialTab)

    const isOpenRef = useRef(isOpen)
    useEffect(() => {
        isOpenRef.current = isOpen
    }, [isOpen])

    useEffect(() => {
        if (activeTab === 'tokens' && data.tokens == null) {
            setActiveTab('tavern')
        }
    }, [data.tokens, activeTab])

    useEffect(() => {
        const handleKeyDown = (event: KeyboardEvent) => {
            const target = event.target as HTMLElement
            if (['INPUT', 'TEXTAREA'].includes(target.tagName) || target.isContentEditable) return

            if (event.key === 'i' || event.key === 'I') {
                event.preventDefault()
                event.stopPropagation()
                event.stopImmediatePropagation()
                setIsOpen((prev) => !prev)
                return
            }

            if (event.key === 'Escape' && isOpenRef.current) {
                event.preventDefault()
                event.stopPropagation()
                event.stopImmediatePropagation()
                setIsOpen(false)
            }
        }

        window.addEventListener('keydown', handleKeyDown, true)
        return () => window.removeEventListener('keydown', handleKeyDown, true)
    }, [])

    if (!isOpen) return null

    return (
        <div className="inventory-overlay" onClick={() => setIsOpen(false)}>
            <div className="inventory-container" onClick={(e) => e.stopPropagation()}>

                <div className="inventory-top-extension">
                    {data.tokens != null && (
                        <button
                            className={`inventory-ext-btn ${activeTab === 'tokens' ? 'active' : ''}`}
                            onClick={() => setActiveTab('tokens')}
                        >
                            Jetons
                        </button>
                    )}
                </div>

                <div className="inventory-modal">
                    <div className="inventory-header">
                        <h2 className="inventory-title">Inventory</h2>
                        <button onClick={() => setIsOpen(false)} className="inventory-close-btn">
                            ESCAPE ✕
                        </button>
                    </div>

                    <div className="inventory-body">
                        {activeTab === 'tokens' && data.tokens != null && (
                            <div id={`adventure-tokens-container-${data.id}`} className="adventure-tokens-grid">
                                <AdventureTokensList data={data} isOpponent={false} state={gameState} />
                            </div>
                        )}
                    </div>

                    <div className="inventory-footer">
                        <div className="flex gap-2">
                            <span><kbd className="inventory-kbd">I</kbd> Return </span>
                            <span><kbd className="inventory-kbd">Escape</kbd> Close</span>
                        </div>
                    </div>
                </div>

            </div>
        </div>
    )
}