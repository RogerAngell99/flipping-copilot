package com.flippingcopilot.ui;

import com.flippingcopilot.model.SuggestionManager;
import com.flippingcopilot.model.SuggestionPreferencesManager;
import net.runelite.client.ui.ColorScheme;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import java.awt.*;

@Singleton
public class ControlPanel extends JPanel {
    private final SuggestionPreferencesManager preferencesManager;
    private final JPanel timeframePanel;
    private final JSlider timeframeSlider;
    private final JLabel timeframeValueLabel;
    private final JToggleButton sellFastButton;
    private int previousTimeframe;

    @Inject
    public ControlPanel(
            SuggestionManager suggestionManager,
            SuggestionPreferencesManager preferencesManager) {
        this.preferencesManager = preferencesManager;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        setBounds(0, 0, 300, 150);

        // Add timeframe slider
        timeframePanel = new JPanel();
        timeframePanel.setLayout(new BoxLayout(timeframePanel, BoxLayout.Y_AXIS));
        timeframePanel.setOpaque(false);
        JLabel timeframeLabel = new JLabel("How often do you adjust offers?");
        timeframeLabel.setHorizontalAlignment(SwingConstants.LEFT);
        timeframeLabel.setMaximumSize(timeframeLabel.getPreferredSize());
        JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        labelPanel.setOpaque(false);
        labelPanel.add(timeframeLabel);

        timeframeSlider = new JSlider(1, 480, preferencesManager.getTimeframe());
        timeframeSlider.setOpaque(false);
        timeframeSlider.addChangeListener(e -> {
            int value = timeframeSlider.getValue();
            preferencesManager.setTimeframe(value);
            updateTimeframeLabel(value);
            if (!timeframeSlider.getValueIsAdjusting()) {
                suggestionManager.setSuggestionNeeded(true);
            }
        });

        sellFastButton = new JToggleButton("Sell Fast");
        sellFastButton.addActionListener(e -> {
            if (sellFastButton.isSelected()) {
                previousTimeframe = timeframeSlider.getValue();
                timeframeSlider.setValue(1);
                sellFastButton.setBackground(ColorScheme.BRAND_ORANGE);
            } else {
                timeframeSlider.setValue(previousTimeframe);
                sellFastButton.setBackground(null);
            }
        });

        JPanel sellFastButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        sellFastButtonPanel.setOpaque(false);
        sellFastButtonPanel.add(sellFastButton);

        timeframeValueLabel = new JLabel();
        updateTimeframeLabel(timeframeSlider.getValue());

        JPanel sliderPanel = new JPanel(new BorderLayout());
        sliderPanel.setOpaque(false);
        sliderPanel.add(createIncrementButton("-", -1, suggestionManager), BorderLayout.WEST);
        sliderPanel.add(timeframeSlider, BorderLayout.CENTER);
        sliderPanel.add(createIncrementButton("+", 1, suggestionManager), BorderLayout.EAST);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);

        timeframeValueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        sellFastButtonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        infoPanel.add(timeframeValueLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        infoPanel.add(sellFastButtonPanel);

        timeframePanel.add(labelPanel);
        timeframePanel.add(Box.createRigidArea(new Dimension(0, 3)));
        timeframePanel.add(sliderPanel);
        timeframePanel.add(Box.createRigidArea(new Dimension(0, 3)));
        timeframePanel.add(infoPanel);
        add(timeframePanel);
    }

    private JButton createIncrementButton(String text, int adjustment, SuggestionManager suggestionManager) {
        JButton button = new JButton(text);
        button.addActionListener(e -> {
            int newValue = timeframeSlider.getValue() + adjustment;
            if (newValue >= 1 && newValue <= 480) {
                timeframeSlider.setValue(newValue);
                preferencesManager.setTimeframe(newValue);
                suggestionManager.setSuggestionNeeded(true);
            }
        });
        return button;
    }

    private void updateTimeframeLabel(int minutes) {
        if (sellFastButton.isSelected() && minutes != 1) {
            sellFastButton.setSelected(false);
            sellFastButton.setBackground(null);
        }
        int hours = minutes / 60;
        int mins = minutes % 60;
        String label = "";
        if (hours > 0) {
            label += hours + "h ";
        }
        if (mins > 0 || hours == 0) {
            label += mins + "m";
        }
        timeframeValueLabel.setText(label.trim());
    }

    public void refresh() {
        if(!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refresh);
            return;
        }

        if (sellFastButton.isSelected()) {
            sellFastButton.setSelected(false);
            sellFastButton.setBackground(null);
        }
        int tf = preferencesManager.getTimeframe();
        timeframeSlider.setValue(tf);
        updateTimeframeLabel(tf);
    }
} 