//
// Decompiled by Procyon v0.5.36
//

package net.runelite.client.plugins.autovorki;

import net.runelite.api.MenuAction;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

public class AutoVorkiOverlay extends OverlayPanel {
    private final AutoVorki plugin;
    private final AutoVorkiConfig config;
    String format;

    @Inject
    private AutoVorkiOverlay(final AutoVorki plugin, final AutoVorkiConfig config) {
        super(plugin);
        setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
        //setPosition(OverlayPosition.BOTTOM_RIGHT);
        this.plugin = plugin;
        this.config = config;
        getMenuEntries().add(new OverlayMenuEntry(MenuAction.RUNELITE_OVERLAY_CONFIG, "Configure", "AutoVorki Overlay"));
    }

    public Dimension render(final Graphics2D graphics) {
        if (!config.showOverlay())
            return null;
        panelComponent.getChildren().clear();

        /* Setup */
        final String title = "AutoVorki";
        Duration duration = Duration.between(plugin.botTimer, Instant.now());
        panelComponent.getChildren().add(TitleComponent.builder().text(title).color(Color.YELLOW).build());
        panelComponent.setBackgroundColor(Color.DARK_GRAY);
        panelComponent.setPreferredSize(new Dimension(graphics.getFontMetrics().stringWidth(title) + 120, 0));

        /* Content */
        panelComponent.getChildren().add(LineComponent.builder().left("").build());
        panelComponent.getChildren().add(LineComponent.builder().left("Runtime: ").right((duration.toHours() > 0 ? (Long.toString(duration.toHours()) + ":") : ("")) + (new SimpleDateFormat("mm:ss").format(new Date(duration.toMillis())))).build());
        if (plugin.lastState != null)
            panelComponent.getChildren().add(LineComponent.builder().left("State: ").right(plugin.lastState.toString().toLowerCase().replace("FINISHED_WITHDRAWING", "FINISHED_BANKING").replace("_", " ").replace("WITHDRAW", "").replace("DEPOSIT", "BANK")).build());
        //if (config.debug())
        panelComponent.getChildren().add(LineComponent.builder().left("Timeout: ").right(Integer.toString(plugin.steps > 0 ? plugin.steps : Math.max(plugin.timeout, 0))).build());
        if (plugin.kills > 0)
            panelComponent.getChildren().add(LineComponent.builder().left("Kills: ").right(Integer.toString(plugin.kills)).build());
        if (plugin.lootValue > 0)
            panelComponent.getChildren().add(LineComponent.builder().left("Loot: ").right(formatValue()).build());
        return panelComponent.render(graphics);
    }

    String formatValue() {
        int value = plugin.lootValue;
        String profit = Integer.toString(value);
        if (value >= 1000 && value <= 99999)
            profit = new DecimalFormat("##,###").format(value);
        if (value >= 100000 && value <= 9999999) {
            if (value >= 1000000)
                profit = new DecimalFormat("#,######").format(value);
            profit = profit.substring(0, profit.length() - 3) + "K";
        }
        if (value >= 10000000)
            profit = String.format("%.3fM", value / 1000000.0);
        return profit;
    }
}
