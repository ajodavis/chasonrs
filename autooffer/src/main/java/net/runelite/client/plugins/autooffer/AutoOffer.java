package net.runelite.client.plugins.autooffer;

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
import net.runelite.client.ui.overlay.OverlayManager;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.awt.*;
import java.util.Set;

@Extension
@PluginDependency(iUtils.class)
@PluginDescriptor(
		name = "AutoOffer",
		description = "Automatically casts sinister/demonic offering",
		tags = {"chas", "demonic", "sinister", "offering"}
)
@Slf4j
public class AutoOffer extends Plugin
{
	@Inject
	private PluginConfig config;
	@Inject
	OverlayManager overlayManager;
	@Inject
	private PluginOverlay overlay;
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

	private Player player;
	private Rectangle bounds;
	private Game game;

	LegacyMenuEntry targetMenu;
	PluginState state;
	PluginState lastState;

	int timeout;
	int offeringTimeout;
	boolean startPlugin;

	Set<Integer> BONES = Set.of(
			ItemID.BONES, ItemID.MONKEY_BONES, ItemID.BAT_BONES,
			ItemID.BIG_BONES, ItemID.JOGRE_BONES, ItemID.SHAIKAHAN_BONES,
			ItemID.BABYDRAGON_BONES, ItemID.WYRM_BONES, ItemID.DRAGON_BONES,
			ItemID.WYVERN_BONES, ItemID.DRAKE_BONES, ItemID.FAYRG_BONES,
			ItemID.LAVA_DRAGON_BONES, ItemID.RAURG_BONES, ItemID.HYDRA_BONES,
			ItemID.DAGANNOTH_BONES, ItemID.OURG_BONES, ItemID.SUPERIOR_DRAGON_BONES
	);

	Set<Integer> ASHES = Set.of(
			ItemID.FIENDISH_ASHES, ItemID.VILE_ASHES, ItemID.MALICIOUS_ASHES,
			ItemID.ABYSSAL_ASHES, ItemID.INFERNAL_ASHES
	);

	Set<Integer> HEADS = Set.of(
			ItemID.ENSOULED_GOBLIN_HEAD, ItemID.ENSOULED_GOBLIN_HEAD_13448,
			ItemID.ENSOULED_MONKEY_HEAD, ItemID.ENSOULED_MONKEY_HEAD_13451,
			ItemID.ENSOULED_IMP_HEAD, ItemID.ENSOULED_IMP_HEAD_13454,
			ItemID.ENSOULED_MINOTAUR_HEAD, ItemID.ENSOULED_MINOTAUR_HEAD_13457,
			ItemID.ENSOULED_SCORPION_HEAD, ItemID.ENSOULED_SCORPION_HEAD_13460,
			ItemID.ENSOULED_BEAR_HEAD, ItemID.ENSOULED_BEAR_HEAD_13463,
			ItemID.ENSOULED_UNICORN_HEAD, ItemID.ENSOULED_UNICORN_HEAD_13466,
			ItemID.ENSOULED_DOG_HEAD, ItemID.ENSOULED_DOG_HEAD_13469,
			ItemID.ENSOULED_CHAOS_DRUID_HEAD, ItemID.ENSOULED_CHAOS_DRUID_HEAD_13472,
			ItemID.ENSOULED_GIANT_HEAD, ItemID.ENSOULED_GIANT_HEAD_13475,
			ItemID.ENSOULED_OGRE_HEAD, ItemID.ENSOULED_OGRE_HEAD_13478,
			ItemID.ENSOULED_ELF_HEAD, ItemID.ENSOULED_ELF_HEAD_13481,
			ItemID.ENSOULED_TROLL_HEAD, ItemID.ENSOULED_TROLL_HEAD_13484,
			ItemID.ENSOULED_HORROR_HEAD, ItemID.ENSOULED_HORROR_HEAD_13487,
			ItemID.ENSOULED_KALPHITE_HEAD, ItemID.ENSOULED_KALPHITE_HEAD_13490,
			ItemID.ENSOULED_DAGANNOTH_HEAD, ItemID.ENSOULED_DAGANNOTH_HEAD_13493,
			ItemID.ENSOULED_BLOODVELD_HEAD, ItemID.ENSOULED_BLOODVELD_HEAD_13496,
			ItemID.ENSOULED_TZHAAR_HEAD, ItemID.ENSOULED_TZHAAR_HEAD_13499,
			ItemID.ENSOULED_DEMON_HEAD, ItemID.ENSOULED_DEMON_HEAD_13502,
			ItemID.ENSOULED_AVIANSIE_HEAD, ItemID.ENSOULED_AVIANSIE_HEAD_13505,
			ItemID.ENSOULED_ABYSSAL_HEAD, ItemID.ENSOULED_ABYSSAL_HEAD_13508,
			ItemID.ENSOULED_DRAGON_HEAD, ItemID.ENSOULED_DRAGON_HEAD_13511
	);


	public AutoOffer() {
		startPlugin = false;
		state = PluginState.TIMEOUT;
		lastState = state;
	}

	private void reset() {
		startPlugin = false;
		state = PluginState.TIMEOUT;
		lastState = state;
		overlayManager.remove(overlay);
	}

	@Provides
	PluginConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PluginConfig.class);
	}

	@Override
	protected void startUp() { }

	@Override
	protected void shutDown(){ reset(); }

	@Subscribe
	private void onGameTick(GameTick event)
	{
		if (!startPlugin)
			return;
		player = client.getLocalPlayer();
		if (player != null && client != null) {
			state = getState();
			if (config.debug() && state != lastState)
				utils.sendGameMessage("State: " + state.toString());
			lastState = state;
			if (player.isMoving())
				return;
			Widget widget;
			WidgetItem item;
			switch (state) {
				case TIMEOUT:
					if (timeout <= 0)
						timeout = 0;
					else
						timeout--;
					if (offeringTimeout <= 0)
						offeringTimeout = 0;
					else
						offeringTimeout--;
					return;
				case DEMONIC_OFFERING:
					widget = client.getWidget(218, 174);
					if (widget != null) {
						targetMenu = new LegacyMenuEntry("Cast", "<col=00ff00>Demonic Offering</col>", 1 , MenuAction.CC_OP, -1, widget.getId(), false);
						offeringTimeout = 8 + calc.getRandomIntBetweenRange(1, 3);
						utils.doActionMsTime(targetMenu, widget.getBounds(), calc.getRandomIntBetweenRange(25, 200));
					}
					break;
				case SINISTER_OFFERING:
					widget = client.getWidget(218, 175);
					if (widget != null) {
						targetMenu = new LegacyMenuEntry("Cast", "<col=00ff00>Sinister Offering</col>", 1 , MenuAction.CC_OP, -1, widget.getId(), false);
						offeringTimeout = 8 + calc.getRandomIntBetweenRange(1, 3);
						utils.doActionMsTime(targetMenu, widget.getBounds(), calc.getRandomIntBetweenRange(25, 200));
					}
					break;
				case FILL_SOUL_BEARER:
					item = inventory.getWidgetItem(ItemID.SOUL_BEARER);
					if (item != null) {
						targetMenu = new LegacyMenuEntry("", "", item.getId(), MenuAction.ITEM_FIRST_OPTION, 0, WidgetInfo.INVENTORY.getId(), false);
						timeout += 1 + calc.getRandomIntBetweenRange(0, 1);
						utils.doActionMsTime(targetMenu, item.getCanvasBounds(), calc.getRandomIntBetweenRange(25, 200));
					}
					break;
				default:
					timeout = 1;
					break;
			}
		}
	}

	PluginState getState() {
		if (timeout > 0 || offeringTimeout > 0)
			return PluginState.TIMEOUT;
		if (bank.isOpen())
			return PluginState.TIMEOUT;
		return getStates();
	}

	PluginState getStates() {
		int bones = 0;
		int ashes = 0;
		int heads = 0;
		if (inventory.containsItemAmount(ItemID.WRATH_RUNE, 1, true, false)
				|| inventory.runePouchQuanitity(ItemID.WRATH_RUNE) >= 1) {
			if (config.sinisterOffering()
					&& (inventory.containsItemAmount(ItemID.BLOOD_RUNE, 1, true, false)
					|| inventory.runePouchQuanitity(ItemID.BLOOD_RUNE) >= 1)) {
				for (int id : BONES) {
					if (inventory.getItemCount(id, false) > 0)
						bones += inventory.getItemCount(id, false);
				}
				if (bones >= config.bones())
					return PluginState.SINISTER_OFFERING;
			}
			if (config.demonicOffering()
					&& (inventory.containsItemAmount(ItemID.SOUL_RUNE, 1, true, false)
					|| inventory.runePouchQuanitity(ItemID.SOUL_RUNE) >= 1)) {
				for (int id : ASHES) {
					if (inventory.getItemCount(id, false) > 0)
						ashes += inventory.getItemCount(id, false);
				}
				if (ashes >= config.ashes())
					return PluginState.DEMONIC_OFFERING;
			}
		}
		if (config.soulBearer() && inventory.containsItem(ItemID.SOUL_BEARER)) {
			for (int id : HEADS) {
				if (inventory.getItemCount(id, false) > 0)
					heads += inventory.getItemCount(id, false);
			}
			if (heads >= config.fillAmount())
				return PluginState.FILL_SOUL_BEARER;
		}
		return PluginState.TIMEOUT;
	}

	@Subscribe
	private void onConfigButtonPressed(ConfigButtonClicked configButtonClicked) {
		if (!configButtonClicked.getGroup().equalsIgnoreCase("AutoOffer")) {
			return;
		}
		switch (configButtonClicked.getKey()) {
			case "startPlugin":
				if (!startPlugin) {
					timeout = calc.getRandomIntBetweenRange(2, 4);
					offeringTimeout = calc.getRandomIntBetweenRange(2, 4);
					startPlugin = true;
					overlayManager.add(overlay);
				} else
					reset();
				break;
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
}