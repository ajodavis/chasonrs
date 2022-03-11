package net.runelite.client.plugins.autotrade;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.WorldService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.iutils.*;
import net.runelite.client.plugins.iutils.game.Game;
import net.runelite.client.plugins.worldhopper.WorldHopperPlugin;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.WorldUtil;
import net.runelite.http.api.worlds.WorldResult;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Locale;

@Extension
@PluginDependency(iUtils.class)
@PluginDescriptor(
		name = "AutoTrade",
		description = "Automatically mule items",
		tags = {"chas", "trade", "autotrade"}
)
@Slf4j
public class AutoTrade extends Plugin
{
	@Inject
	private AutoTradeConfig config;

	@Inject
	OverlayManager overlayManager;

	@Inject
	private AutoTradeOverlay overlay;

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
	private MenuUtils menu;

	@Inject
	private MouseUtils mouse;

	@Inject
	private PlayerUtils playerUtils;

	@Inject
	private PrayerUtils prayerUtils;

	@Inject
	private ObjectUtils objectUtils;

	@Inject
	private CalculationUtils calc;

	@Inject
	private BankUtils bank;

	@Inject
	private InterfaceUtils interfaceUtils;

	@Inject
	private WorldService worldService;

	private net.runelite.api.World quickHopTargetWorld;

	private Player player;
	private Rectangle bounds;
	private Game game;
	boolean withdrawn;
	boolean deposited;
	boolean receivedTrade;
	int timeout;
	LegacyMenuEntry targetMenu;
	AutoTradeState state;
	AutoTradeState lastState;
	ChatMessage message;


	public AutoTrade() {
		withdrawn = false;
		deposited = false;
		receivedTrade = false;
		message = null;
		state = AutoTradeState.TIMEOUT;
		lastState = state;
	}

	private void reset() {
		withdrawn = false;
		deposited = false;
		receivedTrade = false;
		message = null;
		state = AutoTradeState.TIMEOUT;
		lastState = state;
		overlayManager.remove(overlay);
	}

	@Provides
	AutoTradeConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(AutoTradeConfig.class);
	}

	@Override
	protected void startUp()
	{
	}

	@Override
	protected void shutDown()
	{
		reset();
	}

	@Subscribe
	private void onGameStateChanged(final GameStateChanged event) {
		final GameState gamestate = event.getGameState();
		if (gamestate == GameState.LOADING) {
			withdrawn = false;
			deposited = false;
			receivedTrade = false;
			state = AutoTradeState.TIMEOUT;
			lastState = state;
		}
	}

	@SuppressWarnings("SameParameterValue")
	private void sendKey(int key)
	{
		keyEvent(KeyEvent.KEY_PRESSED, key);
		keyEvent(KeyEvent.KEY_RELEASED, key);
	}

	private void keyEvent(int id, int key)
	{
		KeyEvent e = new KeyEvent(
				client.getCanvas(), id, System.currentTimeMillis(),
				0, key, KeyEvent.CHAR_UNDEFINED
		);

		client.getCanvas().dispatchEvent(e);
	}

	@Subscribe
	private void onGameTick(GameTick event)
	{
		player = client.getLocalPlayer();
		if (player != null && client != null) {
			state = getState();
			if (config.debug()) {
				if (state != lastState)
					utils.sendGameMessage("AutoTradeState: " + state.toString());
			}

			lastState = state;
			if (!receivedTrade)
				return;
			if (player.isMoving())
				return;
			switch (state) {
				case TIMEOUT:
					if (!bank.isOpen() && !tradeIsOpen())
						playerUtils.handleRun(30, 20);
					timeout--;
					return;
				case DEPOSITED_ITEMS:
					bank.depositAll();
					deposited = true;
					timeout = calc.getRandomIntBetweenRange(2, 3);
					return;
				case FIND_BANK:
					openBank();
					timeout = calc.getRandomIntBetweenRange(2, 3);
					break;
				case WITHDRAW_ITEM:
					Widget bankItem = bank.getBankItemWidget(config.itemID());
					if (bankItem == null) {
						utils.sendGameMessage("You don't have any of this item. ");
						reset();
						break;
					}
					targetMenu = new LegacyMenuEntry("", "", 7, MenuAction.CC_OP_LOW_PRIORITY, bankItem.getIndex(), WidgetInfo.BANK_ITEM_CONTAINER.getId(), false);
					utils.doInvokeMsTime(targetMenu, calc.getRandomIntBetweenRange(25, 200));
					timeout = calc.getRandomIntBetweenRange(4, 6);
				case OPEN_TRADE:
					openTrade();
					timeout = calc.getRandomIntBetweenRange(2, 3);
					break;
				case ADD_ITEMS:
					if (tradeIsOpen() && getTradeWindow() == 1) {
						//new LegacyMenuEntry("", "", item.getId(), action, item.getIndex(), WidgetInfo.INVENTORY.getId(), false);
						if (inventory.containsItem(config.itemID())) {
							final WidgetItem item = inventory.getWidgetItem(config.itemID());
							targetMenu = new LegacyMenuEntry("Offer-All<col=ff9040>", "", 4, MenuAction.CC_OP, item.getIndex(), 22020096, false);
							utils.doInvokeMsTime(targetMenu, calc.getRandomIntBetweenRange(25, 200));
						}
					}
					break;
				case ACCEPT_FIRST:
					if (tradeIsOpen() && getTradeWindow() == 1) {
						if (!inventory.containsItem(config.itemID())) {
							//utils.sendGameMessage("accepting first");
							targetMenu = new LegacyMenuEntry("Accept", "", 1, MenuAction.CC_OP, -1, 21954570, true);
							utils.doInvokeMsTime(targetMenu, calc.getRandomIntBetweenRange(25, 200));
							targetMenu = null;
						}
					}
					break;
				case ACCEPT_SECOND:
					if (tradeIsOpen() && getTradeWindow() == 2) {
						if (!inventory.containsItem(config.itemID())) {
							targetMenu = new LegacyMenuEntry("Accept", "", 1, MenuAction.CC_OP, -1, 21889037, false);
							utils.doInvokeMsTime(targetMenu, calc.getRandomIntBetweenRange(25, 200));
							targetMenu = null;
						}
					}
					break;
				default:
					timeout = 1;
					break;
			}
		}

	}

	@Subscribe
	private void onChatMessage(ChatMessage event) {
		Widget widget = client.getWidget(10485775);

		if (widget != null) {
			bounds = widget.getBounds();
		}

		if (event.getType() == ChatMessageType.CONSOLE)
			return;
		String trade = " wishes to trade with you.";
		String unable = "Unable to find ";
		String busy = "Other player is busy at the moment.";
		String declined = "Other player declined trade.";
		String accepted = "Accepted trade.";

		if (event.getType() == ChatMessageType.TRADEREQ
				&& event.getMessage().equalsIgnoreCase(config.playerName() + trade)) {
			overlayManager.add(overlay);
			message = event;
			receivedTrade = true;
			return;
		}
		if (event.getType() == ChatMessageType.TRADE || event.getType() == ChatMessageType.GAMEMESSAGE) {
			if (event.getMessage().contains(busy)) {
				timeout = 5;
				openTrade();
			}
		}
		if ((event.getType() == ChatMessageType.GAMEMESSAGE
				&& (event.getMessage().equalsIgnoreCase(unable + config.playerName()))) || (event.getType() == ChatMessageType.TRADE
				&& (event.getMessage().equalsIgnoreCase(declined)))) {
			reset();
			return;
		}
		if (event.getType() == ChatMessageType.TRADE && event.getMessage().equalsIgnoreCase(accepted)) {
			reset();
			return;
		}

		/* Chat commands START */

		if (event.getType() == ChatMessageType.FRIENDSCHAT) {
			if (event.getName().equalsIgnoreCase(config.playerName())) {
				if (event.getMessage().toLowerCase().startsWith("!hop")) {
					int world = Integer.parseInt(event.getMessage().substring(event.getMessage().length() - 3));
					if (world >= 301) {
						client.openWorldHopper();
						utils.sendGameMessage(config.playerName() + " told us to hop to world: " + world);
						hop(world);
					}
				} else if (event.getMessage().toLowerCase().startsWith("!wh")) {
					utils.sendGameMessage(config.playerName() + " told us to open the world hopper");
					client.openWorldHopper();
				}
			}
		}

		/* Chat commands END */

	}

	private void hop(int worldId)
	{
		assert client.isClientThread();

		WorldResult worldResult = worldService.getWorlds();
		// Don't try to hop if the world doesn't exist
		net.runelite.http.api.worlds.World world = worldResult.findWorld(worldId);
		if (world == null)
		{
			return;
		}

		final net.runelite.api.World rsWorld = client.createWorld();
		rsWorld.setActivity(world.getActivity());
		rsWorld.setAddress(world.getAddress());
		rsWorld.setId(world.getId());
		rsWorld.setPlayerCount(world.getPlayers());
		rsWorld.setLocation(world.getLocation());
		rsWorld.setTypes(WorldUtil.toWorldTypes(world.getTypes()));

		client.hopToWorld(rsWorld);
		return;
	}

	public AutoTradeState getState() {
		if (timeout > 0)
			return AutoTradeState.TIMEOUT;
		if (bank.isOpen()) {
				//utils.sendGameMessage("bank is open");
			return getBankState();
		}
		if (tradeIsOpen()) {
			//utils.sendGameMessage("trade is open");
			return getTradeState();
		}
		return getStates();
	}

	private boolean tradeIsOpen() {
		Widget trade = client.getWidget(335, 2);
		Widget trade2 = client.getWidget(334, 2);
		if (trade != null && !trade.isHidden() || trade2 != null && !trade2.isHidden())
			return true;
		return false;
	}

	private int getTradeWindow() {
		Widget trade = client.getWidget(335, 2);
		Widget trade2 = client.getWidget(334, 2);
		if (trade != null && !trade.isHidden())
			return 1;
		if (trade2 != null && !trade2.isHidden())
			return 2;
		return -1;
	}

	private AutoTradeState getTradeState() {
		switch (getTradeWindow()) {
			case 1:
				if (inventory.containsItem(config.itemID()))
					return AutoTradeState.ADD_ITEMS;
				if (!inventory.containsItem(config.itemID())) {
					if (client.getWidget(335, 30) != null
							&& client.getWidget(335, 30).getText().equalsIgnoreCase("Waiting for other player..."))
						return AutoTradeState.TIMEOUT;
					else
						return AutoTradeState.ACCEPT_FIRST;
				}
			case 2:
				if (client.getWidget(334, 4).getText().equalsIgnoreCase("Waiting for other player..."))
					return AutoTradeState.TIMEOUT;
				else if (client.getWidget(334, 4).getText().equalsIgnoreCase("Other player has accepted.")
						|| client.getWidget(334, 4).getText().equalsIgnoreCase("Are you sure you want to make this trade?"))
					return AutoTradeState.ACCEPT_SECOND;
				else
					return AutoTradeState.TIMEOUT;
		}
		return AutoTradeState.TIMEOUT;
	}

	private AutoTradeState getStates() {
		if (!withdrawn && receivedTrade)
			return AutoTradeState.FIND_BANK;
		return AutoTradeState.TIMEOUT;
	}

	private AutoTradeState getBankState() {
		if (bank.isOpen()) {
			if (!deposited)
				return AutoTradeState.DEPOSITED_ITEMS;
			if (deposited && !withdrawn) {
				if (inventory.containsItem(config.itemID())) {
					withdrawn = true;
					return AutoTradeState.OPEN_TRADE;
				}
				return AutoTradeState.WITHDRAW_ITEM;
			}

		}
		return AutoTradeState.TIMEOUT;
	}

	private void openBank() {
		GameObject feroxBank = objectUtils.findNearestGameObject(26711);
		GameObject bank = objectUtils.findNearestBank();
		if (feroxBank != null) {
			targetMenu = new LegacyMenuEntry("", "", 26711, MenuAction.GAME_OBJECT_FIRST_OPTION, feroxBank.getSceneMinLocation().getX(), feroxBank.getSceneMinLocation().getY(), false);
			utils.doInvokeMsTime(targetMenu, calc.getRandomIntBetweenRange(25, 200));
		} else {
			invokeObject(bank.getId(), MenuAction.GAME_OBJECT_SECOND_OPTION);
		}
	}

	private boolean invokeObject(int id, MenuAction action) {
		final GameObject obj = objectUtils.findNearestGameObject(id);
		if (obj != null) {
			targetMenu = new LegacyMenuEntry("","",obj.getId(),action,obj.getSceneMinLocation().getX(),obj.getSceneMinLocation().getY(),false);
			utils.doInvokeMsTime(targetMenu, calc.getRandomIntBetweenRange(20, 200));
			return true;
		}
		return false;
	}

	private boolean invokeItem(int id, MenuAction action) {
		if (inventory.containsItem(id)) {
			final WidgetItem item = inventory.getWidgetItem(id);
			LegacyMenuEntry menu = new LegacyMenuEntry("", "", item.getId(), action, item.getIndex(), WidgetInfo.INVENTORY.getId(), false);
			utils.doInvokeMsTime(menu, calc.getRandomIntBetweenRange(25, 200));
			return true;
		}
		return false;
	}

	private void openTrade() {
		if (receivedTrade) {
			List<Player> targets = client.getPlayers();
			Player target = null;
			for (Player p : targets) {
				if (p.getName().equalsIgnoreCase(config.playerName().toLowerCase(Locale.ROOT))) {
					target = p;
					break;
				}
			}
			if (target != null) {
				targetMenu = new LegacyMenuEntry("Trade with", "", target.getPlayerId(), MenuAction.PLAYER_FOURTH_OPTION, 0, 0, false);
				utils.doInvokeMsTime(targetMenu, calc.getRandomIntBetweenRange(600, 1200));
			} else {
				utils.sendGameMessage("Could not find player: " + config.playerName());
				receivedTrade = false;
				return;
			}
			timeout = calc.getRandomIntBetweenRange(2, 3);
		}
	}
}