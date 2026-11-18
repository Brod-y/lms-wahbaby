package com.lmswahbaby;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/** Starts a development copy of RuneLite with this plugin loaded (run with ./gradlew run). */
public class LmsWahBabyPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(LmsWahBabyPlugin.class);
		RuneLite.main(args);
	}
}
