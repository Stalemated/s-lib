package com.stalemated.lib.config.manager.builder.record;

import com.stalemated.lib.config.ConfigProvider;
import com.stalemated.lib.config.model.OptionTree;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.function.Supplier;

public record ResolvedConfig<T>(
        String modId,
        Logger logger,
        Path path,
        Supplier<T> factory,
        OptionTree tree,
        ConfigProvider<T> provider
) {}
