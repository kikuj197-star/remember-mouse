package com.remembermouse.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.remembermouse.RememberMouse;
import com.remembermouse.RememberMouseConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    private static LiteralCommandNode<ClientSuggestionProvider> rmBuildNode(String commandName) {
        LiteralArgumentBuilder<ClientSuggestionProvider> menuFlow =
            LiteralArgumentBuilder.<ClientSuggestionProvider>literal(commandName)
                .executes(context -> 1)
                .then(LiteralArgumentBuilder.<ClientSuggestionProvider>literal("toggle")
                    .executes(context -> 1))
                .then(LiteralArgumentBuilder.<ClientSuggestionProvider>literal("window")
                    .executes(context -> 1)
                    .then(RequiredArgumentBuilder.<ClientSuggestionProvider, Integer>argument(
                        "seconds", IntegerArgumentType.integer(0)).executes(context -> 1)));
        return menuFlow.build();
    }

    // Register after the server command tree is built, matching BowTimer and ItemProtector.
    @Inject(method = "handleCommands", at = @At("TAIL"))
    private void rmOnHandleCommands(ClientboundCommandsPacket packet, CallbackInfo ci) {
        CommandDispatcher<ClientSuggestionProvider> dispatcher =
            ((ClientPacketListener) (Object) this).getCommands();
        rmRegisterCommand(dispatcher, "menuflow");
        rmRegisterCommand(dispatcher, "remembermouse");
    }

    private static void rmRegisterCommand(CommandDispatcher<ClientSuggestionProvider> dispatcher,
                                          String commandName) {
        if (dispatcher.getRoot().getChild(commandName) == null) {
            dispatcher.getRoot().addChild(rmBuildNode(commandName));
        }
    }

    @Inject(method = "sendCommand", at = @At("HEAD"), cancellable = true)
    private void rmOnSendCommand(String command, CallbackInfo ci) {
        String trimmed = command.startsWith("/") ? command.substring(1).trim() : command.trim();

        String[] parts = trimmed.split(" +");
        if (parts.length == 0
            || (!parts[0].equals("menuflow") && !parts[0].equals("remembermouse"))) return;

        ci.cancel();
        if (parts.length < 2) return;

        switch (parts[1]) {
            case "toggle" -> rmHandleToggle();
            case "window" -> rmHandleWindow(parts);
        }
    }

    private static void rmHandleToggle() {
        RememberMouseConfig config = RememberMouse.config;
        config.enabled = !config.enabled;
        config.save();

        send(Component.literal(
            config.enabled ? "§a[MenuFlow] §fEnabled" : "§c[MenuFlow] §fDisabled"));
    }

    private static void rmHandleWindow(String[] parts) {
        RememberMouseConfig config = RememberMouse.config;

        if (parts.length == 2) {
            String message = config.memoryWindowSeconds == 0
                ? "§a[MenuFlow] §fMemory window: §ePermanent"
                : String.format("§a[MenuFlow] §fMemory window: §e%d seconds",
                    config.memoryWindowSeconds);
            send(Component.literal(message));
            return;
        }

        if (parts.length != 3) return;

        try {
            int seconds = Integer.parseInt(parts[2]);
            if (seconds < 0) {
                send(Component.literal("§c[MenuFlow] Memory window must be ≥ 0 (0 = permanent)"));
                return;
            }

            config.memoryWindowSeconds = seconds;
            config.save();

            String message = seconds == 0
                ? "§a[MenuFlow] §fMemory window set to: §ePermanent"
                : String.format("§a[MenuFlow] §fMemory window set to: §e%d seconds", seconds);
            send(Component.literal(message));
        } catch (NumberFormatException ignored) {
            // Brigadier prevents invalid values from normal command entry.
        }
    }

    private static void send(Component message) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(message);
        }
    }
}
