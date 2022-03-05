package net.runelite.client.plugins.autobasalt;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
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
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

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
	private NPCUtils npcUtils;
	@Inject
	private BankUtils bank;
	@Inject
	private Chatbox chat;

	@Inject
	PluginOverlay overlay;
	@Inject
	PluginConfig config;

	private Player player;
	private Rectangle bounds;
	private Game game;
	LegacyMenuEntry targetMenu;
	PluginState state;
	PluginState lastState;
	boolean startPlugin;
	Instant botTimer;
	int timeout;
	List<Integer> mineRegion;
	List<Integer> weissRegion;


	int basalt;
	int urtSalt;
	int efhSalt;
	int teSalt;
	int snowflake;
	int descStairs;
	int ascStairs;

	public AutoBasalt() {
		mineRegion = Arrays.asList(11425);
		weissRegion = Arrays.asList(11325);
		botTimer = null;
		snowflake = NpcID.SNOWFLAKE;
		descStairs = 33234;
		ascStairs = 33261;
		startPlugin = false;
		state = PluginState.TIMEOUT;
		lastState = state;
	}

	@Provides
	PluginConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PluginConfig.class);
	}


	private void reset() {
		timeout = 0;
		startPlugin = false;
		botTimer = null;
		state = PluginState.TIMEOUT;
		lastState = state;
		overlayManager.remove(overlay);
	}

	@Override
	protected void startUp() { }

	@Override
	protected void shutDown(){ reset(); }

	@Subscribe
	private void onConfigButtonPressed(ConfigButtonClicked configButtonClicked) {
		if (!configButtonClicked.getGroup().equalsIgnoreCase("AutoBasalt")) {
			return;
		}
		switch (configButtonClicked.getKey()) {
			case "startPlugin":
				if (!startPlugin) {
					startPlugin = true;
					botTimer = Instant.now();
					state = PluginState.TIMEOUT;
					overlayManager.add(overlay);
				} else {
					reset();
				}
				break;
		}
	}

	@Subscribe
	private void onGameTick(GameTick event)
	{
		if (!startPlugin)
			return;
		player = client.getLocalPlayer();
		if (player != null && client != null) {
			state = getState();
			lastState = state;
			if (player.isMoving())
				return;
			switch (state) {
				case TIMEOUT:
					if (timeout >= 0)
						timeout = 0;
					else
						timeout--;
					break;
				case MINE:
					if (player.getAnimation() != -1)
						break;
					if (config.debug())
						utils.sendGameMessage("click rock");
					actionObject(config.mine().getObjectId(), MenuAction.GAME_OBJECT_FIRST_OPTION);
					timeout = calc.getRandomIntBetweenRange(2, 6);
					break;
				case ASCEND_STAIRS:
					actionObject(ascStairs, MenuAction.GAME_OBJECT_FIRST_OPTION);
					timeout = calc.getRandomIntBetweenRange(1, 4);
					break;
				case DESCEND_STAIRS:
					actionObject(descStairs, MenuAction.GAME_OBJECT_FIRST_OPTION);
					timeout = calc.getRandomIntBetweenRange(1, 4);
					break;
				case NOTE_BASALT:
					if (inventory.containsItem(ItemID.BASALT)) {
						itemOnNPC(ItemID.BASALT, snowflake);
						timeout = calc.getRandomIntBetweenRange(2, 4);
					} else {
						utils.sendGameMessage("no basalt in invent");
						reset();
					}
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
		if (inRegion(client, mineRegion)) {
			if (config.mine() == PluginConfig.Mine.BASALT) {
				if (!inventory.isFull())
					return PluginState.MINE;
				else {
					if (!config.makeTele())
						return PluginState.ASCEND_STAIRS;
				}
			}
		} else if (inRegion(client, weissRegion)) {
			if (config.mine() == PluginConfig.Mine.BASALT) {
				if (inventory.isFull())
					return PluginState.NOTE_BASALT;
				else
					return PluginState.DESCEND_STAIRS;
			}
		}
		return PluginState.TIMEOUT;
	}

	void mine(int objectId) {
		GameObject rock = objectUtils.findNearestGameObject(objectId);
		MenuAction action = MenuAction.GAME_OBJECT_FIRST_OPTION;
		if (rock != null) {
			targetMenu = new LegacyMenuEntry("", "", rock.getId(), action, rock.getSceneMinLocation().getX(), rock.getSceneMinLocation().getY(), false);
			if (!config.invokes())
				utils.doGameObjectActionMsTime(rock, action.getId(), calc.getRandomIntBetweenRange(25, 300));
			else
				utils.doInvokeMsTime(targetMenu, 0);
		}
	}

	boolean inRegion(Client client, List<Integer> region) {
		return Arrays.stream(client.getMapRegions()).anyMatch(region::contains);
	}

	void itemOnNPC(int itemId, int npcId) {
		WidgetItem item = inventory.getWidgetItem(itemId);
		if (item == null)
			return;

		NPC npc = npcUtils.findNearestNpc(npcId);
		if (npc == null)
			return;

		targetMenu = new LegacyMenuEntry("Use", "", npc.getIndex(), MenuAction.ITEM_USE_ON_NPC, 0, 0, false);
		utils.doModifiedActionMsTime(targetMenu, item.getId(), item.getIndex(), MenuAction.ITEM_USE_ON_NPC.getId(), npc.getConvexHull().getBounds(), calc.getRandomIntBetweenRange(25, 200));
	}

	void actionObject(int id, MenuAction action) {
		GameObject obj = objectUtils.findNearestGameObject(id);
		if (obj != null) {
			targetMenu = new LegacyMenuEntry("", "", obj.getId(), action, obj.getSceneMinLocation().getX(), obj.getSceneMinLocation().getY(), false);
			if (!config.invokes())
				utils.doGameObjectActionMsTime(obj, action.getId(), calc.getRandomIntBetweenRange(25, 300));
			else
				utils.doInvokeMsTime(targetMenu, 0);
		}
	}
}