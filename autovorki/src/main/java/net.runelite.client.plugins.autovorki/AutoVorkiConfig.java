/*
 * Copyright (c) 2018, Andrew EP | ElPinche256 <https://github.com/ElPinche256>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.autovorki;

import lombok.Getter;
import net.runelite.api.ItemID;
import net.runelite.api.ObjectID;
import net.runelite.client.config.*;

@ConfigGroup("AutoVorkiConfig")
public interface AutoVorkiConfig extends Config {
    @ConfigItem(keyName = "startVorki", name = "Start/Stop", description = "", position = 0, title = "startVorki")
    default Button startVorki() {
        return new Button();
    }

    @ConfigItem(keyName = "showOverlay", name = "Show UI", description = "Show the UI on screen", position = 1)
    default boolean showOverlay() {
        return true;
    }

    @ConfigItem(keyName = "mainhandID", name = "Mainhand ID", description = "The item ID of your weapon slot", position = 2)
    default int mainhandID() {
        return ItemID.DRAGON_HUNTER_LANCE;
    }

    @ConfigItem(keyName = "offhandID", name = "Offhand ID", description = "The item ID of your shield slot", position = 3)
    default int offhandID() {
        return ItemID.AVERNIC_DEFENDER;
    }

    @ConfigItem(keyName = "useBGS", name = "Use BGS spec", description = "", position = 4)
    default boolean useBGS() {
        return true;
    }

    @ConfigItem(keyName = "useStaff", name = "Equip staff", description = "Equip a staff for crumble undead<br>Useful if your magic attack is too low", position = 5)
    default boolean useStaff() {
        return true;
    }

    @ConfigItem(keyName = "staffID", name = "Staff ID", description = "The item ID of your magic weapon", position = 6, hidden = true, unhide = "useStaff")
    default int staffID() {
        return ItemID.SANGUINESTI_STAFF;
    }

    @ConfigItem(keyName = "foodID", name = "Food", description = "The name of your food", position = 7)
    default Food food() {
        return Food.ANGLERFISH;
    }

    @Range(min = 1, max = 99)
    @ConfigItem(keyName = "eatAt", name = "Eat at", description = "Eat food when under this HP", position = 8)
    default int eatAt() {
        return 35;
    }

    @ConfigItem(keyName = "prayerID", name = "Prayer restore", description = "The name of your prayer restore", position = 9)
    default Prayer prayer() {
        return Prayer.PRAYER_POTION;
    }

    @Range(min = 1, max = 99)
    @ConfigItem(keyName = "restoreAt", name = "Drink prayer at", description = "Drink prayer restore when under this amount of prayer", position = 10)
    default int restoreAt() {
        return 20;
    }

    @ConfigItem(keyName = "antifireID", name = "Antifire", description = "The name of your antifire potion", position = 11)
    default Antifire antifire() {
        return Antifire.EXT_SUPER_ANTIFIRE;
    }

    @ConfigItem(keyName = "drinkAntifire", name = "Drink antifire", description = "Automatically drink antifire", position = 12)
    default boolean drinkAntifire() {
        return true;
    }

    @ConfigItem(keyName = "antivenomID", name = "Antivenom", description = "The name of your antivenom potion", position = 13)
    default Antivenom antivenom() {
        return Antivenom.ANTI_VENOM_PLUS;
    }

    @ConfigItem(keyName = "drinkAntivenom", name = "Drink antivenom", description = "Automatically drink antivenom<br>Ignore if using serpentine helm", position = 14)
    default boolean drinkAntivenom() {
        return true;
    }

    @ConfigItem(keyName = "houseTele", name = "PoH", description = "The name of your house teleport", position = 15)
    default HouseTele houseTele() {
        return HouseTele.CONSTRUCTION_CAPE_T;
    }

    @ConfigItem(keyName = "moonClanTele", name = "MoonClan", description = "The name of your moonclan teleport", position = 16)
    default MoonClanTele moonClanTele() {
        return MoonClanTele.PORTAL_NEXUS;
    }

    @ConfigItem(keyName = "cMoonClanTele", name = "Object ID", description = "Object ID for custom Moonclan Teleport", position = 17)
    default int cMoonClanTele() {
        return 0;
    }

    @ConfigItem(keyName = "superCombatID", name = "Boost", description = "The name of your super combat pot", position = 18)
    default SuperCombat superCombat() {
        return SuperCombat.DIVINE_SUPER_COMBAT;
    }

    @ConfigItem(keyName = "boostLevel", name = "Re-boost at", description = "The level to drink a super combat pot at", position = 19)
    default int boostLevel() {
        return 10;
    }

    @ConfigItem(keyName = "lootBones", name = "Loot Superior dragon bones", description = "", position = 994)
    default boolean lootBones() {
        return true;
    }

    @ConfigItem(keyName = "eatLoot", name = "Eat food to loot", description = "", position = 995)
    default boolean eatLoot() {
        return true;
    }

    @ConfigItem(keyName = "lootValue", name = "Item value to loot", description = "Loot items over this value", position = 996)
    default int lootValue() {
        return 25000;
    }

    @ConfigItem(keyName = "includedItems", name = "Included items", description = "Full or partial names of items to loot regardless of value<br>Seperate with a comma", position = 997)
    default String includedItems() {
        return "rune longsword";
    }

    @ConfigItem(keyName = "excludedItems", name = "Excluded items", description = "Full or partial names of items to NOT loot<br>Seperate with a comma", position = 998)
    default String excludedItems() {
        return "ruby bolt,diamond bolt,emerald bolt,dragonstone bolt";
    }

    @ConfigItem(keyName = "invokes", name = "Use invokes (use with caution)", description = "Use at your own risk :)", position = 998)
    default boolean invokes() {
        return false;
    }

    @ConfigItem(keyName = "debug", name = "Debug Messages", description = "", position = 999)
    default boolean debug() {
        return false;
    }

    enum Food {
        MANTA_RAY(ItemID.MANTA_RAY),
        TUNA_POTATO(ItemID.TUNA_POTATO),
        DARK_CRAB(ItemID.DARK_CRAB),
        ANGLERFISH(ItemID.ANGLERFISH),
        SEA_TURTLE(ItemID.SEA_TURTLE),
        MUSHROOM_POTATO(ItemID.MUSHROOM_POTATO),
        SHARK(ItemID.SHARK),
        COOKED_KARAMBWAN(ItemID.COOKED_KARAMBWAN);
        @Getter
        private final int id;

        Food(int id) {
            this.id = id;
        }
    }

    enum Prayer {
        PRAYER_POTION(ItemID.PRAYER_POTION4, ItemID.PRAYER_POTION3, ItemID.PRAYER_POTION2, ItemID.PRAYER_POTION1),
        SUPER_RESTORE(ItemID.SUPER_RESTORE4, ItemID.SUPER_RESTORE3, ItemID.SUPER_RESTORE2, ItemID.SUPER_RESTORE1);

        @Getter
        private final int dose4, dose3, dose2, dose1;

        Prayer(int dose4, int dose3, int dose2, int dose1) {
            this.dose1 = dose1;
            this.dose2 = dose2;
            this.dose3 = dose3;
            this.dose4 = dose4;
        }
    }

    enum Antifire {
        ANTIFIRE_POTION(ItemID.ANTIFIRE_POTION4, ItemID.ANTIFIRE_POTION3, ItemID.ANTIFIRE_POTION2, ItemID.ANTIFIRE_POTION1),
        EXT_ANTIFIRE_POTION(ItemID.EXTENDED_ANTIFIRE4, ItemID.EXTENDED_ANTIFIRE3, ItemID.EXTENDED_ANTIFIRE2, ItemID.EXTENDED_ANTIFIRE1),
        SUPER_ANTIFIRE(ItemID.SUPER_ANTIFIRE_POTION4, ItemID.SUPER_ANTIFIRE_POTION3, ItemID.SUPER_ANTIFIRE_POTION2, ItemID.SUPER_ANTIFIRE_POTION1),
        EXT_SUPER_ANTIFIRE(ItemID.EXTENDED_SUPER_ANTIFIRE4, ItemID.EXTENDED_SUPER_ANTIFIRE3, ItemID.EXTENDED_SUPER_ANTIFIRE2, ItemID.EXTENDED_SUPER_ANTIFIRE1);

        @Getter
        private final int dose4, dose3, dose2, dose1;

        Antifire(int dose4, int dose3, int dose2, int dose1) {
            this.dose1 = dose1;
            this.dose2 = dose2;
            this.dose3 = dose3;
            this.dose4 = dose4;
        }
    }

    enum Antivenom {
        ANTI_VENOM(ItemID.ANTIVENOM4, ItemID.ANTIVENOM3, ItemID.ANTIVENOM2, ItemID.ANTIVENOM1),
        ANTI_VENOM_PLUS(ItemID.ANTIVENOM4_12913, ItemID.ANTIVENOM3_12915, ItemID.ANTIVENOM2_12917, ItemID.ANTIVENOM1_12919),
        SERPENTINE_HELM(ItemID.SERPENTINE_HELM, -1, -1, -1);

        @Getter
        private final int dose4, dose3, dose2, dose1;

        Antivenom(int dose4, int dose3, int dose2, int dose1) {
            this.dose1 = dose1;
            this.dose2 = dose2;
            this.dose3 = dose3;
            this.dose4 = dose4;
        }
    }

    enum HouseTele {
        CONSTRUCTION_CAPE_T(ItemID.CONSTRUCT_CAPET),
        CONSTRUCTION_CAPE(ItemID.CONSTRUCT_CAPE),
        HOUSE_TABLET(ItemID.TELEPORT_TO_HOUSE);

        @Getter
        private final int id;

        HouseTele(int id) {
            this.id = id;
        }
    }

    enum SuperCombat {
        DIVINE_SUPER_COMBAT(ItemID.DIVINE_SUPER_COMBAT_POTION4, ItemID.DIVINE_SUPER_COMBAT_POTION3, ItemID.DIVINE_SUPER_COMBAT_POTION2, ItemID.DIVINE_SUPER_COMBAT_POTION1),
        SUPER_COMBAT(ItemID.SUPER_COMBAT_POTION4, ItemID.SUPER_COMBAT_POTION3, ItemID.SUPER_COMBAT_POTION2, ItemID.SUPER_COMBAT_POTION1);

        @Getter
        private final int dose4, dose3, dose2, dose1;

        SuperCombat(int dose4, int dose3, int dose2, int dose1) {
            this.dose4 = dose4;
            this.dose3 = dose3;
            this.dose2 = dose2;
            this.dose1 = dose1;
        }
    }

    enum MoonClanTele {
        PORTAL_NEXUS(ObjectID.PORTAL_NEXUS_33402),
        MOONCLAN_PORTAL(29339),
        CUSTOM(-1);

        @Getter
        private final int objectID;

        MoonClanTele(int objectID) {
            this.objectID = objectID;
        }
    }
}