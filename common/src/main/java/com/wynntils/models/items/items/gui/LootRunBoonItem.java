/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.items.items.gui;

public class LootRunBoonItem extends GuiItem {
    private final String title;
    private final String statAffected;
    private final double maxMultiplier;
    private final double statAmount;

    public LootRunBoonItem(String title, double statAmount, String statAffected, double maxMultiplier) {
        this.title = title;
        this.statAffected = statAffected;
        this.maxMultiplier = maxMultiplier;
        this.statAmount = statAmount;
    }

    public String getTitle() {
        return title;
    }

    public double getStatAmount() {
        return statAmount;
    }

    public String getStatAffected() {
        return statAffected;
    }

    public double getMaxMultiplier() {
        return maxMultiplier;
    }

    @Override
    public String toString() {
        return "LootRunBoonItem{" + "title='"
                + title + '\'' + ", statAmount='"
                + statAmount + '\'' + ", statAffected='"
                + statAffected + '\'' + ", maxMultiplier="
                + maxMultiplier + '}';
    }
}
