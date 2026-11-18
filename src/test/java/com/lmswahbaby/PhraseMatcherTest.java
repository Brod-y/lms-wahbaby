package com.lmswahbaby;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import org.junit.Test;

public class PhraseMatcherTest
{
	private final PhraseMatcher matcher = new PhraseMatcher(
		Arrays.asList("gl", "gf", "gg", "ggs", "good luck", "good fight", "good fite", "good game"));

	@Test
	public void allowsGoodLuckAndGoodFight()
	{
		for (String message : new String[]{
			"gl", "GL", "gl!", "GL!!!", "g l", "gll", "glgl",
			"good luck", "Good Luck :)", "goodluck", "gooood luck", "gud luck", "goodluk", "good lcuk",
			"gf", "GF", "gf.", "gff", "g f",
			"good fight", "Good fight!", "gud fight", "good fite", "gd fight",
			"gl hf", "glhf", "GL HF!", "gl and hf", "gl n hf", "good luck have fun",
			"gf gl", "gfgl", "gl gf",
			"gl all", "gf everyone", "gl guys", "gf bro", "ty gf", "gf ty",
			"<col=ff0000>gl</col>", "gl hf",
			"gg", "GG", "GGGGG", "gg!", "g g", "ggs", "gg all", "gg everyone", "ty gg",
			"good game", "Good game!", "goodgame", "gud game", "good gaem",
			"glglgl", "GLGLGL", "gl gl gl", "gfgfgf", "gf gf", "gggg gl", "gl gg", "gg gf gl",
			"good luck good luck", "good game gg", "gg good fight",
		})
		{
			assertTrue("should allow: " + message, matcher.isAllowed(message));
		}
	}

	@Test
	public void replacesEverythingElse()
	{
		for (String message : new String[]{
			"", "   ", ":)", "?", "ez", "sit", "hf", "all", "ty", "lol",
			"gl noob", "gf kid", "trash gf", "gfy", "gl ur bad", "ez gf",
			"good luck you'll need it", "gl next time",
			"gg ez", "ez gg", "gg noob", "gg kid", "gg go next", "gg no re", "ggez", "gg sit",
		})
		{
			assertFalse("should replace: " + message, matcher.isAllowed(message));
		}
	}

	@Test
	public void handlesNull()
	{
		assertFalse(matcher.isAllowed(null));
	}

	@Test
	public void emptyAllowListAllowsNothing()
	{
		PhraseMatcher empty = new PhraseMatcher(Arrays.asList("", " "));
		assertFalse(empty.isAllowed("gl"));
		assertFalse(empty.isAllowed("gl hf"));
	}
}
