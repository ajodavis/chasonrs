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
package net.runelite.client.plugins.autobasalt;

import lombok.Getter;
import net.runelite.api.ItemID;
import net.runelite.api.ObjectID;
import net.runelite.client.config.*;

@ConfigGroup("AutoBasalt")
public interface PluginConfig extends Config
{

	enum Mine {
		BASALT(ObjectID.ROCKS_33257),
		URT_SALT(ObjectID.ROCKS_33254),
		EFH_SALT(ObjectID.ROCKS_33255),
		TE_SALT(ObjectID.ROCKS_33256);

		@Getter
		final int objectId;

		Mine(int objectId) {
			this.objectId = objectId;
		}
	}

	enum CraftTele {
		ICY_BASALT(ItemID.ICY_BASALT),
		STONY_BASALT(ItemID.STONY_BASALT);

		@Getter
		final int itemId;

		CraftTele(int itemId) {
			this.itemId = itemId;
		}
	}

	@ConfigItem(keyName = "startPlugin", name = "Start/Stop", description = "", position = 0, title = "startPlugin")
	default Button startPlugin() {
		return new Button();
	}

	@ConfigItem(
		keyName = "showOverlay",
		name = "Show UI",
		description = "Show the UI on screen",
		position = 1
	)
	default boolean showOverlay() {
		return true;
	}

	@ConfigItem(keyName = "mine", name = "Mine", description = "The rock you want to mine", position = 2)
	default Mine mine() {
		return Mine.BASALT;
	}

	@ConfigItem(
			keyName = "makeTele",
			name = "Make teleports",
			description = "Will note basalt if disabled",
			position = 3
	)
	default boolean makeTele() {
		return true;
	}

	@ConfigItem(keyName = "teleToMake", name = "Tele to make", description = "The tele you want to make", position = 4, hidden = true, unhide = "makeTele")
	default CraftTele craftTele() {
		return CraftTele.ICY_BASALT;
	}

	@ConfigItem(keyName = "invokes", name = "Use invokes (use with caution)", description = "Potentially detected; use with caution", position = 998)
	default boolean invokes() {
		return false;
	}

	@ConfigItem(
		keyName = "debug",
		name = "Debug Messages",
		description = "",
		position = 999
	)
	default boolean debug() {
		return false;
	}
}