package net.runelite.client.plugins.autotrade;

import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;

import java.awt.*;

import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;

public class AutoTradeOverlay extends OverlayPanel {

    private final AutoTrade plugin;
    private final AutoTradeConfig config;

    @Inject
    private AutoTradeOverlay(final AutoTrade plugin, final AutoTradeConfig config) {
        super(plugin);
        setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
        this.plugin = plugin;
        this.config = config;
        getMenuEntries().add(new OverlayMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "AutoTrade Overlay"));
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.showOverlay())
            return null;
        panelComponent.getChildren().clear();
        String title = "AutoTrade";
        /* Title and width */
        panelComponent.getChildren().add(TitleComponent.builder().text(title).color(Color.cyan).build());
        panelComponent.setPreferredSize(new Dimension(graphics.getFontMetrics().stringWidth(title) + 80,0));

        /* State */
        panelComponent.getChildren().add(LineComponent.builder().left("State: ").right(plugin.state.toString()).build());

        /* Render */
        return panelComponent.render(graphics);
    }

}
