package org.lts.tabnamedimmer.gui;

import net.minecraft.client.Minecraft;
import org.lwjgl.sdl.SDLDialog;
import org.lwjgl.sdl.SDL_DialogFileCallback;
import org.lwjgl.system.MemoryUtil;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

final class FileDialogs {
    private FileDialogs() {
    }

    static void openFile(Minecraft minecraft, boolean multiple, Consumer<String> onSelected) {
        SDL_DialogFileCallback callback = callback(minecraft, onSelected);
        try {
            SDLDialog.SDL_ShowOpenFileDialog(callback, MemoryUtil.NULL, windowHandle(minecraft), null,
                    (CharSequence) null, multiple);
        } catch (LinkageError | RuntimeException exception) {
            callback.free();
            throw exception;
        }
    }

    static void saveFile(Minecraft minecraft, String defaultName, Consumer<String> onSelected) {
        SDL_DialogFileCallback callback = callback(minecraft, onSelected);
        try {
            SDLDialog.SDL_ShowSaveFileDialog(callback, MemoryUtil.NULL, windowHandle(minecraft), null, defaultName);
        } catch (LinkageError | RuntimeException exception) {
            callback.free();
            throw exception;
        }
    }

    private static long windowHandle(Minecraft minecraft) {
        return minecraft.getWindow() == null ? MemoryUtil.NULL : minecraft.getWindow().handle();
    }

    private static SDL_DialogFileCallback callback(Minecraft minecraft, Consumer<String> onSelected) {
        AtomicReference<SDL_DialogFileCallback> holder = new AtomicReference<>();
        holder.set(SDL_DialogFileCallback.create((userdata, fileList, filter) -> {
            String selected = firstPath(fileList);
            minecraft.schedule(() -> {
                SDL_DialogFileCallback callback = holder.getAndSet(null);
                if (callback != null) {
                    callback.free();
                }
                onSelected.accept(selected);
            });
        }));
        return holder.get();
    }

    private static String firstPath(long fileList) {
        if (fileList == MemoryUtil.NULL) {
            return null;
        }
        long first = MemoryUtil.memGetAddress(fileList);
        return first == MemoryUtil.NULL ? null : MemoryUtil.memUTF8(first);
    }
}
