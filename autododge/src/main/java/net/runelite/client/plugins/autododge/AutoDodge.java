package net.runelite.client.plugins.autododge;

import com.google.common.collect.ImmutableList;
import com.google.inject.Provides;
import com.openosrs.client.game.WorldLocation;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
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
import net.runelite.client.plugins.iutils.walking.CollisionMap;
import net.runelite.client.ui.overlay.OverlayManager;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.awt.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.rmi.server.LogStream.log;

@Extension
@PluginDependency(iUtils.class)
@PluginDescriptor(
		name = "AutoDodge",
		description = "Automatically dodges AoE (area of attack)",
		tags = {"chas", "aoe", "dodge", "demonic", "gorilla"}
)
@Slf4j
public class AutoDodge extends Plugin
{
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private Client client;
	@Inject
	private Game game;
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
	private NPCUtils npcUtils;
	@Inject
	private BankUtils bank;
	@Inject
	private Chatbox chat;

	@Inject
	PluginConfig config;
	@Provides
	PluginConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PluginConfig.class);
	}

	private Player player;
	private Rectangle bounds;
	LegacyMenuEntry targetMenu;
	PluginState state;
	PluginState lastState;
	boolean startPlugin;
	Instant botTimer;
	int timeout;

	boolean dodgeDemonics;
	List<LocalPoint> avoids;

	public AutoDodge() {
		botTimer = null;
		startPlugin = false;
		state = PluginState.TIMEOUT;
		lastState = state;
		dodgeDemonics = false;
		avoids = ImmutableList.of();
	}

	private void reset() {
		timeout = 0;
		startPlugin = false;
		botTimer = null;
		state = PluginState.TIMEOUT;
		lastState = state;
		dodgeDemonics = false;
	}

	@Override
	protected void startUp() { }

	@Override
	protected void shutDown(){ reset(); }

	@Subscribe
	private void onConfigButtonPressed(ConfigButtonClicked configButtonClicked) {
		if (!configButtonClicked.getGroup().equalsIgnoreCase("AutoDodge")) {
			return;
		}

		WorldPoint loc = Objects.requireNonNull(client.getLocalPlayer()).getWorldLocation();
		LocalPoint localLoc = LocalPoint.fromWorld(client, loc);

		if (configButtonClicked.getKey().equalsIgnoreCase("testdemonics")) {
			CollisionMap map = new CollisionMap();
			if (map.w(player.getLocalLocation().getX(), player.getLocalLocation().getY(), 0))
				utils.sendGameMessage("true");
			else
				utils.sendGameMessage("false");
			return;
		}
	}

	@Subscribe
	private void onGameTick(GameTick event)
	{
		player = client.getLocalPlayer();
		if (player != null && client != null) {
			state = getState();
			if (config.debug() && state != lastState && state != PluginState.TIMEOUT) {
				utils.sendGameMessage("AutoDodge: " + state.toString());
			}
			if (state != PluginState.TIMEOUT)
				lastState = state;
			if (player.isMoving())
				return;
			switch (state) {
				case TIMEOUT:
					if (timeout <= 0)
						timeout = 0;
					else
						timeout--;
					break;
				case DODGE_DEMONICS:
					utils.sendGameMessage("dodging rockkkk");
					dodgeDemonics = false;
					break;
				default:
					timeout = 1;
					break;
			}
		}
	}

	PluginState getState() {
		if (timeout > 0 || player.isMoving())
			return PluginState.TIMEOUT;
		if (config.dodgeDemonics() && dodgeDemonics)
			return PluginState.DODGE_DEMONICS;
		return PluginState.TIMEOUT;
	}

	@Subscribe
	private void onProjectileSpawned(ProjectileSpawned event) {
		Projectile projectile = event.getProjectile();
		WorldPoint loc = Objects.requireNonNull(client.getLocalPlayer()).getWorldLocation();
		LocalPoint localLoc = LocalPoint.fromWorld(client, loc);

		if (config.dodgeDemonics() && projectile.getId() == 856) {
			if (projectile.getTarget() == localLoc) {
				avoids.add(localLoc);
				dodgeDemonics = true;
			}
		}
	}
}