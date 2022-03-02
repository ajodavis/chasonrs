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
package net.runelite.client.plugins.autooffer;

import net.runelite.client.config.*;

@ConfigGroup("AutoOffer")
public interface PluginConfig extends Config
{

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

	@ConfigItem(
		keyName = "demonicOffering",
		name = "Cast Demonic",
		description = "Cast Demonic Offering?",
		position = 2
	)
	default boolean demonicOffering() {
		return true;
	}

	@Range(min = 1, max = 3)
	@ConfigItem(
			keyName = "ashes",
			name = "Ashes amount",
			description = "Cast Demonic Offering when you have this many ashes",
			position = 3
	)
	default int ashes() {
		return 3;
	}

	@ConfigItem(
		keyName = "sinisterOffering",
		name = "Cast Sinister",
		description = "Cast Sinister Offering?",
		position = 4
	)
	default boolean sinisterOffering() {
		return true;
	}

	@Range(min = 1, max = 3)
	@ConfigItem(
			keyName = "bones",
			name = "Bones amount",
			description = "Cast Sinister Offering when you have this many bones",
			position = 5
	)
	default int bones() {
		return 3;
	}

	@ConfigItem(
			keyName = "soulBearer",
			name = "Fill Soul bearer",
			description = "",
			position = 6
	)
	default boolean soulBearer() {
		return true;
	}

	@Range(min = 1, max = 28)
	@ConfigItem(
			keyName = "ensouled",
			name = "Ensouled head amount",
			description = "Fill Soul bearer when your inventory has this many ensouled heads",
			position = 7
	)
	default int fillAmount() {
		return 2;
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