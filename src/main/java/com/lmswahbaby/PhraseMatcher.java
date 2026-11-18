package com.lmswahbaby;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Decides whether a chat message is only a friendly "gl" / "gf" style message.
 * <p>
 * Messages are compared loosely: case, spaces, punctuation, chat tags and repeated
 * letters are ignored ("GL!!", "g l", "gooood luck" all match), longer phrases also
 * accept a one-letter typo ("gud luck", "goodluk"), phrases may be repeated or combined
 * ("glglgl", "gg gl", "GGGGG"), and a few harmless extra words are allowed around them
 * ("gl hf", "gf all", "gl everyone"). Anything else, including "gl" followed by an insult,
 * is not allowed.
 */
final class PhraseMatcher
{
	/**
	 * Words that may sit next to an allowed phrase without changing its meaning.
	 * On their own they don't count as allowed.
	 */
	private static final String[] FILLER_WORDS = {
		"all", "everyone", "every1", "guys", "lads", "boys", "gents", "yall",
		"hf", "have fun", "ty", "thx", "thanks", "and", "n", "mate", "m8", "man", "bro",
	};

	/** Phrases this short must match exactly; a one-letter typo would match too much. */
	private static final int MIN_LENGTH_FOR_TYPOS = 6;

	private final Set<String> allowed = new HashSet<>();
	private final Set<String> fillers = new HashSet<>();

	PhraseMatcher(Collection<String> allowedPhrases)
	{
		for (String phrase : allowedPhrases)
		{
			String normalized = normalize(phrase);
			if (!normalized.isEmpty())
			{
				allowed.add(normalized);
			}
		}

		for (String filler : FILLER_WORDS)
		{
			fillers.add(normalize(filler));
		}
	}

	boolean isAllowed(String message)
	{
		if (message == null)
		{
			return false;
		}

		String text = normalize(message);
		int length = text.length();
		if (length == 0)
		{
			return false;
		}

		// Split the text into allowed phrases and filler words, needing at least one
		// allowed phrase. withPhrase[i]: text[0, i) splits with at least one allowed phrase;
		// onlyFillers[i]: text[0, i) splits into filler words only.
		boolean[] withPhrase = new boolean[length + 1];
		boolean[] onlyFillers = new boolean[length + 1];
		onlyFillers[0] = true;

		for (int start = 0; start < length; start++)
		{
			if (!withPhrase[start] && !onlyFillers[start])
			{
				continue;
			}

			for (int end = start + 1; end <= length; end++)
			{
				String part = text.substring(start, end);

				if (matchesAllowed(part))
				{
					withPhrase[end] = true;
				}

				if (fillers.contains(part))
				{
					withPhrase[end] |= withPhrase[start];
					onlyFillers[end] |= onlyFillers[start];
				}
			}
		}

		return withPhrase[length];
	}

	private boolean matchesAllowed(String part)
	{
		if (allowed.contains(part))
		{
			return true;
		}

		for (String phrase : allowed)
		{
			if (phrase.length() >= MIN_LENGTH_FOR_TYPOS
				&& Math.abs(phrase.length() - part.length()) <= 1
				&& typoDistance(phrase, part) <= 1)
			{
				return true;
			}
		}

		return false;
	}

	/**
	 * Lowercases, strips chat tags, keeps only letters and digits, and squashes repeated
	 * characters ("Gooood   Luck!!" becomes "godluck").
	 */
	static String normalize(String text)
	{
		String lettersOnly = text
			.replaceAll("<[^>]*>", "")
			.toLowerCase(Locale.ROOT)
			.replaceAll("[^a-z0-9]", "");
		return lettersOnly.replaceAll("(.)\\1+", "$1");
	}

	/**
	 * Edit distance where swapping two neighbouring letters counts as one typo
	 * (optimal string alignment distance).
	 */
	static int typoDistance(String a, String b)
	{
		int[][] d = new int[a.length() + 1][b.length() + 1];
		for (int i = 0; i <= a.length(); i++)
		{
			d[i][0] = i;
		}
		for (int j = 0; j <= b.length(); j++)
		{
			d[0][j] = j;
		}

		for (int i = 1; i <= a.length(); i++)
		{
			for (int j = 1; j <= b.length(); j++)
			{
				int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
				d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost);

				if (i > 1 && j > 1 && a.charAt(i - 1) == b.charAt(j - 2) && a.charAt(i - 2) == b.charAt(j - 1))
				{
					d[i][j] = Math.min(d[i][j], d[i - 2][j - 2] + 1);
				}
			}
		}

		return d[a.length()][b.length()];
	}
}
