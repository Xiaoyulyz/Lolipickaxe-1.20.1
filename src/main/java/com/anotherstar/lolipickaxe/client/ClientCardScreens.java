package com.anotherstar.lolipickaxe.client;

import com.anotherstar.lolipickaxe.client.screen.CardUrlConfigScreen;
import com.anotherstar.lolipickaxe.client.screen.LegacyCardScreen;
import com.anotherstar.lolipickaxe.client.screen.OnlineCardScreen;
import com.anotherstar.lolipickaxe.item.CardItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class ClientCardScreens {
    public static void open(CardItem.Kind kind, InteractionHand hand, ItemStack stack, boolean sneaking) {
        if (kind == CardItem.Kind.ONLINE) {
            String url = stack.hasTag() ? stack.getTag().getString("ImageUrl") : "";
            if (sneaking) Minecraft.getInstance().setScreen(new CardUrlConfigScreen(hand, url));
            else Minecraft.getInstance().setScreen(new OnlineCardScreen(url));
            return;
        }
        List<LegacyCardScreen.Page> pages = new ArrayList<>();
        if (kind == CardItem.Kind.ALBUM) {
            int[][] sizes = {{656,1000},{644,1000},{1157,1637},{2890,4092},{1060,1500},{1920,1237},{1301,2015},{1018,1500}};
            for (int i = 0; i < sizes.length; i++) pages.add(new LegacyCardScreen.Page(
                    LegacyCardScreen.card("xiaomo_daughter_" + (i + 1) + ".png"), sizes[i][0], sizes[i][1]));
        } else {
            String picture = stack.hasTag() ? stack.getTag().getString("picture") : "";
            if (picture.contains("53ec") || picture.contains("summon_altar"))
                pages.add(new LegacyCardScreen.Page(LegacyCardScreen.card("summon_altar_layout.png"), 63, 63));
            else pages.add(new LegacyCardScreen.Page(LegacyCardScreen.card("gk_head_portrait.png"), 1000, 1000));
        }
        Minecraft.getInstance().setScreen(new LegacyCardScreen(pages));
    }

    private ClientCardScreens() {}
}
