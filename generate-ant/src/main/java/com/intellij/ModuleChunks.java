package com.intellij;

import com.intellij.compiler.ModuleCompilerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.Chunk;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.graph.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ModuleChunks {
    @NotNull
    public static <Node> Graph<Chunk<Node>> toChunkGraph(@NotNull Graph<Node> graph) {
        return GraphAlgorithms.getInstance().computeSCCGraph(graph);
    }

    @NotNull
    public static List<Chunk<Module>> getSortedModuleChunks(@NotNull Project project, @NotNull List<? extends Module> modules) {
        final Module[] allModules = ModuleManager.getInstance(project).getModules();
        final List<Chunk<Module>> chunks = getSortedChunks(createModuleGraph(allModules));

        final Set<Module> modulesSet = new HashSet<>(modules);
        // leave only those chunks that contain at least one module from modules
        chunks.removeIf(chunk -> !ContainerUtil.intersects(chunk.getNodes(), modulesSet));
        return chunks;
    }

    @NotNull
    private static Graph<Module> createModuleGraph(Module @NotNull [] modules) {
        return GraphGenerator.generate(CachingSemiGraph.cache(new InboundSemiGraph<>() {
            @NotNull
            @Override
            public Collection<Module> getNodes() {
                return Arrays.asList(modules);
            }

            @NotNull
            @Override
            public Iterator<Module> getIn(Module module) {
                return Arrays.asList(ModuleCompilerUtil.getDependencies(module)).iterator();
            }
        }));
    }

    @NotNull
    private static <Node> List<Chunk<Node>> getSortedChunks(@NotNull Graph<Node> graph) {
        final Graph<Chunk<Node>> chunkGraph = toChunkGraph(graph);
        final List<Chunk<Node>> chunks = new ArrayList<>(chunkGraph.getNodes());
        DFSTBuilder<Chunk<Node>> builder = new DFSTBuilder<>(chunkGraph);
        if (!builder.isAcyclic()) {
            LOG.error("Acyclic graph expected");
            return null;
        }

        chunks.sort(builder.comparator());
        return chunks;
    }

    private static final Logger LOG = Logger.getInstance(ModuleChunks.class);
}
