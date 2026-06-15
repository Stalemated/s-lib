package com.stalemated.lib.compat.yacl.controller.builder;

import com.stalemated.lib.compat.yacl.controller.AdvancedColorController;
import dev.isxander.yacl3.api.Controller;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.controller.ControllerBuilder;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import net.minecraft.text.Text;

public class AdvancedColorControllerBuilder implements ControllerBuilder<String> {
    private final Option<String> option;
    private boolean alpha = false;

    private AdvancedColorControllerBuilder(Option<String> option) {
        this.option = option;
    }

    public static AdvancedColorControllerBuilder create(Option<String> option) {
        return new AdvancedColorControllerBuilder(option);
    }

    public AdvancedColorControllerBuilder alpha(boolean alpha) {
        this.alpha = alpha;
        return this;
    }

    @Override
    public Controller<String> build() {
        final AdvancedColorController advancedColorController = new AdvancedColorController(this.option, this.alpha);

        return new Controller<>() {
            @Override
            public Option<String> option() {
                return AdvancedColorControllerBuilder.this.option;
            }

            @Override
            public Text formatValue() {
                return advancedColorController.formatValue();
            }

            @Override
            public AbstractWidget provideWidget(YACLScreen screen, Dimension<Integer> widgetDimension) {
                return advancedColorController.provideWidget(screen, widgetDimension);
            }
        };
    }
}