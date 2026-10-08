package dev.mobrealms.client;

import dev.mobrealms.fabric.SettlerEntity;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.Identifier;

public final class SettlerRenderer extends HumanoidMobRenderer<SettlerEntity,SettlerRenderer.State,HumanoidModel<SettlerRenderer.State>> {
    public static final class State extends HumanoidRenderState { public int skin; }
    private static final String[] SKINS={"steve","alex","ari","efe","kai","makena","noor","sunny","zuri"};
    public SettlerRenderer(EntityRendererProvider.Context context){
        super(context,new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)),.5f);
        addLayer(new HumanoidArmorLayer<>(this,ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR,context.getModelSet(),HumanoidModel<State>::new),context.getEquipmentRenderer()));
    }
    @Override public State createRenderState(){return new State();}
    @Override public Identifier getTextureLocation(State state){return Identifier.withDefaultNamespace("textures/entity/player/wide/"+SKINS[state.skin]+".png");}
    @Override public void extractRenderState(SettlerEntity entity,State state,float delta){super.extractRenderState(entity,state,delta);state.skin=Math.floorMod(entity.getUUID().hashCode(),SKINS.length);}
}
