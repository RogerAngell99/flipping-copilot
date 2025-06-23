package com.flippingcopilot.ui;

import com.flippingcopilot.controller.FlippingCopilotConfig;
import com.flippingcopilot.controller.GrandExchange;
import com.flippingcopilot.controller.HighlightController;
import com.flippingcopilot.controller.PremiumInstanceController;
import com.flippingcopilot.model.*;
import com.flippingcopilot.ui.graph.PriceGraphController;
import com.flippingcopilot.util.GeTax;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;

import static com.flippingcopilot.ui.UIUtilities.*;
import static com.flippingcopilot.util.Constants.MIN_GP_NEEDED_TO_FLIP;

@Singleton
@Slf4j
public class SuggestionPanel extends JPanel {

    // dependencies
    private final FlippingCopilotConfig config;
    private final SuggestionManager suggestionManager;
    private final AccountStatusManager accountStatusManager;
    public final PauseButton pauseButton;
    private final BlockButton blockButton;
    private final OsrsLoginManager osrsLoginManager;
    private final Client client;
    private final PausedManager pausedManager;
    private final GrandExchangeUncollectedManager uncollectedManager;
    private final ClientThread clientThread;
    private final HighlightController highlightController;
    private final ItemManager itemManager;
    private final GrandExchange grandExchange;
    private final PriceGraphController priceGraphController;
    private final PremiumInstanceController premiumInstanceController;
    private final TimeframeSuggestionCache timeframeSuggestionCache;
    private final SuggestionPreferencesManager suggestionPreferencesManager;

    private static final String SUGGESTION_VIEW = "SUGGESTION_VIEW";
    private static final String PREFERENCES_VIEW = "PREFERENCES_VIEW";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel containerPanel = new JPanel(cardLayout);

    private final JLabel suggestionText = new JLabel();
    private final JLabel suggestionIcon = new JLabel();
    private final JPanel suggestionTextContainer = new JPanel();
    public final Spinner spinner = new Spinner();
    private JLabel skipButton;
    private final JPanel buttonContainer = new JPanel();
    private JLabel graphButton;
    private final PreferencesPanel preferencesPanel;
    private String innerSuggestionMessage;
    private String highlightedColor = "yellow";

    @Setter
    private String serverMessage = "";

    @Inject
    public SuggestionPanel(FlippingCopilotConfig config,
                           SuggestionManager suggestionManager,
                           AccountStatusManager accountStatusManager,
                           PauseButton pauseButton,
                           BlockButton blockButton,
                           PreferencesPanel preferencesPanel,
                           OsrsLoginManager osrsLoginManager,
                           Client client, PausedManager pausedManager,
                           GrandExchangeUncollectedManager uncollectedManager,
                           ClientThread clientThread,
                           HighlightController highlightController,
                           ItemManager itemManager,
                           GrandExchange grandExchange, PriceGraphController priceGraphController, PremiumInstanceController premiumInstanceController, TimeframeSuggestionCache timeframeSuggestionCache, SuggestionPreferencesManager suggestionPreferencesManager) {
        this.config = config;
        this.suggestionManager = suggestionManager;
        this.accountStatusManager = accountStatusManager;
        this.pauseButton = pauseButton;
        this.blockButton = blockButton;
        this.preferencesPanel = preferencesPanel;
        this.osrsLoginManager = osrsLoginManager;
        this.client = client;
        this.pausedManager = pausedManager;
        this.uncollectedManager = uncollectedManager;
        this.clientThread = clientThread;
        this.highlightController = highlightController;
        this.itemManager = itemManager;
        this.grandExchange = grandExchange;
        this.priceGraphController = priceGraphController;
        this.premiumInstanceController = premiumInstanceController;
        this.timeframeSuggestionCache = timeframeSuggestionCache;
        this.suggestionPreferencesManager = suggestionPreferencesManager;

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARKER_GRAY_COLOR);

        // Painel principal que conterá ou a sugestão ou as preferências
        containerPanel.add(createSuggestionActionPanel(), SUGGESTION_VIEW);
        containerPanel.add(this.preferencesPanel, PREFERENCES_VIEW);

        add(containerPanel, BorderLayout.CENTER);

        cardLayout.show(containerPanel, SUGGESTION_VIEW);
    }

    private JPanel createSuggestionActionPanel() {
        JPanel suggestedActionPanel = new JPanel(new BorderLayout());
        suggestedActionPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        suggestedActionPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        // Painel do título com o botão de engrenagem
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        JLabel title = new JLabel("Suggested Action:");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        titlePanel.add(title, BorderLayout.CENTER);

        BufferedImage gearIconImg = ImageUtil.loadImageResource(getClass(), "/preferences-icon.png");
        JLabel gearButton = buildButton(ImageUtil.resizeImage(gearIconImg, 16, 16), "Settings", this::handleGearClick);
        titlePanel.add(gearButton, BorderLayout.WEST);

        suggestedActionPanel.add(titlePanel, BorderLayout.NORTH);

        JPanel suggestionContainer = new JPanel(new CardLayout());
        suggestionContainer.setOpaque(true);
        suggestionContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        suggestedActionPanel.add(suggestionContainer, BorderLayout.CENTER);

        suggestionTextContainer.setLayout(new BoxLayout(suggestionTextContainer, BoxLayout.X_AXIS));
        suggestionTextContainer.add(Box.createHorizontalGlue());
        suggestionTextContainer.add(suggestionIcon);
        suggestionTextContainer.add(suggestionText);
        suggestionTextContainer.add(Box.createHorizontalGlue());
        suggestionTextContainer.setOpaque(true);
        suggestionTextContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        suggestionIcon.setVisible(false);
        suggestionIcon.setOpaque(true);
        suggestionIcon.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        suggestionIcon.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));

        suggestionText.setHorizontalAlignment(SwingConstants.CENTER);
        suggestionContainer.add(suggestionTextContainer, "text");

        suggestionContainer.add(spinner, "spinner");
        setupButtonContainer();
        suggestedActionPanel.add(buttonContainer, BorderLayout.SOUTH);

        return suggestedActionPanel;
    }

    private void handleGearClick() {
        preferencesPanel.refresh();
        cardLayout.show(containerPanel, PREFERENCES_VIEW);
    }

    // Método para voltar para a tela de sugestão (pode ser chamado de dentro do painel de preferências)
    public void showSuggestionView() {
        cardLayout.show(containerPanel, SUGGESTION_VIEW);
    }

    private void setupButtonContainer() {
        buttonContainer.setLayout(new BorderLayout());
        buttonContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        buttonContainer.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        JPanel centerPanel = new JPanel(new GridLayout(1, 5, 15, 0));
        centerPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        BufferedImage graphIcon = ImageUtil.loadImageResource(getClass(), "/graph.png");
        graphButton = buildButton(graphIcon, "Price graph", () -> {
            Suggestion suggestion = suggestionManager.getSuggestion();
            if (suggestion == null || suggestion.getName() == null) return;
            if (config.priceGraphWebsite().equals(FlippingCopilotConfig.PriceGraphWebsite.FLIPPING_COPILOT)) {
                priceGraphController.showPriceGraph(suggestion.getName(), true);
            } else {
                String url = config.priceGraphWebsite().getUrl(suggestion.getName(), suggestion.getItemId());
                LinkBrowser.browse(url);
            }
        });
        centerPanel.add(graphButton);

        centerPanel.add(new JPanel(){{setOpaque(false);}}); // Espaçador
        centerPanel.add(pauseButton);
        centerPanel.add(blockButton);

        BufferedImage skipIcon = ImageUtil.loadImageResource(getClass(), "/skip.png");
        skipButton = buildButton(skipIcon, "Skip suggestion", () -> {
            showLoading();
            Suggestion s = suggestionManager.getSuggestion();
            accountStatusManager.setSkipSuggestion(s != null ? s.getId() : -1);
            suggestionManager.setSuggestionNeeded(true);
        });
        centerPanel.add(skipButton);

        buttonContainer.add(centerPanel, BorderLayout.CENTER);
    }

    private void setItemIcon(int itemId) {
        AsyncBufferedImage image = itemManager.getImage(itemId);
        if (image != null) {
            suggestionIcon.setIcon(new ImageIcon(image));
            suggestionIcon.setVisible(true);
        }
    }

    public void updateSuggestion(Suggestion suggestion) {
        ((CardLayout)suggestionText.getParent().getParent().getLayout()).show(suggestionText.getParent().getParent(), "text");
        NumberFormat formatter = NumberFormat.getNumberInstance();
        String suggestionString = "<html><center>";
        suggestionTextContainer.setVisible(false);

        switch (suggestion.getType()) {
            case "wait":
                suggestionString += "Wait <br>";
                suggestionIcon.setVisible(false);
                break;
            case "abort":
                suggestionString += "Abort offer for<br><FONT COLOR=white>" + suggestion.getName() + "<br></FONT>";
                setItemIcon(suggestion.getItemId());
                break;
            case "buy":
            case "sell":
                String capitalisedAction = suggestion.getType().equals("buy") ? "Buy" : "Sell";
                suggestionString += capitalisedAction +
                        " <FONT COLOR=" + highlightedColor + ">" + formatter.format(suggestion.getQuantity()) + "</FONT><br>" +
                        "<FONT COLOR=white>" + suggestion.getName() + "</FONT><br>" +
                        "for <FONT COLOR=" + highlightedColor + ">" + formatter.format(suggestion.getPrice()) + "</FONT> gp";

                if ("buy".equals(suggestion.getType())) {
                    long totalBuyPrice = (long) suggestion.getQuantity() * suggestion.getPrice();
                    suggestionString += " (" + formatter.format(totalBuyPrice) + " gp total)";
                } else { // "sell"
                    int postTaxPricePerItem = GeTax.getPostTaxPrice(suggestion.getItemId(), suggestion.getPrice());
                    long totalSellPrice = (long) suggestion.getQuantity() * postTaxPricePerItem;
                    suggestionString += " (" + formatter.format(totalSellPrice) + " gp total after tax)";
                    suggestionString += addDeltaComparison(suggestion);
                }
                suggestionString += "<br>";
                setItemIcon(suggestion.getItemId());
                break;
            default:
                suggestionString += "Error processing suggestion<br>";
                suggestionIcon.setVisible(false);
        }
        suggestionString += suggestion.getMessage();
        suggestionString += "</center></html>";
        innerSuggestionMessage = "";
        if (!suggestion.getType().equals("wait")) {
            setButtonsVisible(true);
        }
        suggestionText.setText(suggestionString);
        suggestionTextContainer.setVisible(true);
    }

    private String addDeltaComparison(Suggestion currentSuggestion) {
        StringBuilder deltaString = new StringBuilder();
        int currentTf = suggestionPreferencesManager.getTimeframe();
        List<Integer> allTimeframes = Arrays.asList(5, 30, 120, 480);

        long currentTotalSell = (long) currentSuggestion.getQuantity() * GeTax.getPostTaxPrice(currentSuggestion.getItemId(), currentSuggestion.getPrice());

        for (int otherTf : allTimeframes) {
            if (otherTf == currentTf) continue;

            Suggestion cachedSuggestion = timeframeSuggestionCache.getSuggestionFor(otherTf);
            if (cachedSuggestion != null && cachedSuggestion.getItemId() == currentSuggestion.getItemId()) {
                long otherTotalSell = (long) cachedSuggestion.getQuantity() * GeTax.getPostTaxPrice(cachedSuggestion.getItemId(), cachedSuggestion.getPrice());
                long delta = currentTotalSell - otherTotalSell;

                String deltaColor = delta >= 0 ? "green" : "red";
                String sign = delta >= 0 ? "+" : "";
                String formattedDelta = NumberFormat.getNumberInstance().format(delta);

                deltaString.append(String.format("<br>vs %s: <font color='%s'>%s%s gp</font>", timeframeToString(otherTf), deltaColor, sign, formattedDelta));
            }
        }
        return deltaString.toString();
    }

    private String timeframeToString(int tf) {
        if (tf == 120) return "2h";
        if (tf == 480) return "8h";
        return tf + "m";
    }

    public void suggestCollect() {
        setMessage("Collect items");
        setButtonsVisible(false);
    }

    public void suggestAddGp() {
        NumberFormat formatter = NumberFormat.getNumberInstance();
        setMessage("Add " +
                "at least <FONT COLOR=" + highlightedColor + ">" + formatter.format(MIN_GP_NEEDED_TO_FLIP)
                + "</FONT> gp<br>to your inventory<br>"
                + "to get a flip suggestion");
        setButtonsVisible(false);
    }

    public void suggestOpenGe() {
        setMessage("Open the Grand Exchange<br>"
                + "to get a flip suggestion");
        setButtonsVisible(false);
    }

    public void setIsPausedMessage() {
        setMessage("Suggestions are paused");
        setButtonsVisible(false);
    }

    public void setMessage(String message) {
        ((CardLayout)suggestionText.getParent().getParent().getLayout()).show(suggestionText.getParent().getParent(), "text");
        suggestionIcon.setVisible(false);
        innerSuggestionMessage = message;
        setButtonsVisible(false);

        String displayMessage = message;
        if (message != null && message.contains("<manage>")) {
            displayMessage = message.replace("<manage>", "<a href='#' style='text-decoration:underline'>manage</a>");
            if (Arrays.stream(suggestionText.getMouseListeners()).noneMatch(l -> l instanceof ManageClickListener)) {
                suggestionText.addMouseListener(new ManageClickListener());
                suggestionText.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
        } else {
            suggestionText.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
        suggestionText.setText("<html><center>" + displayMessage + "<br>" + serverMessage + "</center></html>");
    }

    private class ManageClickListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            String text = suggestionText.getText();
            if (text.contains("manage")) {
                premiumInstanceController.loadAndOpenPremiumInstanceDialog();
            }
        }
    }

    public boolean isCollectItemsSuggested() {
        return suggestionText.isVisible() && "Collect items".equals(innerSuggestionMessage);
    }

    public void showLoading() {
        ((CardLayout)spinner.getParent().getLayout()).show(spinner.getParent(), "spinner");
        setServerMessage("");
        setButtonsVisible(false);
        suggestionIcon.setVisible(false);
    }

    public void hideLoading() {
        ((CardLayout)spinner.getParent().getLayout()).show(spinner.getParent(), "text");
    }


    private void setButtonsVisible(boolean visible) {
        skipButton.setVisible(visible);
        blockButton.setVisible(visible);
        graphButton.setVisible(visible);
    }

    public void displaySuggestion() {
        Suggestion suggestion = suggestionManager.getSuggestion();
        if (suggestion == null) return;

        AccountStatus accountStatus = accountStatusManager.getAccountStatus();
        if(accountStatus == null) return;

        setServerMessage(suggestion.getMessage());
        boolean collectNeeded = accountStatus.isCollectNeeded(suggestion);
        if(collectNeeded && !uncollectedManager.HasUncollected(osrsLoginManager.getAccountHash())) {
            log.warn("tick {} collect is suggested but there is nothing to collect! suggestion: {} {} {}", client.getTickCount(), suggestion.getType(), suggestion.getQuantity(), suggestion.getItemId());
        }

        if (collectNeeded) {
            suggestCollect();
        } else if(suggestion.getType().equals("wait") && !grandExchange.isOpen() && accountStatus.emptySlotExists()) {
            suggestOpenGe();
        } else if (suggestion.getType().equals("wait") && accountStatus.moreGpNeeded()) {
            suggestAddGp();
        } else {
            updateSuggestion(suggestion);
        }
        highlightController.redraw();
    }

    public void refresh() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refresh);
            return;
        }

        hideLoading();

        if (pausedManager.isPaused()) {
            setIsPausedMessage();
            return;
        }

        String errorMessage = osrsLoginManager.getInvalidStateDisplayMessage();
        if (errorMessage != null) {
            setMessage(errorMessage);
            return;
        }

        if (suggestionManager.isSuggestionRequestInProgress()) {
            showLoading();
            return;
        }

        final HttpResponseException suggestionError = suggestionManager.getSuggestionError();
        if(suggestionError != null) {
            highlightController.redraw();
            setMessage("Error: " + suggestionError.getMessage());
            return;
        }

        if(!client.isClientThread()) {
            clientThread.invoke(this::displaySuggestion);
        } else {
            displaySuggestion();
        }
    }
}