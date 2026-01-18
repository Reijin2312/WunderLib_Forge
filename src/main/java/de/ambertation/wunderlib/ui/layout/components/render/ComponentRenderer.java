package de.ambertation.wunderlib.ui.layout.components.render;

import de.ambertation.wunderlib.ui.layout.values.Rectangle;

import net.minecraft.client.gui.GuiGraphics;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ComponentRenderer {
    void renderInBounds(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float deltaTicks,
            Rectangle bounds,
            Rectangle clipRect
    );
}
