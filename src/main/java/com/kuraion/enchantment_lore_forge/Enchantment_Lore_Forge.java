package com.kuraion.enchantment_lore_forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

@Mod("enchantment_lore_forge")
@Mod.EventBusSubscriber
@OnlyIn(Dist.CLIENT)
public class Enchantment_Lore_Forge {

    private static final int LINES_PER_PAGE = 14;
    public static final String MOD_ID = "enchantment_lore_forge";
    private static final String MOD_ID_DOT = MOD_ID + ".";

    public Enchantment_Lore_Forge() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player entity = event.getEntity();
        InteractionHand hand = event.getHand();
        Level world = entity.getLevel();
        useEnchantedBook(entity, world, hand);
    }

    public static InteractionResultHolder<ItemStack> useEnchantedBook(Player user, Level world, InteractionHand hand) {
        ItemStack usedStack = user.getItemInHand(hand);
        if (!(world instanceof ClientLevel) || usedStack.getItem() != Items.ENCHANTED_BOOK)
            return InteractionResultHolder.pass(usedStack);

        Minecraft clientInstance = Minecraft.getInstance();

        List<String> storedEnchantIds = new LinkedList<>();

        for (Tag nbt : EnchantedBookItem.getEnchantments(usedStack)) {
            if (!(nbt instanceof CompoundTag)) continue;
            String idString = ((CompoundTag) nbt).getString("id");
            if (!idString.isEmpty())
                storedEnchantIds.add(idString);
        }

        ItemStack dummyStack = new ItemStack(Items.ENCHANTED_BOOK);
        // even though title and author will never be seen,
        // both must be present or the BookScreen#WrittenBookContents
        // won't consider the nbt valid
        dummyStack.addTagElement("title", StringTag.valueOf(usedStack.getDisplayName().getString()));
        dummyStack.addTagElement("author", StringTag.valueOf("supersaiyansubtlety"));

        ListTag pages = new ListTag();

        if (storedEnchantIds.isEmpty())
            pages.add(StringTag.valueOf(Component.translatable(MOD_ID_DOT + "contains_no_enchantments").getString()));
        else {
            for (String idString : storedEnchantIds) {
                boolean translated = addTranslatedPages(pages, MOD_ID_DOT + idString + ".description");

                translated |= addTranslatedPages(pages, MOD_ID_DOT + idString + ".lore");

                if (!translated)
                    pages.add(StringTag.valueOf(Component.translatable(MOD_ID_DOT + "no_translation_for").getString() + idString));
            }
        }

        dummyStack.addTagElement("pages", pages);
        final BookViewScreen screen = new BookViewScreen(new BookViewScreen.WrittenBookAccess(dummyStack));
        clientInstance.setScreen(screen);

        return InteractionResultHolder.success(usedStack);
    }

    private static boolean addTranslatedPages(ListTag nbtList, String loreKey) {
        if (I18n.exists(loreKey)) {
            String translatedLore = Component.translatable(loreKey).getString();
            if (!translatedLore.isEmpty()) {
                nbtList.addAll(SplitToPageTags(translatedLore));
                return true;
            }
        }
        return false;
    }

    private static Collection<StringTag> SplitToPageTags(String string) {
        List<String> lines = new LinkedList<>();
        StringSplitter textHandler = Minecraft.getInstance().font.getSplitter();
        textHandler.splitLines(string, 114, Style.EMPTY, true, (style, ix, jx) -> {
            String substring = string.substring(ix, jx);
            lines.add(substring);
        });

        List<StringTag> pageTags = new LinkedList<>();

        int linesLeft = LINES_PER_PAGE;
        StringBuilder curString = new StringBuilder();
        while (!lines.isEmpty()) {
            curString.append(lines.remove(0));
            linesLeft--;
            if (linesLeft <= 0) {
                linesLeft = LINES_PER_PAGE;
                pageTags.add(StringTag.valueOf(curString.toString()));
                curString = new StringBuilder();
            }
        }

        if (curString.length() > 0)
            pageTags.add(StringTag.valueOf(curString.toString()));

        return pageTags;
    }
}
