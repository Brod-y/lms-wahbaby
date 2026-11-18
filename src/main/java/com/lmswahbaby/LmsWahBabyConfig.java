package com.lmswahbaby;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(LmsWahBabyConfig.GROUP)
public interface LmsWahBabyConfig extends Config
{
	String GROUP = "lmswahbaby";

	@ConfigItem(
		keyName = "lmsGames",
		name = "In LMS games",
		description = "Replace chat while you're in a Last Man Standing game",
		position = 1
	)
	default boolean lmsGames()
	{
		return true;
	}

	@ConfigItem(
		keyName = "feroxEnclave",
		name = "In Ferox Enclave",
		description = "Replace chat while you're in Ferox Enclave, including the LMS lobby",
		position = 2
	)
	default boolean feroxEnclave()
	{
		return true;
	}

	@ConfigItem(
		keyName = "casualWorlds",
		name = "Casual LMS worlds too",
		description = "Also work on the free-to-play LMS Casual worlds, not just the Competitive ones",
		position = 3
	)
	default boolean casualWorlds()
	{
		return false;
	}

	@ConfigItem(
		keyName = "replaceOverhead",
		name = "Replace overhead text",
		description = "Also replace the text shown above players' heads",
		position = 4
	)
	default boolean replaceOverhead()
	{
		return true;
	}

	@ConfigItem(
		keyName = "allowedPhrases",
		name = "Allowed phrases",
		description = "Comma-separated messages to leave alone. Case, punctuation, spaces and repeated letters are ignored, "
			+ "longer phrases allow a one-letter typo, and words like \"hf\" or \"all\" may be added around them",
		position = 5
	)
	default String allowedPhrases()
	{
		return "gl, gf, gg, ggs, good luck, good fight, good fite, good game";
	}

	@ConfigItem(
		keyName = "replacements",
		name = "Replacements",
		description = "Comma-separated messages to show instead; one is picked at random each time",
		position = 6
	)
	default String replacements()
	{
		return "Wah, Wahhh!, Waaaaaah, Wah wah wah, *cries*, *sobs*, *sniffles*, *throws controller*, *cries in LMS*";
	}
}
