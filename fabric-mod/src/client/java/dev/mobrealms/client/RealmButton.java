package dev.mobrealms.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

/** Keyboard-accessible button with the same palette as the realm dashboard. */
final class RealmButton extends AbstractButton {
    private final Runnable action;
    private final boolean selected;
    RealmButton(int x,int y,int w,int h,Component label,Runnable action,boolean selected){super(x,y,w,h,label);this.action=action;this.selected=selected;}
    @Override public void onPress(InputWithModifiers input){if(active)action.run();}
    @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta){
        boolean hover=isHoveredOrFocused();int background=selected?0xff30494c:hover?0xff283540:0xff19242d;
        g.fill(getX(),getY(),getX()+getWidth(),getY()+getHeight(),background);
        g.fill(getX(),getY(),getX()+2,getY()+getHeight(),selected?0xffe8bd75:0xff425462);
        g.outline(getX(),getY(),getWidth(),getHeight(),hover?0xffc5aa78:0xff34434d);
        var font=Minecraft.getInstance().font;String label=font.plainSubstrByWidth(getMessage().getString(),getWidth()-10);
        g.text(font,label,getX()+6,getY()+(getHeight()-8)/2,active?0xffeee6d5:0xff77838b,false);
    }
    @Override protected void updateWidgetNarration(NarrationElementOutput narration){defaultButtonNarrationText(narration);}
}
