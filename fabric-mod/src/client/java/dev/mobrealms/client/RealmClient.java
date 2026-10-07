package dev.mobrealms.client;

import dev.mobrealms.fabric.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import com.google.gson.JsonParser;

public final class RealmClient implements ClientModInitializer {
    @Override public void onInitializeClient(){
        EntityRendererRegistry.register(SettlerEntity.TYPE,SettlerRenderer::new);
        ClientPlayNetworking.registerGlobalReceiver(RealmPayload.TYPE,(payload,context)->{
            var data=JsonParser.parseString(payload.json()).getAsJsonObject();
            if(context.client().gui.screen() instanceof RealmScreen screen)screen.update(data);
            else context.client().gui.setScreen(new RealmScreen(data));
        });
    }
}
