package com.whobuscusyt.forgediscord;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import com.whobuscusyt.forgediscord.Discord.DiscordManager;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import java.io.Serializable;

public class DiscordConsoleAppender
        extends AbstractAppender {

    protected DiscordConsoleAppender(
            String name,
            Filter filter,
            Layout<? extends Serializable> layout
    ) {

        super(
                name,
                filter,
                layout,
                false,
                null
        );
    }

    @Override
    public void append(LogEvent event) {

        String msg =
                event.getMessage()
                        .getFormattedMessage();

        DiscordManager.sendConsole(msg);
    }

    public static Appender create() {

        return new DiscordConsoleAppender(
                "ForgeDiscordConsole",
                null,
                null
        );
    }
}