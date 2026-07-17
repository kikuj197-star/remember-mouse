package com.remembermouse.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.remembermouse.RememberMouse;
import com.remembermouse.RememberMouseConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    // ── Build remembermouse command tree ──────────────────────
    private static LiteralCommandNode<ClientSuggestionProvider> rmBuildNode() {
        LiteralArgumentBuilder<ClientSuggestionProvider> rm =
            LiteralArgumentBuilder.<ClientSuggestionProvider>literal("remembermouse")
                .executes(ctx -> 1)
                .then(LiteralArgumentBuilder.<ClientSuggestionProvider>literal("toggle")
                    .executes(ctx -> 1))
                .then(LiteralArgumentBuilder.<ClientSuggestionProvider>literal("window")
                    .executes(ctx -> 1)
                    .then(RequiredArgumentBuilder.<ClientSuggestionProvider, Integer>argument("seconds", IntegerArgumentType.integer(0))
                        .executes(ctx -> 1)));
        return rm.build();
    }

    // ── Inject into root BEFORE dispatcher creation via ModifyArg ──
    // Uses ModifyArg on CommandDispatcher.<init> to avoid @Redirect conflict with other mods.
    @ModifyArg(
        method = "handleCommands",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/brigadier/CommandDispatcher;<init>(Lcom/mojang/brigadier/tree/RootCommandNode;)V"
        ),
        index = 0
    )
    @SuppressWarnings({"rawtypes", "unchecked"})
    private RootCommandNode rmInjectNode(RootCommandNode root) {
        root.addChild(rmBuildNode());
        return root;
    }

    // ── Fallback for edge-cases ──────────────────────────────
    @Inject(method = "handleCommands", at = @At("TAIL"))
    private void rmOnHandleCommands(ClientboundCommandsPacket packet, CallbackInfo ci) {
        CommandDispatcher<ClientSuggestionProvider> dispatcher =
            ((ClientPacketListener) (Object) this).getCommands();
        if (dispatcher.getRoot().getChild("remembermouse") == null) {
            dispatcher.register(LiteralArgumentBuilder.<ClientSuggestionProvider>literal("remembermouse")
                .redirect(rmBuildNode()));
        }
    }

    // ── Intercept & cancel server-bound command ───────────────
    @Inject(method = "sendCommand", at = @At("HEAD"), cancellable = true)
    private void rmOnSendCommand(String command, CallbackInfo ci) {
        String trimmed = command.startsWith("/") ? command.substring(1).trim() : command.trim();

        if (trimmed.equals("remembermouse") || trimmed.startsWith("remembermouse ")) {
            ci.cancel();

            String[] parts = trimmed.split(" +");
            if (parts.length < 2) return;

            switch (parts[1]) {
                case "toggle" -> rmHandleToggle();
                case "window" -> rmHandleWindow(parts);
            }
        }
    }

    private void rmHandleToggle() {
        RememberMouseConfig cfg = RememberMouse.config;
        cfg.enabled = !cfg.enabled;
        cfg.save();

        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal(
                    cfg.enabled ? "§a[RememberMouse] §fEnabled" : "§c[RememberMouse] §fDisabled"));
        }
    }

    private void rmHandleWindow(String[] parts) {
        RememberMouseConfig cfg = RememberMouse.config;

        if (parts.length == 2) {
            if (Minecraft.getInstance().player != null) {
                String msg = cfg.memoryWindowSeconds == 0
                    ? "§a[RememberMouse] §fMemory window: §ePermanent"
                    : String.format("§a[RememberMouse] §fMemory window: §e%d seconds", cfg.memoryWindowSeconds);
                Minecraft.getInstance().player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(msg));
            }
            return;
        }

        if (parts.length != 3) return;

        try {
            int seconds = Integer.parseInt(parts[2]);
            if (seconds < 0) {
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.sendSystemMessage(
                        net.minecraft.network.chat.Component.literal(
                            "§c[RememberMouse] Memory window must be ≥ 0 (0 = permanent)"));
                }
                return;
            }

            cfg.memoryWindowSeconds = seconds;
            cfg.save();

            if (Minecraft.getInstance().player != null) {
                String msg = seconds == 0
                    ? "§a[RememberMouse] §fMemory window set to: §ePermanent"
                    : String.format("§a[RememberMouse] §fMemory window set to: §e%d seconds", seconds);
                Minecraft.getInstance().player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(msg));
            }
        } catch (NumberFormatException ignored) {}
    }
}
