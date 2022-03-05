package net.runelite.client.plugins.autobasalt;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.iutils.*;
import net.runelite.client.plugins.iutils.game.Game;
import net.runelite.client.plugins.iutils.ui.Chatbox;
import net.runelite.client.ui.overlay.OverlayManager;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.awt.*;

@Extension
@PluginDependency(iUtils.class)
@PluginDescriptor(
		name = "AutoBasalt",
		description = "Automatically mines basalt and salt",
		tags = {"chas", "basalt", "salt", "weiss", "mining", "mine", "miner"}
)
@Slf4j
public class AutoBasalt extends Plugin
{
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private Client client;
	@Inject
	private ClientThread clientThread;
	@Inject
	private iUtils utils;
	@Inject
	private WalkUtils walk;
	@Inject
	private InventoryUtils inventory;
	@Inject
	private ObjectUtils objectUtils;
	@Inject
	private CalculationUtils calc;
	@Inject
	private BankUtils bank;
	@Inject
	private Chatbox chat;

	private Player player;
	private Rectangle bounds;
	private Game game;
	LegacyMenuEntry targetMenu;
	PluginState state;
	PluginState lastState;
	int timeout;


	public AutoBasalt() {
		state = PluginState.TIMEOUT;
		lastState = state;
	}

	private void reset() {
		state = PluginState.TIMEOUT;
		lastState = state;
	}

	@Override
	protected void startUp() { }

	@Override
	protected void shutDown(){ reset(); }

	@Subscribe
	private void onGameTick(GameTick event)
	{
		player = client.getLocalPlayer();
		if (player != null && client != null) {
			state = getState();
			lastState = state;
			if (player.isMoving())
				return;
			switch (state) {
				case TIMEOUT:
					if (timeout > 0)
						timeout = 0;
					else
						timeout--;
					break;
				default:
					timeout = 1;
					break;
			}
		}
	}

	PluginState getState() {
		return PluginState.TIMEOUT;
	}
}