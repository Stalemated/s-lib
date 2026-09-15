package com.stalemated.lib.config.manager.builder;

import blue.endless.jankson.Jankson;
import com.stalemated.lib.config.io.ConfigProvider;
import com.stalemated.lib.config.io.FileConfigProvider;
import com.stalemated.lib.config.io.json5.Json5Serializer;
import com.stalemated.lib.config.manager.builder.record.ResolvedConfig;
import com.stalemated.lib.config.model.OptionTree;
import com.stalemated.lib.util.io.PathUtils;
import com.stalemated.lib.util.reflection.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.stalemated.lib.SLib.MOD_ID;

public final class ConfigBuilderHelper {

    private ConfigBuilderHelper() {}

    public static <T> ResolvedConfig<T> resolveDefaults(
            Class<T> configClass,
            String modId,
            Path configPath,
            Logger logger,
            Supplier<T> defaultFactory,
            ConfigProvider<T> provider,
            Consumer<Jankson.Builder> janksonCustomizer,
            String builderName
    ) {
        if (modId == null && configPath == null) {
            throw new IllegalStateException("Either modId or configPath must be specified in " + builderName);
        }

        String resolvedModId = modId != null ? modId : MOD_ID + " (External Mod)";
        Logger resolvedLogger = logger != null ? logger : LoggerFactory.getLogger(resolvedModId);
        Path resolvedPath = configPath != null ? configPath : PathUtils.buildPath(resolvedModId + ".json5");

        Supplier<T> resolvedFactory = defaultFactory != null ? defaultFactory :
                () -> ReflectionUtils.tryInstantiate(configClass);

        OptionTree tree = new OptionTree(configClass, resolvedFactory);
        ConfigProvider<T> resolvedProvider = provider != null ? provider :
                new FileConfigProvider<>(
                        resolvedPath,
                        resolvedFactory,
                        resolvedLogger,
                        new Json5Serializer<>(configClass, tree, janksonCustomizer)
                );

        return new ResolvedConfig<>(
                resolvedModId,
                resolvedLogger,
                resolvedPath,
                resolvedFactory,
                tree,
                resolvedProvider
        );
    }
}
