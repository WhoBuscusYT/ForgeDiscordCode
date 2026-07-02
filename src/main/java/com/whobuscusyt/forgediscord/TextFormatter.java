package com.whobuscusyt.forgediscord;

public class TextFormatter {

    public static String minecraftToDiscord(String text) {

        boolean bold = false;
        boolean italic = false;
        boolean underline = false;
        boolean strike = false;

        StringBuilder out = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {

            char c = text.charAt(i);

            if (c == '§' && i + 1 < text.length()) {

                char code = Character.toLowerCase(text.charAt(i + 1));

                switch (code) {

                    // Bold
                    case 'l' -> {
                        if (!bold) {
                            out.append("**");
                            bold = true;
                        }
                    }

                    // Italic
                    case 'o' -> {
                        if (!italic) {
                            out.append("*");
                            italic = true;
                        }
                    }

                    // Underline
                    case 'n' -> {
                        if (!underline) {
                            out.append("__");
                            underline = true;
                        }
                    }

                    // Strikethrough
                    case 'm' -> {
                        if (!strike) {
                            out.append("~~");
                            strike = true;
                        }
                    }

                    // Reset formatting
                    case 'r' -> {

                        if (strike) out.append("~~");
                        if (underline) out.append("__");
                        if (italic) out.append("*");
                        if (bold) out.append("**");

                        bold = false;
                        italic = false;
                        underline = false;
                        strike = false;
                    }

                    // Minecraft color codes also reset active formatting.
                    case '0','1','2','3','4','5','6','7',
                         '8','9','a','b','c','d','e','f' -> {
                        if (strike) out.append("~~");
                        if (underline) out.append("__");
                        if (italic) out.append("*");
                        if (bold) out.append("**");

                        bold = false;
                        italic = false;
                        underline = false;
                        strike = false;
                    }

                    // Discord has no equivalent for obfuscated text.
                    case 'k' -> {
                    }
                }

                i++;
                continue;
            }

            out.append(c);
        }

        // Close remaining formatting

        if (strike) out.append("~~");
        if (underline) out.append("__");
        if (italic) out.append("*");
        if (bold) out.append("**");

        return out.toString();
    }
}
