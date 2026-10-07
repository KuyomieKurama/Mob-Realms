package dev.mobrealms.client;

import com.google.gson.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Responsive, paginated realm atlas. All numbers are server snapshots, never authoritative client state. */
public final class RealmScreen extends Screen {
    private JsonObject data;
    private int tab,peoplePage,scroll;
    private int left,top,panelW,panelH,mainX,mainW;
    private String confirm;
    private static final int GOLD=0xffe5bd7c,INK=0xff101920,PAPER=0xffe9e2d2,MUTED=0xff9caeb7,TEAL=0xff6ec9b6;
    private static final String[] TABS={"overview","map","diplomacy","people","research"};
    public RealmScreen(JsonObject data){super(Component.translatable("screen.mobrealms.title"));this.data=data;}
    public void update(JsonObject next){data=next;confirm=null;rebuildWidgets();}
    private Component tr(String key,Object... args){return Component.translatable("screen.mobrealms."+key,args);}
    private Component name(String category,String value){return Component.translatable(category+".mobrealms."+value);}
    private String str(JsonObject j,String key){return j.has(key)?j.get(key).getAsString():"";}
    private int number(JsonObject j,String key){return j.has(key)?j.get(key).getAsInt():0;}
    private JsonObject detail(){return data.has("detail")?data.getAsJsonObject("detail"):new JsonObject();}
    private void send(String command){if(minecraft.getConnection()!=null)minecraft.getConnection().sendCommand(command);}
    private void action(String command){send("realm action "+command);}
    private void button(int x,int y,int w,String label,Runnable work,boolean selected){addRenderableWidget(new RealmButton(x,y,w,19,tr(label),work,selected));}
    @Override protected void init(){
        panelW=Math.min(860,width-16);panelH=Math.min(510,height-16);left=(width-panelW)/2;top=(height-panelH)/2;
        int sidebar=panelW<480?90:126;mainX=left+sidebar+16;mainW=panelW-sidebar-30;
        int tabW=Math.max(32,(mainW-8)/5);
        for(int i=0;i<TABS.length;i++){final int n=i;button(mainX+i*tabW,top+40,tabW-3,TABS[i],()->{tab=n;peoplePage=0;scroll=0;confirm=null;rebuildWidgets();},tab==i);}
        JsonArray towns=data.getAsJsonArray("towns");int visible=8;int townRow=Math.max(12,Math.min(26,(panelH-140)/8));
        for(int i=0;i<Math.min(visible,towns.size());i++){
            var town=towns.get(i).getAsJsonObject();String id=str(town,"id");
            String species=str(town,"species").replace(':','.');
            addRenderableWidget(new RealmButton(left+10,top+67+i*townRow,sidebar-6,townRow-2,Component.translatable("species."+species),()->send("realm view "+id),id.equals(str(data,"selected"))));
        }
        button(left+10,top+panelH-62,(sidebar-10)/2,"prev",()->send("realm page "+Math.max(0,number(data,"page")-1)),false);
        button(left+12+(sidebar-10)/2,top+panelH-62,(sidebar-10)/2,"next",()->send("realm page "+Math.min(number(data,"pages")-1,number(data,"page")+1)),false);
        button(left+10,top+panelH-37,sidebar-6,"refresh",()->send(str(data,"selected").isEmpty()?"realm":"realm view "+str(data,"selected")),false);
        button(left+panelW-57,top+12,45,"close",this::onClose,false);
        if(str(data,"selected").isEmpty())return;
        int y=top+panelH-60;String id=str(data,"selected");
        if(tab==0){
            button(mainX,y,Math.min(140,mainW/2-4),"donate",()->action("donate "+id),false);
            if(str(data,"own").equals(id))button(mainX+mainW/2,y,mainW/2,"claim",()->action("claim "+id),false);
            if(data.get("admin").getAsBoolean()){
                int w=Math.max(30,(mainW-9)/4);
                button(mainX,y+23,w,"days",()->send("civ simulate 30"),false);
                button(mainX+w+3,y+23,w,"speed",()->{confirm="speed";rebuildWidgets();},false);
                button(mainX+(w+3)*2,y+23,w,"normal",()->send("civ speed 1"),false);
                button(mainX+(w+3)*3,y+23,w,"observe",()->{send("civ observe");onClose();},false);
            }
        }
        if(tab==2){
            String[] acts={"gift","buy","barter","trade","non_aggression","alliance","neutral","war","vassal","accept","decline"};
            int w=(mainW-8)/3;int start=top+113;
            for(int i=0;i<acts.length;i++){String a=acts[i];button(mainX+(i%3)*(w+4),start+(i/3)*21,w,a,()->{
                if(a.equals("war")||a.equals("vassal")){confirm=a;rebuildWidgets();}
                else action((Set.of("gift","buy","barter","accept","decline").contains(a)?a:"treaty")+" "+id+(Set.of("gift","buy","barter","accept","decline").contains(a)?"":" "+a));
            },false);}
        }
        if(tab==3){
            JsonArray people=detail().getAsJsonArray("people");int count=Math.max(1,(panelH-160)/40);int start=peoplePage*count;
            if(start>=people.size()){peoplePage=0;start=0;}
            for(int i=start;i<Math.min(start+count,people.size());i++){
                var person=people.get(i).getAsJsonObject();String citizen=str(person,"id");int row=top+99+(i-start)*40;
                if(str(data,"own").equals(id)){
                    String[] roles={"gatherer","miner","builder","farmer","guard","soldier","trader","leader"};int current=Arrays.asList(roles).indexOf(str(person,"role"));String next=roles[(current+1)%roles.length];
                    button(mainX+mainW-88,row,88,"assign",()->action("role "+citizen+" "+next),false);
                }else button(mainX+mainW-88,row,88,"recruit",()->action("recruit "+citizen),false);
            }
            button(mainX,y,80,"prev",()->{peoplePage=Math.max(0,peoplePage-1);rebuildWidgets();},false);
            button(mainX+85,y,80,"next",()->{peoplePage++;rebuildWidgets();},false);
        }
        if(confirm!=null){String pending=confirm;button(mainX,top+panelH-87,mainW,"confirm",()->{if(pending.equals("speed"))send("civ speed 5");else action("treaty "+id+" "+pending);confirm=null;rebuildWidgets();},true);}
    }
    private void text(GuiGraphicsExtractor g,Component value,int x,int y,int color){g.text(font,font.plainSubstrByWidth(value.getString(),mainW),x,y,color,false);}
    private void rule(GuiGraphicsExtractor g,int y){g.fill(mainX,y,mainX+mainW,y+1,0xff34434d);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        g.fill(0,0,width,height,0xb5081016);
        g.fillGradient(left,top,left+panelW,top+panelH,0xff202e38,INK);g.outline(left,top,panelW,panelH,0xff8c7751);
        g.fill(left,top,left+panelW,top+2,GOLD);g.fill(left+8,top+39,mainX-10,top+panelH-8,0xff131e27);
        g.text(font,tr("title"),left+13,top+13,GOLD,false);
        g.text(font,tr("day",number(data,"day"),number(data,"queued")),mainX,top+18,MUTED,false);
        var d=detail();
        g.enableScissor(mainX,top+64,mainX+mainW,top+panelH-66);
        if(tab==0||tab==4){g.pose().pushMatrix();g.pose().translate(0,-scroll);}
        if(!data.has("detail"))g.textWithWordWrap(font,tr("empty"),mainX,top+84,mainW,PAPER);
        else switch(tab){
            case 0->overview(g,d);
            case 1->map(g);
            case 2->{text(g,tr("relations",number(d,"relation"),name("treaty",str(d,"treaty"))),mainX,top+76,GOLD);text(g,tr("reputation",number(d,"reputation")),mainX,top+96,TEAL);g.textWithWordWrap(font,tr("diplomacy_help"),mainX,top+211,mainW,MUTED);}
            case 3->people(g,d);
            case 4->research(g,d);
        }
        if(tab==0||tab==4)g.pose().popMatrix();
        g.disableScissor();
        if(confirm!=null)text(g,tr("confirm_"+confirm),mainX,top+panelH-102,GOLD);
        super.extractRenderState(g,mx,my,delta);
    }
    private void overview(GuiGraphicsExtractor g,JsonObject d){
        text(g,Component.translatable("species."+str(d,"species").replace(':','.')),mainX,top+74,GOLD);
        text(g,tr("population",number(d,"population"),number(d,"housing")),mainX,top+93,PAPER);
        text(g,tr("objective",name("building",str(d,"objective"))),mainX,top+114,TEAL);
        int progress=number(d,"progress"),total=Math.max(1,number(d,"total"));
        g.fill(mainX,top+131,mainX+mainW,top+137,0xff0b1218);g.fill(mainX,top+131,mainX+(int)((long)mainW*progress/total),top+137,TEAL);
        text(g,tr("progress",progress,total),mainX,top+143,MUTED);
        text(g,name("obstacle",str(d,"obstacle")),mainX,top+159,GOLD);rule(g,top+176);
        var stock=d.getAsJsonObject("stock");int columns=Math.max(1,mainW/135),i=0;
        for(var entry:stock.entrySet()){
            int x=mainX+(i%columns)*(mainW/columns),y=top+187+(i/columns)*24;if(i>=24)break;
            var item=BuiltInRegistries.ITEM.getValue(Identifier.parse(entry.getKey()));g.item(new ItemStack(item),x,y-4);
            g.text(font,font.plainSubstrByWidth(item.getName(new ItemStack(item)).getString(),mainW/columns-54),x+20,y,PAPER,false);
            g.text(font,entry.getValue().getAsString(),x+mainW/columns-32,y,TEAL,false);i++;
        }
    }
    private void map(GuiGraphicsExtractor g){
        var towns=data.getAsJsonArray("towns");int minX=Integer.MAX_VALUE,minZ=Integer.MAX_VALUE,maxX=Integer.MIN_VALUE,maxZ=Integer.MIN_VALUE;
        for(var e:towns)if(str(e.getAsJsonObject(),"dimension").equals(str(detail(),"dimension")))for(var p:e.getAsJsonObject().getAsJsonArray("claims")){var a=p.getAsJsonArray();minX=Math.min(minX,a.get(0).getAsInt());maxX=Math.max(maxX,a.get(0).getAsInt());minZ=Math.min(minZ,a.get(1).getAsInt());maxZ=Math.max(maxZ,a.get(1).getAsInt());}
        if(minX==Integer.MAX_VALUE)return;
        int availableH=panelH-160;double scale=Math.min(18,Math.min((double)(mainW-12)/(maxX-minX+2),(double)Math.max(10,availableH)/(maxZ-minZ+2)));
        g.fill(mainX,top+75,mainX+mainW,top+panelH-78,0xff101e25);int i=0;
        for(var e:towns){var town=e.getAsJsonObject();if(!str(town,"dimension").equals(str(detail(),"dimension")))continue;int[] colors={0xff65b9a5,0xffd1aa6a,0xff9b8abd,0xffaab969,0xff7cadd2,0xffd68b77,0xffbac2c7,0xffc790b9};int color=colors[i++%8];
            for(var p:town.getAsJsonArray("claims")){var a=p.getAsJsonArray();int x=mainX+6+(int)((a.get(0).getAsInt()-minX)*scale),z=top+82+(int)((a.get(1).getAsInt()-minZ)*scale),size=Math.max(2,(int)scale-1);g.fill(x,z,x+size,z+size,color);if(str(town,"id").equals(str(data,"selected")))g.outline(x,z,size,size,GOLD);}
        }
        text(g,tr("map_help"),mainX,top+panelH-94,MUTED);
    }
    private void people(GuiGraphicsExtractor g,JsonObject d){
        int count=Math.max(1,(panelH-160)/40),start=peoplePage*count;var people=d.getAsJsonArray("people");
        for(int i=start;i<Math.min(start+count,people.size());i++){
            var p=people.get(i).getAsJsonObject();int y=top+77+(i-start)*40;
            text(g,tr("citizen",str(p,"id").substring(0,8),name("role",str(p,"role")),number(p,"rank")),mainX,y,PAPER);
            text(g,Component.translatable("goal.mobrealms."+str(p,"goal")),mainX,y+15,MUTED);rule(g,y+34);
        }
    }
    private void research(GuiGraphicsExtractor g,JsonObject d){
        text(g,tr("strategy",name("strategy",str(d,"strategy"))),mainX,top+76,GOLD);
        text(g,tr("knowledge",number(d,"research")),mainX,top+95,TEAL);
        String[] techs={"agriculture","masonry","shields","flanking","siege"};Set<String> unlocked=new HashSet<>();for(var e:d.getAsJsonArray("technologies"))unlocked.add(e.getAsString());
        for(int i=0;i<techs.length;i++){int y=top+123+i*27;boolean done=unlocked.contains(techs[i]);g.fill(mainX,y,mainX+mainW,y+23,done?0xff203e39:0xff1c2832);g.fill(mainX,y,mainX+3,y+23,done?TEAL:0xff5e6870);text(g,name("technology",techs[i]),mainX+10,y+7,done?PAPER:MUTED);}
        var weights=d.getAsJsonArray("weights");String[] strategies={"prosperity","expansion","security"};
        for(int i=0;i<3;i++){int y=top+302+i*28;double score=weights.get(i).getAsDouble();text(g,name("strategy",strategies[i]),mainX,y,MUTED);g.fill(mainX,y+13,mainX+mainW,y+17,0xff0b1218);g.fill(mainX,y+13,mainX+(int)(mainW*Math.max(0,Math.min(1,(score+1)/2))),y+17,GOLD);}
        text(g,tr("ranged",(int)(d.get("ranged").getAsDouble()*100)),mainX,top+270,MUTED);
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){if((tab==0||tab==4)&&x>=mainX){scroll=Math.max(0,Math.min(300,scroll-(int)(vertical*20)));return true;}return super.mouseScrolled(x,y,horizontal,vertical);}
    @Override public boolean isPauseScreen(){return false;}
}
