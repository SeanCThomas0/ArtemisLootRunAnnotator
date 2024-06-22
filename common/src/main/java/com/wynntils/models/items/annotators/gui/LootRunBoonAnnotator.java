/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.items.annotators.gui;

import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.item.GuiItemAnnotator;
import com.wynntils.handlers.item.ItemAnnotation;
import com.wynntils.models.items.items.gui.LootRunBoonItem;
import com.wynntils.utils.mc.LoreUtils;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class LootRunBoonAnnotator implements GuiItemAnnotator {
    private static final Pattern LOOTRUN_BOON_PATTERN = Pattern.compile(
            "(?<=gain\\s\\+)(?<statAmount>(?:\\d+/)?\\d+(?:\\.\\d+)?)(?:s)?(?:%?)\\s(?<statType>Strength|Dexterity|Intelligence|Defense|Damage|Agility|Loot Bonus|Loot Quality|Spell Damage|Health Bonus|Health Regen|Life Steal|Mana Regen|Mana Steal|Walk Speed|Enemy Weakness|Health|Elemental Damage|Weaken Enemy|Critical Damage Bonus)(?:.*?Max x\\s*(?<maxValue>\\d+))?");

    @Override
    public ItemAnnotation getAnnotation(ItemStack itemStack, StyledText name) throws InterruptedException {
        // Loot run boon items are always a diamond
        if (itemStack.getItem() != Items.DIAMOND) {
            return null;
        }

        List<StyledText> lore = LoreUtils.getLore(itemStack);

        StyledText loreString = LoreUtils.getStringLore(itemStack);
        StyledText spacedString = LoreUtils.getStringLoreSpaced(itemStack);
        String testOutput = itemStack.getDisplayName().getString() + LoreUtils.getLore(itemStack);

        String loreText = loreString.toString().trim().replaceAll("§.", ""); // Remove § and the next character
        String spacedLoreText = spacedString.toString().trim().replaceAll("§.", ""); // Remove § and the next character

        Matcher matcher = LOOTRUN_BOON_PATTERN.matcher(loreText);
        if (matcher.find()) {
            double statAmount = parseStatAmount(matcher.group("statAmount"));
            String statType = matcher.group("statType");
            String maxValueGroup = matcher.group("maxValue");
            double maxValue = maxValueGroup != null
                    ? Double.parseDouble(maxValueGroup)
                    : 0; // Set default value if max value is not present
            return new LootRunBoonItem(itemStack.getDisplayName().getString(), statAmount, statType, maxValue);
        }

        return null;
    }

    private static double parseStatAmount(String statAmountString) {
        if (statAmountString.endsWith("%")) {
            // Remove "%" sign and convert to double
            return Double.parseDouble(statAmountString.substring(0, statAmountString.length() - 1));
        } else {
            // Directly parse as double
            return Double.parseDouble(
                    statAmountString.split("/")[0]); // Extract the first part of the stat amount in case of a fraction
        }
    }

    private static void appendToFile(String filePath, String textToAppend) {
        BufferedWriter writer = null;
        try {
            // Create a FileWriter in append mode
            writer = new BufferedWriter(new FileWriter(filePath, true));
            // Write the text to the file
            writer.write(textToAppend);
            // Add a new line for separation
            writer.newLine();
        } catch (IOException e) {
            // Handle the exception
            e.printStackTrace();
        } finally {
            // Ensure the writer is closed
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                // Handle the exception
                e.printStackTrace();
            }
        }
    }
}
