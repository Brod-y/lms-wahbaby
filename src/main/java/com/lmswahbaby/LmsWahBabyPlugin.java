package com.lmswahbaby;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.IterableHashTable;
import net.runelite.api.MessageNode;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.WorldType;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.OverheadTextChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "LMS WahBaby",
	description = "Turns other players' public chat in Last Man Standing and Ferox Enclave into \"Wah\", except friendly ones like \"gl\", \"gf\" and \"gg\"",
	tags = {"lms", "last man standing", "chat", "ferox", "fun"}
)
public class LmsWahBabyPlugin extends Plugin
{
	/** Map regions of the LMS islands (Deserted Island, then Wild Varrock), as used by RuneLite's loot tracker. */
	private static final Set<Integer> LMS_REGIONS = ImmutableSet.of(
		13658, 13659, 13660, 13914, 13915, 13916,
		13918, 13919, 13920, 14174, 14175, 14176, 14430, 14431, 14432);

	/** Ferox Enclave's walkable area, as rectangles of {minX, minY, maxX, maxY}. */
	private static final int[][] FEROX_ENCLAVE = {
		{3123, 3622, 3124, 3631},
		{3125, 3617, 3140, 3639},
		{3138, 3636, 3155, 3645},
		{3141, 3625, 3154, 3635},
	};

	/** How long a replacement is remembered so the chat line and overhead text match. */
	private static final int REMEMBER_TICKS = 5;

	/** Replaced chat lines remembered for restoring; more than the chatbox keeps, so none are missed. */
	private static final int MAX_TRACKED_MESSAGES = 1000;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private LmsWahBabyConfig config;

	private PhraseMatcher matcher;
	private List<String> replacements;

	/** Recent replacements by player name, so a message's chat line and overhead text show the same thing. */
	private final Map<String, Replacement> recent = new HashMap<>();

	/** Chat lines this plugin replaced, by message id, so turning the plugin off can put them back. */
	private final Map<Integer, Replacement> replacedMessages = new LinkedHashMap<Integer, Replacement>()
	{
		@Override
		protected boolean removeEldestEntry(Map.Entry<Integer, Replacement> eldest)
		{
			return size() > MAX_TRACKED_MESSAGES;
		}
	};

	@Override
	protected void startUp()
	{
		loadConfig();
	}

	@Override
	protected void shutDown()
	{
		Map<Integer, Replacement> messages = new HashMap<>(replacedMessages);
		Map<String, Replacement> overheads = new HashMap<>(recent);
		replacedMessages.clear();
		recent.clear();
		clientThread.invoke(() -> restoreOriginals(messages, overheads));
	}

	/** Puts back the original text of chat lines and overhead text this plugin replaced. */
	private void restoreOriginals(Map<Integer, Replacement> messages, Map<String, Replacement> overheads)
	{
		IterableHashTable<MessageNode> chatHistory = client.getMessages();
		if (chatHistory != null)
		{
			for (Map.Entry<Integer, Replacement> entry : messages.entrySet())
			{
				MessageNode node = chatHistory.get(entry.getKey());
				// Skip lines that have left the chat history or were changed again by something else.
				if (node != null && entry.getValue().text.equals(node.getValue()))
				{
					node.setValue(entry.getValue().original);
				}
			}
			client.refreshChat();
		}

		WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null)
		{
			return;
		}

		for (Player player : worldView.players())
		{
			if (player == null || player.getName() == null)
			{
				continue;
			}

			Replacement replacement = overheads.get(Text.standardize(player.getName()));
			if (replacement != null && replacement.text.equals(player.getOverheadText()))
			{
				player.setOverheadText(replacement.original);
			}
		}
	}

	@Provides
	LmsWahBabyConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(LmsWahBabyConfig.class);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (LmsWahBabyConfig.GROUP.equals(event.getGroup()))
		{
			loadConfig();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		int now = client.getTickCount();
		recent.values().removeIf(r -> now - r.tick > REMEMBER_TICKS);
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if ((event.getType() != ChatMessageType.PUBLICCHAT && event.getType() != ChatMessageType.AUTOTYPER)
			|| event.getName() == null)
		{
			return;
		}

		String name = Text.standardize(event.getName());
		String original = event.getMessage();
		if (name.isEmpty() || isLocalPlayer(name) || !isActive() || matcher.isAllowed(original))
		{
			return;
		}

		String replacement = replacementFor(name, original);
		MessageNode node = event.getMessageNode();
		replacedMessages.put(node.getId(), new Replacement(node.getValue(), replacement, client.getTickCount()));
		node.setValue(replacement);

		// The overhead text may already be showing the original message.
		if (config.replaceOverhead())
		{
			for (Player player : client.getTopLevelWorldView().players())
			{
				// Players whose name hasn't loaded yet have a null name.
				if (player != null && player.getName() != null && name.equals(Text.standardize(player.getName()))
					&& sameText(player.getOverheadText(), original))
				{
					player.setOverheadText(replacement);
				}
			}
		}
	}

	@Subscribe
	public void onOverheadTextChanged(OverheadTextChanged event)
	{
		if (!config.replaceOverhead() || !(event.getActor() instanceof Player) || event.getActor().getName() == null)
		{
			return;
		}

		// Only replace overhead text that came from a chat message we replaced, so special
		// attack shouts and other game-set text are left alone. If the overhead text arrives
		// before the chat message, onChatMessage replaces it instead.
		Replacement replacement = recent.get(Text.standardize(event.getActor().getName()));
		if (replacement != null && sameText(event.getOverheadText(), replacement.original))
		{
			event.getActor().setOverheadText(replacement.text);
		}
	}

	private String replacementFor(String name, String original)
	{
		Replacement previous = recent.get(name);
		if (previous != null && sameText(previous.original, original))
		{
			return previous.text;
		}

		String text = replacements.get(ThreadLocalRandom.current().nextInt(replacements.size()));
		recent.put(name, new Replacement(original, text, client.getTickCount()));
		return text;
	}

	private boolean isActive()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return false;
		}

		EnumSet<WorldType> worldTypes = client.getWorldType();
		boolean lmsWorld = worldTypes.contains(WorldType.LAST_MAN_STANDING);
		boolean competitive = worldTypes.contains(WorldType.MEMBERS);
		if (!lmsWorld || (!competitive && !config.casualWorlds()))
		{
			return false;
		}

		return (config.lmsGames() && isInLmsGame()) || (config.feroxEnclave() && isInFeroxEnclave());
	}

	private boolean isInLmsGame()
	{
		for (int region : client.getTopLevelWorldView().getMapRegions())
		{
			if (LMS_REGIONS.contains(region))
			{
				return true;
			}
		}
		return false;
	}

	private boolean isInFeroxEnclave()
	{
		Player local = client.getLocalPlayer();
		if (local == null)
		{
			return false;
		}

		WorldPoint location = local.getWorldLocation();
		for (int[] area : FEROX_ENCLAVE)
		{
			if (location.getX() >= area[0] && location.getY() >= area[1]
				&& location.getX() <= area[2] && location.getY() <= area[3])
			{
				return true;
			}
		}
		return false;
	}

	private boolean isLocalPlayer(String standardizedName)
	{
		Player local = client.getLocalPlayer();
		return local != null && local.getName() != null && standardizedName.equals(Text.standardize(local.getName()));
	}

	private static boolean sameText(String a, String b)
	{
		return a != null && b != null && Text.standardize(a).equals(Text.standardize(b));
	}

	private void loadConfig()
	{
		matcher = new PhraseMatcher(Text.fromCSV(config.allowedPhrases()));

		List<String> parsed = new ArrayList<>();
		for (String text : Text.fromCSV(config.replacements()))
		{
			if (!text.isEmpty())
			{
				parsed.add(text);
			}
		}
		if (parsed.isEmpty())
		{
			parsed.add("Wah");
		}
		replacements = parsed;
	}

	private static final class Replacement
	{
		private final String original;
		private final String text;
		private final int tick;

		private Replacement(String original, String text, int tick)
		{
			this.original = original;
			this.text = text;
			this.tick = tick;
		}
	}
}
