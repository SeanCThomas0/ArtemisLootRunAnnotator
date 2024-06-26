/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.text;

import com.wynntils.utils.colors.CustomColor;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Arrays;
import java.util.Objects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public final class PartStyle {
    private static final String STYLE_PREFIX = "§";
    private static final Int2ObjectMap<ChatFormatting> INTEGER_TO_CHATFORMATTING_MAP = Arrays.stream(
                    ChatFormatting.values())
            .filter(ChatFormatting::isColor)
            .collect(
                    () -> new Int2ObjectOpenHashMap<>(ChatFormatting.values().length),
                    (map, cf) -> map.put(cf.getColor(), cf),
                    Int2ObjectMap::putAll);

    private final StyledTextPart owner;

    private final CustomColor color;
    private final boolean obfuscated;
    private final boolean bold;
    private final boolean strikethrough;
    private final boolean underlined;
    private final boolean italic;
    private final ClickEvent clickEvent;
    private final HoverEvent hoverEvent;

    private PartStyle(
            StyledTextPart owner,
            CustomColor color,
            boolean obfuscated,
            boolean bold,
            boolean strikethrough,
            boolean underlined,
            boolean italic,
            ClickEvent clickEvent,
            HoverEvent hoverEvent) {
        this.owner = owner;
        this.color = color;
        this.obfuscated = obfuscated;
        this.bold = bold;
        this.strikethrough = strikethrough;
        this.underlined = underlined;
        this.italic = italic;
        this.clickEvent = clickEvent;
        this.hoverEvent = hoverEvent;
    }

    PartStyle(PartStyle partStyle, StyledTextPart owner) {
        this.owner = owner;
        this.color = partStyle.color;
        this.obfuscated = partStyle.obfuscated;
        this.bold = partStyle.bold;
        this.strikethrough = partStyle.strikethrough;
        this.underlined = partStyle.underlined;
        this.italic = partStyle.italic;
        this.clickEvent = partStyle.clickEvent;
        this.hoverEvent = partStyle.hoverEvent;
    }

    static PartStyle fromStyle(Style style, StyledTextPart owner, Style parentStyle) {
        Style inheritedStyle;

        if (parentStyle == null) {
            inheritedStyle = style;
        } else {
            // This changes properties that are null, as-in, inherting from the previous style.
            inheritedStyle = style.applyTo(parentStyle);
        }

        return new PartStyle(
                owner,
                inheritedStyle.getColor() == null
                        ? CustomColor.NONE
                        : CustomColor.fromInt(inheritedStyle.getColor().getValue()),
                inheritedStyle.isObfuscated(),
                inheritedStyle.isBold(),
                inheritedStyle.isStrikethrough(),
                inheritedStyle.isUnderlined(),
                inheritedStyle.isItalic(),
                inheritedStyle.getClickEvent(),
                inheritedStyle.getHoverEvent());
    }

    public String asString(PartStyle previousStyle, StyleType type) {
        // Rules of converting a Style to a String:
        // Every style is prefixed with a §.
        // 0. Every style string is fully qualified, meaning that it contains all the formatting, and reset if needed.
        // 1. Style color is converted to a color segment.
        //    A color segment is the prefix and the chatFormatting char.
        //    If this is a custom color, a hex color code is used.
        //    Example: §#FF0000 or §1
        // 2. Formatting is converted the same way as in the Style class.
        // 3. Click events are wrapped in square brackets, and is represented as an id.
        //    The parent of this style's owner is responsible for keeping track of click events.
        //    Example: §[1] -> (1st click event)
        // 4. Hover events are wrapped in angle brackets, and is represented as an id.
        //    The parent of this style's owner is responsible for keeping track of hover events.
        //    Example: §<1> -> (1st hover event)

        if (type == StyleType.NONE) return "";

        StringBuilder styleString = new StringBuilder();

        boolean skipFormatting = false;

        // If the color is the same as the previous style, we can try to construct a difference.
        // If colors don't match, the inserted color will reset the formatting, thus we need to include all formatting.
        // If the current color is NONE, we NEED to try to construct a difference,
        // since there will be no color formatting resetting the formatting afterwards.
        if (previousStyle != null && (color == CustomColor.NONE || previousStyle.color.equals(color))) {
            String differenceString = this.tryConstructDifference(previousStyle, type == StyleType.INCLUDE_EVENTS);

            if (differenceString != null) {
                styleString.append(differenceString);
                skipFormatting = true;
            } else {
                styleString.append(STYLE_PREFIX).append(ChatFormatting.RESET.getChar());
            }
        }

        if (!skipFormatting) {
            // 1. Color
            if (color != CustomColor.NONE) {
                ChatFormatting chatFormatting = INTEGER_TO_CHATFORMATTING_MAP.get(color.asInt());

                if (chatFormatting != null) {
                    styleString.append(STYLE_PREFIX).append(chatFormatting.getChar());
                } else {
                    styleString.append(STYLE_PREFIX).append(color.toHexString());
                }
            }

            // 2. Formatting
            if (obfuscated) {
                styleString.append(STYLE_PREFIX).append(ChatFormatting.OBFUSCATED.getChar());
            }
            if (bold) {
                styleString.append(STYLE_PREFIX).append(ChatFormatting.BOLD.getChar());
            }
            if (strikethrough) {
                styleString.append(STYLE_PREFIX).append(ChatFormatting.STRIKETHROUGH.getChar());
            }
            if (underlined) {
                styleString.append(STYLE_PREFIX).append(ChatFormatting.UNDERLINE.getChar());
            }
            if (italic) {
                styleString.append(STYLE_PREFIX).append(ChatFormatting.ITALIC.getChar());
            }

            if (type == StyleType.INCLUDE_EVENTS) {
                // 3. Click event
                if (clickEvent != null) {
                    styleString
                            .append(STYLE_PREFIX)
                            .append("[")
                            .append(owner.getParent().getClickEventIndex(clickEvent))
                            .append("]");
                }

                // 4. Hover event
                if (hoverEvent != null) {
                    styleString
                            .append(STYLE_PREFIX)
                            .append("<")
                            .append(owner.getParent().getHoverEventIndex(hoverEvent))
                            .append(">");
                }
            }
        }

        return styleString.toString();
    }

    public Style getStyle() {
        // Optimization: Use raw Style constructor, instead of the builder.
        TextColor textColor = color == CustomColor.NONE ? null : TextColor.fromRgb(color.asInt());
        Style reconstructedStyle = new Style(
                textColor, bold, italic, underlined, strikethrough, obfuscated, clickEvent, hoverEvent, null, null);

        return reconstructedStyle;
    }

    public PartStyle withColor(ChatFormatting color) {
        if (!color.isColor()) {
            throw new IllegalArgumentException("ChatFormatting " + color + " is not a color!");
        }

        CustomColor newColor = CustomColor.fromInt(color.getColor());

        return new PartStyle(
                owner, newColor, obfuscated, bold, strikethrough, underlined, italic, clickEvent, hoverEvent);
    }

    public boolean isBold() {
        return bold;
    }

    public boolean isObfuscated() {
        return obfuscated;
    }

    public boolean isStrikethrough() {
        return strikethrough;
    }

    public boolean isUnderlined() {
        return underlined;
    }

    public boolean isItalic() {
        return italic;
    }

    public ClickEvent getClickEvent() {
        return clickEvent;
    }

    public HoverEvent getHoverEvent() {
        return hoverEvent;
    }

    public PartStyle withBold(boolean bold) {
        return new PartStyle(owner, color, obfuscated, bold, strikethrough, underlined, italic, clickEvent, hoverEvent);
    }

    public PartStyle withObfuscated(boolean obfuscated) {
        return new PartStyle(owner, color, obfuscated, bold, strikethrough, underlined, italic, clickEvent, hoverEvent);
    }

    public PartStyle withStrikethrough(boolean strikethrough) {
        return new PartStyle(owner, color, obfuscated, bold, strikethrough, underlined, italic, clickEvent, hoverEvent);
    }

    public PartStyle withUnderlined(boolean underlined) {
        return new PartStyle(owner, color, obfuscated, bold, strikethrough, underlined, italic, clickEvent, hoverEvent);
    }

    public PartStyle withItalic(boolean italic) {
        return new PartStyle(owner, color, obfuscated, bold, strikethrough, underlined, italic, clickEvent, hoverEvent);
    }

    public PartStyle withClickEvent(ClickEvent clickEvent) {
        return new PartStyle(owner, color, obfuscated, bold, strikethrough, underlined, italic, clickEvent, hoverEvent);
    }

    public PartStyle withHoverEvent(HoverEvent hoverEvent) {
        return new PartStyle(owner, color, obfuscated, bold, strikethrough, underlined, italic, clickEvent, hoverEvent);
    }

    private String tryConstructDifference(PartStyle oldStyle, boolean includeEvents) {
        StringBuilder add = new StringBuilder();

        int oldColorInt = oldStyle.color.asInt();
        int newColorInt = this.color.asInt();

        if (oldColorInt == -1) {
            if (newColorInt != -1) {
                Arrays.stream(ChatFormatting.values())
                        .filter(c -> c.isColor() && newColorInt == c.getColor())
                        .findFirst()
                        .ifPresent(add::append);
            }
        } else if (oldColorInt != newColorInt) {
            return null;
        }

        if (oldStyle.obfuscated && !this.obfuscated) return null;
        if (!oldStyle.obfuscated && this.obfuscated) add.append(ChatFormatting.OBFUSCATED);

        if (oldStyle.bold && !this.bold) return null;
        if (!oldStyle.bold && this.bold) add.append(ChatFormatting.BOLD);

        if (oldStyle.strikethrough && !this.strikethrough) return null;
        if (!oldStyle.strikethrough && this.strikethrough) add.append(ChatFormatting.STRIKETHROUGH);

        if (oldStyle.underlined && !this.underlined) return null;
        if (!oldStyle.underlined && this.underlined) add.append(ChatFormatting.UNDERLINE);

        if (oldStyle.italic && !this.italic) return null;
        if (!oldStyle.italic && this.italic) add.append(ChatFormatting.ITALIC);

        if (includeEvents) {
            // If there is a click event in the old style, but not in the new one, we can't construct a difference.
            // Otherwise, if the old style and the new style has different events, add the new event.
            // This can happen in two cases:
            // - The old style has an event, but the new one has one as well.
            // - The old style doesn't have an event, but the new does.

            if (oldStyle.clickEvent != null && this.clickEvent == null) return null;
            if (oldStyle.clickEvent != this.clickEvent) {
                add.append(STYLE_PREFIX)
                        .append("[")
                        .append(owner.getParent().getClickEventIndex(clickEvent))
                        .append("]");
            }

            if (oldStyle.hoverEvent != null && this.hoverEvent == null) return null;
            if (oldStyle.hoverEvent != this.hoverEvent) {
                add.append(STYLE_PREFIX)
                        .append("<")
                        .append(owner.getParent().getHoverEventIndex(hoverEvent))
                        .append(">");
            }
        }

        return add.toString();
    }

    @Override
    public String toString() {
        return "PartStyle{" + "color="
                + color + ", bold="
                + bold + ", italic="
                + italic + ", underlined="
                + underlined + ", strikethrough="
                + strikethrough + ", obfuscated="
                + obfuscated + ", clickEvent="
                + clickEvent + ", hoverEvent="
                + hoverEvent + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PartStyle partStyle = (PartStyle) o;
        return bold == partStyle.bold
                && italic == partStyle.italic
                && underlined == partStyle.underlined
                && strikethrough == partStyle.strikethrough
                && obfuscated == partStyle.obfuscated
                && Objects.equals(color, partStyle.color)
                && Objects.equals(clickEvent, partStyle.clickEvent)
                && Objects.equals(hoverEvent, partStyle.hoverEvent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(color, bold, italic, underlined, strikethrough, obfuscated, clickEvent, hoverEvent);
    }

    public enum StyleType {
        INCLUDE_EVENTS, // Includes click and hover events
        DEFAULT, // The most minimal way to represent a style
        NONE // No styling
    }
}
