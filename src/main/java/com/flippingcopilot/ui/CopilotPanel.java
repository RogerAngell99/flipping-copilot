package com.flippingcopilot.ui;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import java.awt.*;

@Singleton
public class CopilotPanel extends JPanel {

    public final SuggestionPanel suggestionPanel;
    public final StatsPanelV2 statsPanel;
    public final ControlPanel controlPanel;

    @Inject
    public CopilotPanel(SuggestionPanel suggestionPanel,
                        StatsPanelV2 statsPanel,
                        ControlPanel controlPanel) {
        this.statsPanel = statsPanel;
        this.suggestionPanel = suggestionPanel;
        this.controlPanel = controlPanel;

        // Mantendo o BoxLayout original, que é o correto para este tipo de empilhamento vertical.
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // Adicionando os painéis sem nenhuma restrição de tamanho,
        // para que eles possam se ajustar livremente.
        add(suggestionPanel);
        add(Box.createRigidArea(new Dimension(0, 5)));
        add(controlPanel);
        add(Box.createRigidArea(new Dimension(0, 5)));
        add(Box.createVerticalGlue());
        add(statsPanel);
    }

    public void refresh() {
        if(!SwingUtilities.isEventDispatchThread()) {
            // we always execute this in the Swing EDT thread
            SwingUtilities.invokeLater(this::refresh);
            return;
        }
        suggestionPanel.refresh();
        controlPanel.refresh();

        // ADICIONADO: Força o painel a revalidar seu layout e se redesenhar.
        // Isso garante que ele se ajuste ao novo tamanho do SuggestionPanel após o login.
        revalidate();
        repaint();
    }
}