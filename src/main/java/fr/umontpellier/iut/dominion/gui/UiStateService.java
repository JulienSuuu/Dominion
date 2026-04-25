package fr.umontpellier.iut.dominion.gui;

import org.springframework.stereotype.Component;

@Component
public class UiStateService {
    private boolean promptActive = false;

    public boolean isPromptActive() {
        return promptActive;
    }

    public void setPromptActive(boolean active) {
        this.promptActive = active;
    }
}
