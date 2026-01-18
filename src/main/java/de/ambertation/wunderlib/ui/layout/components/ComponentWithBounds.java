package de.ambertation.wunderlib.ui.layout.components;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import de.ambertation.wunderlib.ui.layout.values.Rectangle;

@OnlyIn(Dist.CLIENT)
public interface ComponentWithBounds {
    Rectangle getRelativeBounds();
}
