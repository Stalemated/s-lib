package com.stalemated.lib.compat.yacl.internal;

import com.stalemated.lib.util.ResourceIds;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.gui.controllers.string.IStringController;

public record IdController(Option<String> option) implements IStringController<String> {

    @Override
    public String getString() {
        return option.pendingValue();
    }

    @Override
    public void setFromString(String value) {
        option.requestSet(value);
    }

    @Override
    public boolean isInputValid(String input) {
        return ResourceIds.isValid(input);
    }
}
