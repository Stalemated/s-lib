package com.stalemated.lib.client.internal;

//? if fabric {
import com.google.common.collect.Maps;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import java.util.Map;

public final class ModMenuScreens implements ModMenuApi {

    @Override
    public Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories() {
        return Maps.transformValues(ConfigScreenFactories.view(), factory -> factory::apply);
    }
}
//?}
