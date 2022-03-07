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
package net.runelite.client.plugins.autododge;

import lombok.Getter;
import net.runelite.api.ItemID;
import net.runelite.api.ObjectID;
import net.runelite.client.config.*;

@ConfigGroup("AutoDodge")
public interface PluginConfig extends Config
{
	@ConfigItem(keyName = "dodgeDemonics", name = "Demonic Gorillas", description = "Dodge Demonic Gorilla's AoE", position = 1)
	default boolean dodgeDemonics() {
		return true;
	}

	@ConfigItem(keyName = "testDemonics", name = "Test Demonics", description = "", position = 0, title = "testDemonics")
	default Button testDemonics() {
		return new Button();
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