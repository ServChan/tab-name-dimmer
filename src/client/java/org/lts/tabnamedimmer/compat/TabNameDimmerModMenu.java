package org.lts.tabnamedimmer.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import org.lts.tabnamedimmer.gui.TabNameDimmerConfigScreen;

public class TabNameDimmerModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return TabNameDimmerConfigScreen::new;
    }
}
