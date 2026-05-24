package com.hbm.util;

import net.minecraft.util.text.*;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public final class ChatBuilder {

    private final TextComponentString text;
    private ITextComponent last;

    private ChatBuilder(String text) {
        this.text = new TextComponentString(text);
        this.last = this.text;
    }

    public static ChatBuilder start(String text) {
        return new ChatBuilder(text);
    }

    public static ChatBuilder startTranslation(String key, Object... args) {
        return new ChatBuilder("").nextTranslation(key, args);
    }

    public ChatBuilder next(String text) {
        TextComponentString append = new TextComponentString(text);
        this.last.appendSibling(append);
        this.last = append;
        return this;
    }

    public ChatBuilder nextTranslation(String key, Object... args) {
        TextComponentTranslation append = new TextComponentTranslation(key, args);
        this.last.appendSibling(append);
        this.last = append;
        return this;
    }

    public ChatBuilder color(TextFormatting format) {
        Style style = this.last.getStyle();
        style.setColor(format);
        return this;
    }

    /**
     * Recursively applies the color to the root component and all its siblings.
     */
    public ChatBuilder colorAll(TextFormatting format) {
        List<ITextComponent> list = new ArrayList<>();
        list.add(text);

        ListIterator<ITextComponent> it = list.listIterator();
        while (it.hasNext()) {
            ITextComponent component = it.next();
            component.getStyle().setColor(format);
            for (ITextComponent s : component.getSiblings()) it.add(s);
        }
        return this;
    }

    public TextComponentString flush() {
        return this.text;
    }
}
