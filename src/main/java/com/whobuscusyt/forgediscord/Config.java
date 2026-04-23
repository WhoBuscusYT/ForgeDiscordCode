package com.whobuscusyt.forgediscord;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;
    public static ForgeConfigSpec.ConfigValue<String> STATUS;
    public static ForgeConfigSpec.ConfigValue<String> CHANNEL_ID;
    public static ForgeConfigSpec.ConfigValue<String> ACTIVITY_TYPE;
    public static ForgeConfigSpec.ConfigValue<String> ACTIVITY_TEXT;
    public static ForgeConfigSpec.ConfigValue<String> OWNER_ID;
    public static ForgeConfigSpec.ConfigValue<String> MOD_PREFIX;
    public static ForgeConfigSpec.ConfigValue<String> CONSOLE_ID;

    public static ForgeConfigSpec.ConfigValue<String> DISCORD_TOKEN;

    static {
        BUILDER.push("ForgeDiscord Config");
        DISCORD_TOKEN = BUILDER
                .comment(" This is the config for ForgeDiscord. This is where most of the stuff is configured, with this updating every new mod release.")
                .comment(" Put your Discord bot's token here. This is the primary place and is how you can actually start using the mod.")
                .comment(" Restart the server after updating this.")
                .define("Bot Token", "PUT_TOKEN_HERE");

        MOD_PREFIX = BUILDER
                .comment(" This is the prefix that will be used in every message publically/privatly sent in chat.")
                .comment(" The prefix supports color, but can only be done with the § symbol and cannot be colored with &.")
                .comment(" You can set this to whatever, at whatever length you want. To my knowledge, this isn't character limited.")
                .define("Mod Prefix", "§6[§b§lForgeDiscord§6]");

        STATUS = BUILDER
                .comment(" The status that the bot will be. This can be changed by ONLINE IDLE DND or INVIS")
                .define("Status", "ONLINE");
        CHANNEL_ID = BUILDER
                .comment(" This is the channel that will have all the normal messages, examples: 'Player joined the server', or 'Player: Hello!'")
                .comment(" This is going to be the CHANNEL ID not the channel name.")
                .comment(" Any messages sent here will be sent in game. Editing a message does nothing to what it shows in game.")
                .define("Channel ID", "PUT_CHANNEL_ID_HERE");

        CONSOLE_ID = BUILDER
                .comment(" This is the channel that will output everything displayed in console.")
                .comment(" Any message sent in this channel will be executed as a command via console.")
                .define("Console ID", "PUT_CHANNEL_ID_HERE");

        ACTIVITY_TYPE = BUILDER
                .comment(" This is for the bot's rich presence. If you are living under a rock and don't know what a rich presence is, it's")
                .comment(" for what is the bot doing. You can change this to PLAYING, WATCHING, LISTENING, or COMPETING. It defaults to PLAYING.")
                .define("Activity Type", "PLAYING");

        ACTIVITY_TEXT = BUILDER
                .comment(" This is the text for the activity. The default of this is 'Minecraft'. mixing that with the activity type, it will come out to")
                .comment(" 'Playing Minecraft'")
                .define("Activity Text", "Minecraft");
        OWNER_ID = BUILDER
                .comment(" This user ID will be pinged if the server hits 90% or more on RAM/CPU. Requires the channel ID to be set for this to work.")
                .define("Server Owner User ID", "PUT_YOUR_DISCORD_ID");

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}