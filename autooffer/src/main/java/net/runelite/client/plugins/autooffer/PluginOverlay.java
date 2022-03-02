package net.runelite.client.plugins.autooffer;

import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;

import java.awt.*;
import java.util.Locale;

import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;

public class PluginOverlay extends OverlayPanel {

    private final AutoOffer plugin;
    private final PluginConfig config;

    @Inject
    private PluginOverlay(final AutoOffer plugin, final PluginConfig config) {
        super(plugin);
        setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
        this.plugin = plugin;
        this.config = config;
        getMenuEntries().add(new OverlayMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "AutoOffer Overlay"));
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.showOverlay())
            return null;
        panelComponent.getChildren().clear();

        /* Title and width */
        String title = "AutoOffer";
        panelComponent.getChildren().add(TitleComponent.builder().text(title).color(Color.YELLOW).build());
        panelComponent.setBackgroundColor(Color.DARK_GRAY);
        panelComponent.setPreferredSize(new Dimension(graphics.getFontMetrics().stringWidth(title) + 80,0));

        /* Content */
        panelComponent.getChildren().add(LineComponent.builder().left("State: ").right(plugin.state.toString().toLowerCase().replace("_", " ")).build());

        /* Render */
        return panelComponent.render(graphics);
    }

}
