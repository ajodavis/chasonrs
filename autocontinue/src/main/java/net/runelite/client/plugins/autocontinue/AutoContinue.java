package net.runelite.client.plugins.autocontinue;

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
		name = "AutoContinue",
		description = "Automatically progress through dialogue",
		tags = {"chas", "chat", "dialogue", "continue"}
)
@Slf4j
public class AutoContinue extends Plugin
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


	public AutoContinue() {
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
					return;
				case CONTINUE_CHAT:
					continueChat();
				break;
				default:
					timeout = 0;
					break;
			}
		}
	}

	PluginState getState() {
		if (chatboxIsOpen())
			return PluginState.CONTINUE_CHAT;
		return PluginState.TIMEOUT;
	}

	private boolean chatboxIsOpen() {
		return chat.chatState() == Chatbox.ChatState.NPC_CHAT || chat.chatState() == Chatbox.ChatState.PLAYER_CHAT
				|| chat.chatState() == Chatbox.ChatState.ITEM_CHAT || chat.chatState() == Chatbox.ChatState.SPECIAL
				|| chat.chatState() == Chatbox.ChatState.MODEL;
	}

	private void continueChat() {
		targetMenu = null;
		if (chat.chatState() == Chatbox.ChatState.NPC_CHAT)
			targetMenu = new LegacyMenuEntry("Continue", "", 0, MenuAction.WIDGET_TYPE_6, -1, client.getWidget(231, 5).getId(), false);
		if (chat.chatState() == Chatbox.ChatState.PLAYER_CHAT)
			targetMenu = new LegacyMenuEntry("Continue", "", 0, MenuAction.WIDGET_TYPE_6, -1, client.getWidget(217, 5).getId(), false);
		if (chat.chatState() == Chatbox.ChatState.ITEM_CHAT)
			targetMenu = new LegacyMenuEntry("Continue", "", 0, MenuAction.WIDGET_TYPE_6, -1, client.getWidget(11, 4).getId(), false);
		if (chat.chatState() == Chatbox.ChatState.SPECIAL)
			targetMenu = new LegacyMenuEntry("Continue", "", 1, MenuAction.CC_OP, 2, client.getWidget(193, 0).getId(), false);
		if (chat.chatState() == Chatbox.ChatState.MODEL)
			targetMenu = new LegacyMenuEntry("Continue", "", 0, MenuAction.WIDGET_TYPE_6, -1, client.getWidget(229, 2).getId(), false);
		utils.doInvokeMsTime(targetMenu, 0);
	}
}