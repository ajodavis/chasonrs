// 
// Decompiled by Procyon v0.5.36
// 

package net.runelite.client.plugins.autovorki;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
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
import net.runelite.client.plugins.iutils.ui.Equipment;
import net.runelite.client.ui.overlay.OverlayManager;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.List;
import java.util.*;
import java.util.stream.IntStream;

@Extension
@PluginDependency(iUtils.class)
@PluginDescriptor(name = "AutoVorki", description = "Kills and loots Vorkath, rebanks at Moonclan", tags = {"chas", "autovorki", "vorkath", "auto"})
@Slf4j
public class AutoVorki extends Plugin {
    static List<Integer> regions;
    boolean withdrawn;
    boolean deposited;
    boolean inInstance;
    boolean looted;
    boolean specced;
    boolean dodgeBomb;
    boolean killSpawn;
    int timeout;
    boolean startVorki;
    boolean attack;
    boolean obtainedPet;
    LegacyMenuEntry targetMenu;
    AutoVorkiState state;
    AutoVorkiState lastState;
    ChatMessage message;
    WorldArea moonclanTele;
    WorldArea moonclanBank;
    WorldPoint moonclanBankTile;
    WorldArea kickedOffIsland;
    WorldArea afterBoat;
    WorldPoint beforeObstacle;
    int goodBanker;
    int badBanker;
    int torfinn;
    int obstacle;
    int vorkathRegion;
    int pool;
    int nexus;
    int kills;
    int lootValue;
    NPC vorkath;
    NPC zombSpawn;
    LocalPoint startLoc;
    String[] excluded;
    List<String> excludedItems = new ArrayList<>();
    String[] included;
    List<String> includedItems = new ArrayList<>();
    List<TileItem> toLoot = new ArrayList<>();
    Instant botTimer;
    @Inject
    OverlayManager overlayManager;
    @Inject
    AutoVorkiConfig config;
    @Inject
    private AutoVorkiOverlay overlay;
    @Inject
    private Client client;
    @Inject
    private ClientThread clientThread;
    @Inject
    private iUtils utils;
    @Inject
    private WalkUtils walk;
    @Inject
    InventoryUtils inv;
    @Inject
    Equipment equip;
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
    BankUtils bank;
    @Inject
    private InterfaceUtils interfaceUtils;
    @Inject
    private NPCUtils npcs;
    @Inject
    private Chatbox chat;
    private Player player;
    private Rectangle bounds;
    private Rectangle prayBounds;
    private Game game;
    int acidX;
    int acidY;
    int steps;
    int safeX;

    public AutoVorki() {
        moonclanTele = new WorldArea(new WorldPoint(2106, 3912, 0), new WorldPoint(2115, 3919, 0));
        moonclanBankTile = new WorldPoint(2099, 3919, 0);
        kickedOffIsland = new WorldArea(new WorldPoint(2628, 3675, 0), new WorldPoint(2635, 3680, 0));
        afterBoat = new WorldArea(new WorldPoint(2277, 4034, 0), new WorldPoint(2279, 4036, 0));
        beforeObstacle = new WorldPoint(2272, 4052, 0);
        regions = Arrays.asList(7513, 7514, 7769, 7770, 8025, 8026);
        goodBanker = 3472;
        badBanker = 3843;
        torfinn = 10405;
        obstacle = 31990;
        vorkathRegion = 9023;
        pool = 29241;
        nexus = 33402;
        vorkath = null;
        inInstance = false;
        startVorki = false;
        looted = true;
        specced = false;
        attack = false;
        dodgeBomb = false;
        killSpawn = false;
        obtainedPet = false;
        kills = 0;
        lootValue = 0;
        toLoot.clear();
        botTimer = null;
        steps = 0;
        safeX = -1;
    }

    public static boolean isInPOH(Client client) {
        IntStream stream = Arrays.stream(client.getMapRegions());
        List<Integer> regions = AutoVorki.regions;
        Objects.requireNonNull(regions);
        IntStream intStream = stream;
        List<Integer> obj = regions;
        Objects.requireNonNull(obj);
        return intStream.anyMatch(obj::contains);
    }

    private void reset() {
        regions = Arrays.asList(7513, 7514, 7769, 7770, 8025, 8026);
        startVorki = false;
        withdrawn = false;
        deposited = false;
        inInstance = false;
        looted = true;
        specced = false;
        attack = false;
        dodgeBomb = false;
        killSpawn = false;
        obtainedPet = false;
        message = null;
        vorkath = null;
        zombSpawn = null;
        kills = 0;
        lootValue = 0;
        state = AutoVorkiState.TIMEOUT;
        lastState = state;
        toLoot.clear();
        overlayManager.remove(overlay);
        botTimer = null;
        steps = 0;
        safeX = -1;
    }

    @Provides
    AutoVorkiConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(AutoVorkiConfig.class);
    }

    protected void startUp() {
    }

    protected void shutDown() {
        reset();
    }

    @Subscribe
    private void onNpcSpawned(NpcSpawned event) {
        if (!startVorki)
            return;

        final NPC npc = event.getNpc();

        if (npc.getName() == null) {
            return;
        }

        if (npc.getName().equals("Vorkath")) {
            vorkath = event.getNpc();
        }
        if (npc.getName().equals("Zombified Spawn")) {
            killSpawn = true;
            zombSpawn = event.getNpc();
        }
    }

    @Subscribe
    private void onNpcDespawned(NpcDespawned event) {
        if (!startVorki)
            return;

        final NPC npc = event.getNpc();

        if (npc.getName() == null) {
            return;
        }

        Widget widget = client.getWidget(10485775);
        if (widget != null) {
            prayBounds = widget.getBounds();
        }

        if (npc.getName().equals("Vorkath")) {
            vorkath = null;

            if (client.getVar(Varbits.QUICK_PRAYER) == 1) {
                LegacyMenuEntry entry = new LegacyMenuEntry("Deactivate", "Quick-prayers", 1, MenuAction.CC_OP.getId(), -1, 10485775, false);
                int sleep = calc.getRandomIntBetweenRange(25, 200);
                utils.doInvokeMsTime(entry, sleep);
            }
        }

        if (npc.getName().equals("Zombified Spawn")) {
            zombSpawn = null;
            killSpawn = false;
            equipWeapons();
        }
    }

    @Subscribe
    private void onAnimationChanged(AnimationChanged event) {
        if (!startVorki)
            return;

        final WorldPoint loc = client.getLocalPlayer().getWorldLocation();
        final LocalPoint localLoc = LocalPoint.fromWorld(client, loc);

        final Actor actor = event.getActor();

        if (actor == player) {
            if (actor.getAnimation() == 7642)
                specced = true;
        }

        if (vorkath != null) {
            if (actor.getAnimation() == 7957 && actor.getName().contains("Vorkath")) { // acid walk
                walkToStart();
                steps = 30;
            }
            if (actor.getAnimation() == 7950 && actor.getName().contains("Vorkath")) {
                Widget widget = client.getWidget(10485775);

                if (widget != null)
                {
                    prayBounds = widget.getBounds();
                }

                if (client.getVar(Varbits.QUICK_PRAYER) == 0) // turn on prayer of it's off
                {
                    LegacyMenuEntry entry = new LegacyMenuEntry("Activate", "Quick-prayers", 1, MenuAction.CC_OP.getId(), -1, 10485775, false);
                    int sleep = calc.getRandomIntBetweenRange(25, 200);
                    utils.doInvokeMsTime(entry, sleep);
                }
            }
            if (actor.getAnimation() == 7949 && actor.getName().contains("Vorkath")) {
                if (client.getVar(Varbits.QUICK_PRAYER) == 1)
                {
                    LegacyMenuEntry entry = new LegacyMenuEntry("Deactivate", "Quick-prayers", 1, MenuAction.CC_OP.getId(), -1, 10485775, false);
                    int sleep = calc.getRandomIntBetweenRange(25, 200);
                    utils.doInvokeMsTime(entry, sleep);
                }
            }
        }
    }

    @Subscribe
    private void onProjectileSpawned(ProjectileSpawned event) {
        if (!startVorki)
            return;

        Projectile projectile = event.getProjectile();

        WorldPoint loc = client.getLocalPlayer().getWorldLocation();

        LocalPoint localLoc = LocalPoint.fromWorld(client, loc);

        if (projectile.getId() == 1481) {// fire bomb
            dodgeBomb = true;
        }
        if (projectile.getId() == 395) {
            int sleep = calc.getRandomIntBetweenRange(25, 200);
            if (config.useStaff() && inv.containsItem(config.staffID()))
                actionItem(config.staffID(), MenuAction.ITEM_SECOND_OPTION);
            startLoc = new LocalPoint(vorkath.getLocalLocation().getX(), vorkath.getLocalLocation().getY() - (4 * 128));
            if (config.invokes())
                walk.walkTile(startLoc.getSceneX(), startLoc.getSceneY());
            else
                walk.sceneWalk(startLoc, 0, 0);
            killSpawn = true;
        }
    }

    @Subscribe
    private void onItemSpawned(ItemSpawned event) {
        if (!startVorki)
            return;

        if (isLootableItem(event.getItem())) {
            toLoot.add(event.getItem());
            if (config.debug())
                utils.sendGameMessage("toLoot added: " + event.getItem().getId() + ", qty: " + event.getItem().getQuantity());
        }
    }

    @Subscribe
    private void onItemDespawned(ItemDespawned event) {
        if (!startVorki)
            return;

        if (toLoot.remove(event.getItem())) {
            int value = utils.getItemPrice(event.getItem().getId(), true) * event.getItem().getQuantity();
            lootValue += value;
        }
        if (toLoot.isEmpty())
            looted = true;
    }

    @Subscribe
    private void onGameStateChanged(GameStateChanged event) {
        if (!startVorki)
            return;

        GameState gamestate = event.getGameState();
        if (gamestate == GameState.LOADING) {

        }
    }

    @Subscribe
    private void onConfigButtonPressed(ConfigButtonClicked configButtonClicked) {
        if (!configButtonClicked.getGroup().equalsIgnoreCase("AutoVorkiConfig")) {
            return;
        }
        switch (configButtonClicked.getKey()) {
            case "startVorki":
                if (!startVorki) {
                    startVorki = true;
                    timeout = 0;
                    state = null;
                    targetMenu = null;
                    obtainedPet = false;
                    botTimer = Instant.now();
                    toLoot.clear();
                    looted = true;
                    lootValue = 0;
                    killSpawn = false;
                    inInstance = false;
                    dodgeBomb = false;
                    kills = 0;
                    excluded = config.excludedItems().toLowerCase().split("\\s*,\\s*");
                    excludedItems.clear();
                    included = config.includedItems().toLowerCase().split("\\s*,\\s*");
                    includedItems.clear();
                    excludedItems.addAll(Arrays.asList(excluded));
                    includedItems.addAll(Arrays.asList(included));
                    overlayManager.add(overlay);
                } else {
                    reset();
                }
                break;
        }
    }

    @Subscribe
    private void onGameTick(GameTick event) {
        if (!startVorki)
            return;

        WorldPoint loc = Objects.requireNonNull(client.getLocalPlayer()).getWorldLocation();
        LocalPoint localLoc = LocalPoint.fromWorld(client, loc);

        int pot = -1;
        player = client.getLocalPlayer();

        if (player != null && client != null) {
            state = getState();
            if (config.debug() && state != lastState && state != AutoVorkiState.TIMEOUT) {
                utils.sendGameMessage("AutoVorkiState: " + state.toString());
            }
            if (state != AutoVorkiState.TIMEOUT)
                lastState = state;
            inInstance = isInVorkath();
            if (inInstance && startLoc != null)
                safeX = getSafeX(startLoc);
            if (player.isMoving() && !inInstance && timeout <= 2 && vorkath == null) {
                timeout = 1;
                return;
            }
            if (!toLoot.isEmpty())
                looted = false;
            if (toLoot.isEmpty())
                looted = true;
            switch (state) {
                case TIMEOUT:
                    if (!bank.isOpen()) {
                        playerUtils.handleRun(30, 20);
                    }
                    --timeout;
                    break;
                case FIND_BANK:
                    openBank();
                    timeout = 2;
                    break;
                case TRAVEL_BANK:
                    if (player.getWorldArea().intersectsWith(moonclanTele)) {
                        walk.sceneWalk(moonclanBankTile, 0, 0);
                        timeout = 8;
                    }
                    break;
                case EQUIP_BGS:
                    targetMenu = new LegacyMenuEntry("Wield", "", 9, MenuAction.CC_OP_LOW_PRIORITY, 0, WidgetInfo.BANK_INVENTORY_ITEMS_CONTAINER.getId(), false);
                    if (!config.invokes())
                        utils.doActionMsTime(targetMenu, inv.getWidgetItem(ItemID.BANDOS_GODSWORD).getCanvasBounds(), 0);
                    else
                        utils.doInvokeMsTime(targetMenu, 0);
                    timeout = 2;
                    deposited = false;
                    break;
                case DEPOSIT_INVENTORY:
                    bank.depositAll();
                    deposited = true;
                    timeout = 3;
                    break;
                case TELE_TO_POH:
                    teleToPoH();
                    break;
                case WITHDRAW_MAGIC_STAFF:
                    withdrawItem(config.staffID());
                    break;
                case WITHDRAW_MAINHAND:
                    withdrawItem(config.mainhandID());
                    break;
                case WITHDRAW_SUPER_COMBAT:
                    withdrawItem(config.superCombat().getDose4());
                    break;
                case WITHDRAW_ANTIFIRE:
                    withdrawItem(config.antifire().getDose4());
                    break;
                case WITHDRAW_ANTIVENOM:
                    withdrawItem(config.antivenom().getDose4());
                    break;
                case WITHDRAW_OFFHAND:
                    withdrawItem(config.offhandID());
                    break;
                case WITHDRAW_PRAYER_RESTORE:
                    withdrawItem(config.prayer().getDose4(), 2);
                    timeout = 1;
                    break;
                case WITHDRAW_RUNE_POUCH:
                    withdrawItem(ItemID.RUNE_POUCH);
                    break;
                case WITHDRAW_SINGLE_FOOD:
                    withdrawItem(config.food().getId());
                    break;
                case WITHDRAW_HOUSE_TELE:
                    if (config.houseTele().getId() == ItemID.TELEPORT_TO_HOUSE)
                        withdrawItem(config.houseTele().getId(), 5);
                    else
                        withdrawItem(config.houseTele().getId());
                    break;
                case WITHDRAW_FOOD_FILL:
                    withdrawAllItem(config.food().getId());
                    break;
                case WITHDRAW_BGS:
                    withdrawItem(ItemID.BANDOS_GODSWORD);
                    break;
                case FINISHED_WITHDRAWING:
                    if (inv.isFull())
                        withdrawn = true;
                    break;
                case TALK_TO_BANKER:
                    actionNPC(badBanker, MenuAction.NPC_FIRST_OPTION);
                    break;
                case DRINK_POOL:
                    actionObject(pool, MenuAction.GAME_OBJECT_FIRST_OPTION);
                    timeout = 5;
                    break;
                case TELEPORT_TO_MOONCLAN:
                    int obj = config.moonClanTele().getObjectID();
                    if (config.moonClanTele() == AutoVorkiConfig.MoonClanTele.CUSTOM)
                        obj = config.cMoonClanTele();
                    actionObject(obj, MenuAction.GAME_OBJECT_FIRST_OPTION);
                    timeout = 5;
                    break;
                case UNHANDLED:
                    utils.sendGameMessage("Unhandled state - stopping");
                    timeout = 2;
                    shutDown();
                    break;
                case PROGRESS_DIALOGUE:
                    continueChat();
                    break;
                case USE_BOAT:
                    actionObject(29917, MenuAction.GAME_OBJECT_FIRST_OPTION);
                    timeout = calc.getRandomIntBetweenRange(6, 8);
                    break;
                case USE_OBSTACLE:
                    actionObject(obstacle, MenuAction.GAME_OBJECT_FIRST_OPTION);
                    if (!inInstance)
                        looted = true;
                    timeout = inInstance ? 0 : (calc.getRandomIntBetweenRange(6, 8));
                    break;
                case POKE_VORKATH:
                    startLoc = new LocalPoint(vorkath.getLocalLocation().getX(), vorkath.getLocalLocation().getY() - (4 * 128));
                    if (!player.getLocalLocation().equals(startLoc)) {
                        walkToStart();
                        timeout = 2;
                        break;
                    }
                    actionNPC(NpcID.VORKATH_8059, MenuAction.NPC_FIRST_OPTION); // 8061
                    acidX = startLoc.getSceneX();
                    acidY = startLoc.getSceneY();
                    timeout = 2;
                    specced = false;
                    attack = true;
                    break;
                case LOOT_VORKATH:
                    if (toLoot != null)
                        lootItem(toLoot);
                    break;
                case SPECIAL_ATTACK:
                    Widget widget = client.getWidget(38862884);

                    if (widget != null) {
                        bounds = widget.getBounds();
                    }

                    if (!config.useBGS())
                        break;
                    if (specced)
                        break;
                    if (!equip.isEquipped(ItemID.BANDOS_GODSWORD) && !inv.isFull())
                        actionItem(ItemID.BANDOS_GODSWORD, MenuAction.ITEM_SECOND_OPTION);
                    else {
                        targetMenu = new LegacyMenuEntry("<col=ff9040>Special Attack</col>", "", 1, MenuAction.CC_OP.getId(), -1, 38862884, false);
                        if (!config.invokes())
                            utils.doActionMsTime(targetMenu, bounds.getBounds(), calc.getRandomIntBetweenRange(25, 200));
                        else
                            utils.doInvokeMsTime(targetMenu, 0);
                        actionNPC(NpcID.VORKATH_8061, MenuAction.NPC_SECOND_OPTION); // 8061
                    }
                    break;
                case ACID_WALK:
                    acidX = startLoc.getSceneX();
                    acidY = startLoc.getSceneY();
                    if (steps >= 0) {
                        if (steps == 1) {
                            actionNPC(vorkath.getId(), MenuAction.NPC_SECOND_OPTION);
                        } else if (steps == 2) {
                            walkToStart();
                        } else if (steps > 2) {
                            if (steps % 2 == 0) {
                                    actionNPC(vorkath.getId(), MenuAction.NPC_SECOND_OPTION);
                            } else { // move back here
                                if (safeX == -1) {
                                    utils.sendGameMessage("Unable to find suitable walk path");
                                    if (config.houseTele().getId() == ItemID.CONSTRUCT_CAPET || config.houseTele().getId() == ItemID.CONSTRUCT_CAPE)
                                        actionItem(ItemID.CONSTRUCT_CAPET, MenuAction.ITEM_FOURTH_OPTION);
                                    else if (config.houseTele().getId() == ItemID.TELEPORT_TO_HOUSE)
                                        actionItem(ItemID.TELEPORT_TO_HOUSE, MenuAction.ITEM_FIRST_OPTION);
                                    timeout = calc.getRandomIntBetweenRange(6, 8);
                                    withdrawn = false;
                                    deposited = false;
                                    steps = 0;
                                    break;
                                }
                                if (config.invokes()) {
                                    if (client.getBoostedSkillLevel(Skill.HITPOINTS) <= config.eatAt())
                                        eatFood();
                                    if (client.getBoostedSkillLevel(Skill.PRAYER) <= config.restoreAt())
                                        drinkPrayer();
                                    else if (config.drinkAntifire() && needsAntifire())
                                        drinkAntifire();
                                    else if (config.drinkAntivenom() && needsAntivenom())
                                        drinkAntivenom();
                                    else if (needsRepot())
                                        drinkSuperCombat();
                                    walk.walkTile(safeX, startLoc.getSceneY() - 1);
                                } else {
                                    walk.sceneWalk(new LocalPoint(safeX * 128, startLoc.getY() - 128), 0, 0);
                                }
                            }
                        }
                    }
                    if (steps != 0)
                        steps--;
                    if (steps < 0)
                        steps = 0;
                    break;
                case KILL_SPAWN:
                    if (zombSpawn != null) {
                        targetMenu =  new LegacyMenuEntry("Cast", "", zombSpawn.getIndex(), MenuAction.SPELL_CAST_ON_NPC.getId(), 0, 0, false);
                        utils.oneClickCastSpell(WidgetInfo.SPELL_CRUMBLE_UNDEAD, targetMenu, zombSpawn.getConvexHull().getBounds(), 100);
                        killSpawn = false;
                        timeout = 10;
                    }
                    break;
                case DODGE_BOMB:
                    if (config.debug())
                        utils.sendGameMessage("dodging bomb");
                    assert localLoc != null;
                    if (localLoc.getX() < 6208) {
                        if (config.invokes())
                            walk.walkTile(localLoc.getSceneX() + 2, localLoc.getSceneY());
                        else
                            walk.sceneWalk(new LocalPoint(localLoc.getX() + 256, localLoc.getY()), 0, 0);
                    } else {
                        if (config.invokes())
                            walk.walkTile(localLoc.getSceneX() - 2, localLoc.getSceneY());
                        else
                            walk.sceneWalk(new LocalPoint(localLoc.getX() - 256, localLoc.getY()), 0, 0);
                    }
                    attack = true;
                    dodgeBomb = false;
                    timeout = 1;
                    break;
                case EAT_FOOD:
                    eatFood();
                    break;
                case DRINK_PRAYER:
                    drinkPrayer();
                    attack = true;
                    break;
                case DRINK_ANTIFIRE:
                    drinkAntifire();
                    attack = true;
                    break;
                case DRINK_ANTIVENOM:
                    drinkAntivenom();
                    attack = true;
                    break;
                case DRINK_SUPER_COMBAT:
                    drinkSuperCombat();
                    attack = true;
                    break;
                case ENABLE_PRAYER:
                    LegacyMenuEntry entry = new LegacyMenuEntry("Activate", "Quick-prayers", 1, MenuAction.CC_OP.getId(), -1, 10485775, false);
                    int sleep = calc.getRandomIntBetweenRange(25, 200);
                    utils.doInvokeMsTime(entry, sleep);
                    break;
            }
        }
    }


    int getSafeX(LocalPoint startLoc) {
        int safeX = -1;
        if (objectUtils.getGameObjectAtLocalPoint(new LocalPoint(startLoc.getX(), startLoc.getY())) == null
                && objectUtils.getGameObjectAtLocalPoint(new LocalPoint(startLoc.getX(), startLoc.getY() - 128)) == null) {
            safeX = startLoc.getSceneX();
        } else if (objectUtils.getGameObjectAtLocalPoint(new LocalPoint(startLoc.getX() - 128, startLoc.getY())) == null
                && objectUtils.getGameObjectAtLocalPoint(new LocalPoint(startLoc.getX() - 128, startLoc.getY() - 128)) == null) {
            safeX = startLoc.getSceneX() - 1;
        } else if (objectUtils.getGameObjectAtLocalPoint(new LocalPoint(startLoc.getX() + 128, startLoc.getY())) == null
                && objectUtils.getGameObjectAtLocalPoint(new LocalPoint(startLoc.getX() + 128, startLoc.getY() - 128)) == null) {
            safeX = startLoc.getSceneX() + 1;
        } else if (objectUtils.getGameObjectAtLocalPoint(new LocalPoint(startLoc.getX() - 256, startLoc.getY())) == null
                && objectUtils.getGameObjectAtLocalPoint(new LocalPoint(startLoc.getX() - 256, startLoc.getY() - 128)) == null) {
            safeX = startLoc.getSceneX() - 2;
        } else if (objectUtils.getGameObjectAtLocalPoint(new LocalPoint(startLoc.getX() + 256, startLoc.getY())) == null
                && objectUtils.getGameObjectAtLocalPoint(new LocalPoint(startLoc.getX() + 256, startLoc.getY() - 128)) == null) {
            safeX = startLoc.getSceneX() + 2;
        }
        return safeX;
    }

    @Subscribe
    private void onChatMessage(ChatMessage event) {
        if (!startVorki)
            return;
        if (event.getType() == ChatMessageType.CONSOLE) {
            return;
        }

        Widget widget = client.getWidget(10485775);

        if (widget != null)
        {
            prayBounds = widget.getBounds();
        }

        String prayerMessage = "Your prayers have been disabled!";
        String spawnExplode = "The spawn violently explodes, unfreezing you as it does so.";
        String unfrozenMessage = "You become unfrozen as you kill the spawn.";
        String deathMessage = "Oh dear, you are dead!";
        String killComplete = "Your Vorkath";
        String petDrop = "You have a funny feeling like you're being followed.";

        if (event.getMessage().equals(prayerMessage) && client.getVar(Varbits.QUICK_PRAYER) == 0 ) {
            LegacyMenuEntry entry = new LegacyMenuEntry("Activate", "Quick-prayers", 1, MenuAction.CC_OP.getId(), -1, 10485775, false);
            int sleep = calc.getRandomIntBetweenRange(25, 200);
            utils.doInvokeMsTime(entry, sleep);
        } else if (event.getMessage().equals(spawnExplode) || (event.getMessage().equals(unfrozenMessage))) {
            killSpawn = false;
            zombSpawn = null;
            timeout = 0;
            equipWeapons();
        } else if (event.getMessage().contains(killComplete)) {
            kills++;
            equipWeapons();
            steps = 0;
            dodgeBomb = false;
            zombSpawn = null;
            killSpawn = false;
            looted = false;
            specced = false;
            timeout = 2;
            //walk.sceneWalk(startLoc, 2, calc.getRandomIntBetweenRange(25, 200));
        } else if (event.getMessage().equals(deathMessage)) {
            timeout = 2;
            utils.sendGameMessage("AutoVorki: Stopping because we died.");
            utils.sendGameMessage("AutoVorki: Last state:" + lastState.toString() + ".");
            SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
            Date date = new Date();
            utils.sendGameMessage("Died at: " + format.format(date));
            reset();
            shutDown();
        } else if (event.getMessage().equals(petDrop)) {
            obtainedPet = true;
        }
    }

    boolean chatboxIsOpen() {
        return chat.chatState() == Chatbox.ChatState.NPC_CHAT || chat.chatState() == Chatbox.ChatState.PLAYER_CHAT;
    }

    private void continueChat() {
        targetMenu = null;
        if (chat.chatState() == Chatbox.ChatState.NPC_CHAT) {
            targetMenu = new LegacyMenuEntry("Continue", "", 0, MenuAction.WIDGET_TYPE_6, -1, client.getWidget(231, 5).getId(), false);
            bounds = client.getWidget(231, 5).getBounds();
        }
        if (chat.chatState() == Chatbox.ChatState.PLAYER_CHAT) {
            targetMenu = new LegacyMenuEntry("Continue", "", 0, MenuAction.WIDGET_TYPE_6, -1, client.getWidget(217, 5).getId(), false);
            bounds = client.getWidget(217, 5).getBounds();
        }
        if (!config.invokes())
            utils.doActionMsTime(targetMenu, bounds, 0);
        else
            utils.doInvokeMsTime(targetMenu, 0);
    }

    AutoVorkiState getState() {
        if (dodgeBomb && timeout > 0)
            return AutoVorkiState.DODGE_BOMB;
        if (timeout > 0) {
            return AutoVorkiState.TIMEOUT;
        }
        if (chatboxIsOpen())
            return AutoVorkiState.PROGRESS_DIALOGUE;
        if (bank.isOpen())
            return getBankState();
        return getStates();
    }

    AutoVorkiState getStates() {
        if (!inInstance) {
            if (isInPOH(client)) {
                if (client.getBoostedSkillLevel(Skill.HITPOINTS) < client.getRealSkillLevel(Skill.HITPOINTS)
                        || client.getBoostedSkillLevel(Skill.PRAYER) < client.getRealSkillLevel(Skill.PRAYER)
                        || client.getVar(VarPlayer.SPECIAL_ATTACK_PERCENT) < 1000) {
                    return AutoVorkiState.DRINK_POOL;
                }
                return AutoVorkiState.TELEPORT_TO_MOONCLAN;
            } else if (!inv.isFull()) {
                if (player.getWorldLocation().equals(moonclanBankTile)) {
                    deposited = false;
                    withdrawn = false;
                    return AutoVorkiState.FIND_BANK;
                }
                if (player.getWorldArea().intersectsWith(moonclanTele)) {
                    return AutoVorkiState.TRAVEL_BANK;
                }
                if (inv.containsItem(config.houseTele().getId())) {
                    return AutoVorkiState.TELE_TO_POH;
                }
                return AutoVorkiState.TIMEOUT;
            } else {
                if (inv.isFull() && player.getWorldArea().intersectsWith(moonclanTele))
                    return AutoVorkiState.TRAVEL_BANK;
                if (inv.isFull() && player.getWorldLocation().equals(moonclanBankTile))
                    return AutoVorkiState.FIND_BANK;
                if (player.getWorldArea().intersectsWith(kickedOffIsland))
                    return AutoVorkiState.USE_BOAT;
                if (player.getWorldArea().intersectsWith(afterBoat))
                    return AutoVorkiState.USE_OBSTACLE;
            }
        } else { // is in instance
            if (vorkath != null) {
                if (vorkath.getId() == NpcID.VORKATH_8059 && !looted && !toLoot.isEmpty() && !inv.isFull())
                    return AutoVorkiState.LOOT_VORKATH;
                if (vorkath.getId() == NpcID.VORKATH_8059 && !looted && !toLoot.isEmpty() && inv.isFull() && config.eatLoot())
                    return AutoVorkiState.EAT_FOOD;
                if (obtainedPet) {
                    if (config.houseTele().getId() == ItemID.CONSTRUCT_CAPET || config.houseTele().getId() == ItemID.CONSTRUCT_CAPE)
                        actionItem(ItemID.CONSTRUCT_CAPET, MenuAction.ITEM_FOURTH_OPTION);
                    else if (config.houseTele().getId() == ItemID.TELEPORT_TO_HOUSE)
                        actionItem(ItemID.TELEPORT_TO_HOUSE, MenuAction.ITEM_FIRST_OPTION);
                    shutDown();
                }

                if (dodgeBomb)
                    return AutoVorkiState.DODGE_BOMB;
                if (killSpawn)
                    return AutoVorkiState.KILL_SPAWN;
                if (steps > 0)
                    return AutoVorkiState.ACID_WALK;
                if (client.getVar(Varbits.QUICK_PRAYER) == 0)
                    return AutoVorkiState.ENABLE_PRAYER;

                if (client.getBoostedSkillLevel(Skill.HITPOINTS) <= config.eatAt()) {
                    if (inv.containsItem(config.food().getId())) {
                        eatFood();
                        attack = true;
                    } else
                        return AutoVorkiState.TELE_TO_POH;
                }
                if (client.getBoostedSkillLevel(Skill.PRAYER) <= config.restoreAt())
                    return AutoVorkiState.DRINK_PRAYER;
                if (config.drinkAntifire() && needsAntifire())
                    return AutoVorkiState.DRINK_ANTIFIRE;
                if (config.drinkAntivenom() && needsAntivenom())
                    return AutoVorkiState.DRINK_ANTIVENOM;
                if (needsRepot())
                    return AutoVorkiState.DRINK_SUPER_COMBAT;

                if (vorkath.getId() == NpcID.VORKATH_8059 && looted
                        && inv.containsItemAmount(config.food().getId(), 4, false, false))
                    return AutoVorkiState.POKE_VORKATH;
                if (vorkath.getId() == NpcID.VORKATH_8059 && looted
                        && inv.getItemCount(config.food().getId(), false) < 4)
                    return AutoVorkiState.TELE_TO_POH;
                if (vorkath.getId() == NpcID.VORKATH_8061) {
                    if (client.getVar(VarPlayer.SPECIAL_ATTACK_PERCENT) >= 500 && !specced && config.useBGS()) {
                        if (!equip.isEquipped(ItemID.BANDOS_GODSWORD) && inv.isFull())
                            actionItem(config.food().getId(), MenuAction.ITEM_FIRST_OPTION);
                        if (client.getVar(VarPlayer.SPECIAL_ATTACK_ENABLED) == 0)
                            return AutoVorkiState.SPECIAL_ATTACK;
                    } else {
                        equipWeapons();
                    }
                }
                if (attack) {
                    equipWeapons();
                }
            }
        }
        return AutoVorkiState.TIMEOUT;
    }

    AutoVorkiState getBankState() {
        if (bank.isOpen()) {
            if (!deposited && !withdrawn) {
                return AutoVorkiState.DEPOSIT_INVENTORY;
            }
            if (deposited && !withdrawn) {
                if (config.useBGS() && inv.containsItem(ItemID.BANDOS_GODSWORD) && !equip.isEquipped(ItemID.BANDOS_GODSWORD) && !inv.isFull()) {
                    return AutoVorkiState.EQUIP_BGS;
                }
                if (config.useBGS() && !inv.containsItem(ItemID.BANDOS_GODSWORD) && !equip.isEquipped(ItemID.BANDOS_GODSWORD)) {
                    return AutoVorkiState.WITHDRAW_BGS;
                }
                if (!inv.containsItem(config.mainhandID()) && !equip.isEquipped(config.mainhandID())) {
                    return AutoVorkiState.WITHDRAW_MAINHAND;
                }
                if (!inv.containsItem(config.superCombat().getDose4())) {
                    return AutoVorkiState.WITHDRAW_SUPER_COMBAT;
                }
                if (!inv.containsItem(config.antifire().getDose4())) {
                    return AutoVorkiState.WITHDRAW_ANTIFIRE;
                }
                if (!inv.containsItem(config.antivenom().getDose4()) && config.antivenom().getDose4() != ItemID.SERPENTINE_HELM) {
                    return AutoVorkiState.WITHDRAW_ANTIVENOM;
                }
                if (!inv.containsItem(config.offhandID()) && !equip.isEquipped(config.offhandID())) {
                    return AutoVorkiState.WITHDRAW_OFFHAND;
                }
                if (inv.getItemCount(config.prayer().getDose4(), false) == 0) {
                    return AutoVorkiState.WITHDRAW_PRAYER_RESTORE;
                }
                if (!inv.containsItem(ItemID.RUNE_POUCH)) {
                    return AutoVorkiState.WITHDRAW_RUNE_POUCH;
                }
                if (config.useStaff() && !inv.containsItem(config.staffID()) && !equip.isEquipped(config.staffID())) {
                    return AutoVorkiState.WITHDRAW_MAGIC_STAFF;
                }
                if (!config.useStaff() && inv.getItemCount(config.food().getId(), false) == 0)
                    return AutoVorkiState.WITHDRAW_SINGLE_FOOD;
                if (inv.getItemCount(config.prayer().getDose4(), false) == 2) {
                    return AutoVorkiState.WITHDRAW_PRAYER_RESTORE;
                }
                if (!inv.containsItem(config.houseTele().getId())) {
                    return AutoVorkiState.WITHDRAW_HOUSE_TELE;
                }
                if (!inv.isFull()) {
                    return AutoVorkiState.WITHDRAW_FOOD_FILL;
                }
                return AutoVorkiState.FINISHED_WITHDRAWING;
            } else if (deposited && withdrawn && inv.getItemCount(config.food().getId(), false) >= 4) {
                return AutoVorkiState.TALK_TO_BANKER;
            } else {
                return AutoVorkiState.DEPOSIT_INVENTORY;
            }
        }
        return AutoVorkiState.TIMEOUT;
    }

    void eatFood() {
        actionItem(config.food().getId(), MenuAction.ITEM_FIRST_OPTION);
    }

    void equipWeapons() {
        if (!equip.isEquipped(config.mainhandID()) && timeout <= 1)
            actionItem(config.mainhandID(), MenuAction.ITEM_SECOND_OPTION, 0);
        if (!equip.isEquipped(config.offhandID()) && timeout <= 1)
            actionItem(config.offhandID(), MenuAction.ITEM_SECOND_OPTION, 0);
        if (attack) {
            actionNPC(NpcID.VORKATH_8061, MenuAction.NPC_SECOND_OPTION); // 8061
            attack = false;
        }
    }

    void drinkSuperCombat() {
        int pot = -1;
        if (inv.containsItem(config.superCombat().getDose4()))
            pot = config.superCombat().getDose4();
        if (inv.containsItem(config.superCombat().getDose3()))
            pot = config.superCombat().getDose3();
        if (inv.containsItem(config.superCombat().getDose2()))
            pot = config.superCombat().getDose2();
        if (inv.containsItem(config.superCombat().getDose1()))
            pot = config.superCombat().getDose1();
        if (pot == -1) {
            teleToPoH();
            return;
        }
        actionItem(pot, MenuAction.ITEM_FIRST_OPTION);
    }

    void drinkAntivenom() {
        int pot = -1;
        if (equip.isEquipped(ItemID.SERPENTINE_HELM))
            return;
        if (inv.containsItem(config.antivenom().getDose4()))
            pot = config.antivenom().getDose4();
        if (inv.containsItem(config.antivenom().getDose3()))
            pot = config.antivenom().getDose3();
        if (inv.containsItem(config.antivenom().getDose2()))
            pot = config.antivenom().getDose2();
        if (inv.containsItem(config.antivenom().getDose1()))
            pot = config.antivenom().getDose1();
        if (pot == -1) {
            teleToPoH();
            return;
        }
        actionItem(pot, MenuAction.ITEM_FIRST_OPTION);
    }

    void drinkAntifire() {
        int pot = -1;
        if (inv.containsItem(config.antifire().getDose4()))
            pot = config.antifire().getDose4();
        if (inv.containsItem(config.antifire().getDose3()))
            pot = config.antifire().getDose3();
        if (inv.containsItem(config.antifire().getDose2()))
            pot = config.antifire().getDose2();
        if (inv.containsItem(config.antifire().getDose1()))
            pot = config.antifire().getDose1();
        if (pot == -1) {
            teleToPoH();
            return;
        }
        actionItem(pot, MenuAction.ITEM_FIRST_OPTION);
    }

    void drinkPrayer() {
        int pot = -1;
        if (inv.containsItem(config.prayer().getDose4()))
            pot = config.prayer().getDose4();
        if (inv.containsItem(config.prayer().getDose3()))
            pot = config.prayer().getDose3();
        if (inv.containsItem(config.prayer().getDose2()))
            pot = config.prayer().getDose2();
        if (inv.containsItem(config.prayer().getDose1()))
            pot = config.prayer().getDose1();
        if (pot == -1) {
            teleToPoH();
            return;
        }
        actionItem(pot, MenuAction.ITEM_FIRST_OPTION);
    }

    private boolean needsAntifire() {
        int varbit = 0;
        if (config.antifire().name() == AutoVorkiConfig.Antifire.SUPER_ANTIFIRE.name()
                || config.antifire().name() == AutoVorkiConfig.Antifire.EXT_SUPER_ANTIFIRE.name())
            varbit = 6101;
        else
            varbit = 3981;
        return client.getVarbitValue(varbit) == 0;
    }

    private boolean needsAntivenom() {
        if (equip.isEquipped(ItemID.SERPENTINE_HELM))
            return false;
        return client.getVarpValue(VarPlayer.POISON.getId()) > 0;
    }

    private boolean needsRepot() {
        int real = client.getRealSkillLevel(Skill.STRENGTH);
        int boost = client.getBoostedSkillLevel(Skill.STRENGTH);
        int repot = config.boostLevel();
        return boost <= (real + repot);
    }

    private void openBank() {
        actionNPC(goodBanker, MenuAction.NPC_THIRD_OPTION);
    }

    private void walkToStart() {
        if (vorkath != null)
            startLoc = new LocalPoint(vorkath.getLocalLocation().getX(), vorkath.getLocalLocation().getY() - (4 * 128));
        if (!config.invokes())
            walk.sceneWalk(new LocalPoint(startLoc.getX(), startLoc.getY()), 0, 0);
        else
            walk.walkTile(startLoc.getSceneX(), startLoc.getSceneY());
    }

    private void withdrawItem(int id, int qty) {
        if (qty <= 0)
            qty = 1;
        bank.withdrawItemAmount(id, qty);
    }

    private void withdrawItem(int id) {
        withdrawItem(id, 1);
    }

    private void withdrawAllItem(int id) {
        bank.withdrawAllItem(id);
    }

    private boolean isInVorkath() {
        boolean inside = false;
        if (vorkath != null) {
            inside = vorkath.getId() != NpcID.VORKATH_8058
                    || vorkath.getAnimation() != -1;
        }
        return inside;
    }

    private int calculateHealth(NPC target, Integer maxHealth) {
        if (target == null || target.getName() == null) {
            return -1;
        }
        int healthScale = target.getHealthScale();
        int healthRatio = target.getHealthRatio();
        if (healthRatio < 0 || healthScale <= 0 || maxHealth == null) {
            return -1;
        }
        return (int) (maxHealth * healthRatio / healthScale + 0.5f);
    }

    private void teleToPoH() {
        if (config.houseTele().getId() == ItemID.CONSTRUCT_CAPET || config.houseTele().getId() == ItemID.CONSTRUCT_CAPE)
            actionItem(ItemID.CONSTRUCT_CAPET, MenuAction.ITEM_FOURTH_OPTION);
        else if (config.houseTele().getId() == ItemID.TELEPORT_TO_HOUSE)
            actionItem(ItemID.TELEPORT_TO_HOUSE, MenuAction.ITEM_FIRST_OPTION);
        timeout = calc.getRandomIntBetweenRange(6, 8);
        withdrawn = false;
        deposited = false;
    }

    boolean isLootableItem(TileItem item) {
        String name = client.getItemDefinition(item.getId()).getName().toLowerCase();
        int value = utils.getItemPrice(item.getId(), true) * item.getQuantity();
        if (includedItems.stream().anyMatch(name.toLowerCase()::contains))
            return true;
        if (excludedItems.stream().anyMatch(name.toLowerCase()::contains))
            return false;
        if (name.equalsIgnoreCase("superior dragon bones") && config.lootBones())
            return true;
        return value >= config.lootValue();
    }

    void lootItem(List<TileItem> itemList) {
        TileItem lootItem = this.getNearestTileItem(itemList);
        if (lootItem != null) {
            this.clientThread.invoke(() -> this.client.invokeMenuAction("", "", lootItem.getId(), MenuAction.GROUND_ITEM_THIRD_OPTION.getId(), lootItem.getTile().getSceneLocation().getX(), lootItem.getTile().getSceneLocation().getY()));
        }
    }

    private TileItem getNearestTileItem(List<TileItem> tileItems) {
        int currentDistance;
        TileItem closestTileItem = tileItems.get(0);
        int closestDistance = closestTileItem.getTile().getWorldLocation().distanceTo(player.getWorldLocation());
        for (TileItem tileItem : tileItems) {
            currentDistance = tileItem.getTile().getWorldLocation().distanceTo(player.getWorldLocation());
            if (currentDistance < closestDistance) {
                closestTileItem = tileItem;
                closestDistance = currentDistance;
            }
        }
        return closestTileItem;
    }

    private boolean actionObject(int id, MenuAction action) {
        GameObject obj = objectUtils.findNearestGameObject(id);
        if (obj != null) {
            targetMenu = new LegacyMenuEntry("", "", obj.getId(), action, obj.getSceneMinLocation().getX(), obj.getSceneMinLocation().getY(), false);
            if (!config.invokes())
                utils.doGameObjectActionMsTime(obj, action.getId(), calc.getRandomIntBetweenRange(25, 300));
            else
                utils.doInvokeMsTime(targetMenu, 0);
            return true;
        }
        return false;
    }

    private boolean actionItem(int id, MenuAction action, int delay) {
        if (inv.containsItem(id)) {
            WidgetItem item = inv.getWidgetItem(id);
            targetMenu = new LegacyMenuEntry("", "", item.getId(), action, item.getIndex(), WidgetInfo.INVENTORY.getId(), false);
            if (!config.invokes())
                utils.doActionMsTime(targetMenu, item.getCanvasBounds(), delay);
            else
                utils.doInvokeMsTime(targetMenu, 0);
            return true;
        }
        return false;
    }

    private boolean actionItem(int id, MenuAction action) {
        return actionItem(id, action, 0);
    }

    private boolean actionNPC(int id, MenuAction action, int delay) {
        NPC target = npcs.findNearestNpc(id);
        if (target != null) {
            targetMenu = new LegacyMenuEntry("", "", target.getIndex(), action, target.getIndex(), 0, false);
            if (!config.invokes())
                utils.doNpcActionMsTime(target, action.getId(), delay);
            else
                utils.doInvokeMsTime(targetMenu, 0);
            return true;
        }
        return false;
    }

    private boolean actionNPC(int id, MenuAction action) {
        return actionNPC(id, action, 300);
    }
}