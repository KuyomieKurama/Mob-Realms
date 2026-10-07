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
    private int tab,peoplePage,scroll,refreshTicks,adminPage,logPage;
    private boolean selectedLogs;
    private int left,top,panelW,panelH,mainX,mainW;
    private String confirm;
    private String commandDraft="civ info";
    private static final int GOLD=0xffe5bd7c,INK=0xff101920,PAPER=0xffe9e2d2,MUTED=0xff9caeb7,TEAL=0xff6ec9b6;
    private static final String[] TABS={"overview","map","diplomacy","people","research","admin"};
    public RealmScreen(JsonObject data){super(Component.translatable("screen.mobrealms.title"));this.data=data;if(data.has("focusAdmin")&&data.get("focusAdmin").getAsBoolean())tab=5;}
    public void update(JsonObject next){
        if(!str(data,"selected").equals(str(next,"selected"))){confirm=null;peoplePage=0;scroll=0;}
        data=next;if(!(tab==5&&adminPage!=2&&getFocused() instanceof net.minecraft.client.gui.components.EditBox))rebuildWidgets();
    }
    @Override public void tick(){
        super.tick();if(++refreshTicks>=100){refreshTicks=0;refresh();}
    }
    private void refresh(){send(str(data,"selected").isEmpty()?"realm":"realm view "+str(data,"selected"));}
    private void selectTab(int next){tab=next;peoplePage=0;scroll=0;confirm=null;rebuildWidgets();}
    private Component tr(String key,Object... args){return Component.translatable("screen.mobrealms."+key,args);}
    private Component name(String category,String value){return Component.translatable(category+".mobrealms."+value);}
    private String str(JsonObject j,String key){return j.has(key)?j.get(key).getAsString():"";}
    private int number(JsonObject j,String key){return j.has(key)?j.get(key).getAsInt():0;}
    private JsonObject detail(){return data.has("detail")?data.getAsJsonObject("detail"):new JsonObject();}
    private void send(String command){if(minecraft.getConnection()!=null)minecraft.getConnection().sendCommand(command);}
    private void action(String command){send("realm action "+command);}
    private RealmButton button(int x,int y,int w,String label,Runnable work,boolean selected){
        var b=new RealmButton(x,y,w,19,tr(label),work,selected);
        b.setTooltip(net.minecraft.client.gui.components.Tooltip.create(tr(label)));
        return addRenderableWidget(b);
    }
    @Override protected void init(){
        panelW=Math.min(860,width-16);panelH=Math.min(510,height-16);left=(width-panelW)/2;top=(height-panelH)/2;
        int sidebar=panelW<480?90:126;mainX=left+sidebar+16;mainW=panelW-sidebar-30;
        int tabs=data.get("admin").getAsBoolean()?6:5;if(tab>=tabs)tab=0;
        if(mainW<360){
            button(mainX,top+40,24,"prev",()->selectTab(Math.floorMod(tab-1,tabs)),false);
            button(mainX+27,top+40,mainW-54,TABS[tab],()->{},true);
            button(mainX+mainW-24,top+40,24,"next",()->selectTab((tab+1)%tabs),false);
        }else{
            int tabW=mainW/tabs;
            for(int i=0;i<tabs;i++){final int n=i;button(mainX+i*tabW,top+40,tabW-3,TABS[i],()->selectTab(n),tab==i);}
        }
        JsonArray towns=data.getAsJsonArray("towns");int visible=8;int townRow=Math.max(12,Math.min(26,(panelH-140)/8));
        for(int i=0;i<Math.min(visible,towns.size());i++){
            var town=towns.get(i).getAsJsonObject();String id=str(town,"id");
            String species=str(town,"species").replace(':','.');
            addRenderableWidget(new RealmButton(left+10,top+67+i*townRow,sidebar-6,townRow-2,Component.translatable("species."+species),()->send("realm view "+id),id.equals(str(data,"selected"))));
        }
        button(left+10,top+panelH-62,(sidebar-10)/2,"prev",()->send("realm page "+Math.max(0,number(data,"page")-1)),false).active=number(data,"page")>0;
        button(left+12+(sidebar-10)/2,top+panelH-62,(sidebar-10)/2,"next",()->send("realm page "+Math.min(number(data,"pages")-1,number(data,"page")+1)),false).active=number(data,"page")+1<number(data,"pages");
        button(left+10,top+panelH-37,sidebar-6,"refresh",()->send(str(data,"selected").isEmpty()?"realm":"realm view "+str(data,"selected")),false);
        button(left+panelW-57,top+12,45,"close",this::onClose,false);
        if(tab==5){adminControls();return;}
        if(str(data,"selected").isEmpty())return;
        int y=top+panelH-60;String id=str(data,"selected");
        if(confirm!=null){
            String pending=confirm;
            button(mainX,top+114,mainW,"confirm",()->{if(pending.equals("speed"))send("civ speed 5");else action("treaty "+id+" "+pending);confirm=null;rebuildWidgets();},true);
            button(mainX,top+139,mainW,"close",()->{confirm=null;rebuildWidgets();},false);return;
        }
        if(tab==0){
            button(mainX,y,Math.min(140,mainW/2-4),"donate",()->action("donate "+id),false);
            if(str(data,"own").equals(id))button(mainX+mainW/2,y,mainW/2,"claim",()->action("claim "+id),false);

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
            JsonArray people=detail().getAsJsonArray("people");int count=Math.max(1,(panelH-160)/(data.get("admin").getAsBoolean()?56:40));int start=peoplePage*count;
            if(start>=people.size()){peoplePage=0;start=0;}
            for(int i=start;i<Math.min(start+count,people.size());i++){
                var person=people.get(i).getAsJsonObject();String citizen=str(person,"id");int row=top+77+(i-start)*(data.get("admin").getAsBoolean()?56:40);
                if(str(data,"own").equals(id)){
                    String[] roles={"gatherer","miner","builder","farmer","guard","soldier","trader","leader"};int current=Arrays.asList(roles).indexOf(str(person,"role"));String next=roles[(current+1)%roles.length];
                    button(mainX+mainW-88,row,88,"assign",()->action("role "+citizen+" "+next),false).setTooltip(net.minecraft.client.gui.components.Tooltip.create(residentTooltip(person,citizen)));
                }else button(mainX+mainW-88,row,88,"recruit",()->action("recruit "+citizen),false).setTooltip(net.minecraft.client.gui.components.Tooltip.create(residentTooltip(person,citizen)));
            }
            button(mainX,y,80,"prev",()->{peoplePage=Math.max(0,peoplePage-1);rebuildWidgets();},false).active=peoplePage>0;
            button(mainX+85,y,80,"next",()->{peoplePage++;rebuildWidgets();},false).active=(peoplePage+1)*count<people.size();
        }

    }
    private void adminControls(){
        if(confirm!=null){
            button(mainX,top+114,mainW,"confirm",()->{send("civ speed 5");confirm=null;rebuildWidgets();},true);
            button(mainX,top+139,mainW,"cancel",()->{confirm=null;rebuildWidgets();},false);return;
        }
        int nav=(mainW-8)/3,w=(mainW-6)/2;
        button(mainX,top+75,nav,"simulation",()->{adminPage=0;rebuildWidgets();},adminPage==0);
        button(mainX+nav+4,top+75,nav,"world",()->{adminPage=1;rebuildWidgets();},adminPage==1);
        button(mainX+2*(nav+4),top+75,nav,"logs",()->{adminPage=2;logPage=0;rebuildWidgets();},adminPage==2);
        if(adminPage==2){logControls();return;}
        if(adminPage==1){
            button(mainX,top+98,w,"teleport",()->{send("civ tp "+str(data,"selected"));onClose();},false).active=!str(data,"selected").isEmpty();
            button(mainX+w+6,top+98,w,"goals",()->selectTab(3),false).active=data.has("detail");
        }
        if(adminPage==0){
            int[] days={1,7,30,365};
            for(int i=0;i<days.length;i++){int n=days[i];
                button(mainX+(i%2)*(w+6),top+122+(i/2)*24,w,"days_"+n,()->send("civ simulate "+n),false).active=number(data,"queued")+n<=365;
            }
            button(mainX,top+170,w,"cancel_queue",()->send("civ simulate cancel"),false).active=number(data,"queued")>0;
            button(mainX+w+6,top+170,w,"goals",()->selectTab(3),false).active=data.has("detail");
        }else{
            button(mainX,top+122,w,"normal",()->send("civ speed 1"),false);
            button(mainX+w+6,top+122,w,"speed",()->{confirm="speed";rebuildWidgets();},false);
            button(mainX,top+146,w,"observe",()->{send("civ observe");onClose();},false);
            button(mainX+w+6,top+146,w,"survival",()->{send("civ observe survival");onClose();},false);
            button(mainX,top+170,w,"protect",()->send("civ protect"),false);
            button(mainX+w+6,top+170,w,"creative",()->{send("civ observe creative");onClose();},false);
        }
        var input=new net.minecraft.client.gui.components.EditBox(font,mainX,top+199,Math.max(30,mainW-64),19,tr("command"));
        input.setMaxLength(256);input.setValue(commandDraft);input.setResponder(value->commandDraft=value);
        input.setTooltip(net.minecraft.client.gui.components.Tooltip.create(tr("command_help")));addRenderableWidget(input);
        button(mainX+mainW-60,top+199,60,"execute",()->{
            String command=commandDraft.strip();if(command.startsWith("/"))command=command.substring(1);
            if(command.equals("civ")||command.startsWith("civ ")||command.equals("realm")||command.startsWith("realm ")){send(command);onClose();}
        },false);
    }
    private void logControls(){
        var lines=new ArrayList<JsonObject>();
        if(data.has("logs"))for(var e:data.getAsJsonArray("logs")){
            var entry=e.getAsJsonObject();if(!selectedLogs||str(entry,"town").equals(str(data,"selected")))lines.add(entry);
        }
        if(lines.isEmpty())button(mainX,top+119,mainW,"log_empty",()->{},false).active=false;
        int count=Math.max(1,(panelH-161)/24),pages=Math.max(1,(lines.size()+count-1)/count);logPage=Math.min(logPage,pages-1);
        for(int i=logPage*count;i<Math.min(lines.size(),(logPage+1)*count);i++){
            var e=lines.get(i);String type=str(e,"type"),town=str(e,"town");
            Component message=type.startsWith("obstacle_")?name("obstacle",type.substring(9)):name("event",type);
            var label=tr("log_entry",str(e,"day"),town.substring(0,Math.min(8,town.length())),message);
            var row=new RealmButton(mainX,top+119+(i%count)*24,mainW,20,label,()->send("realm view "+town),type.equals("death")||type.startsWith("obstacle_"));
            row.setTooltip(net.minecraft.client.gui.components.Tooltip.create(label.copy().append("\n"+town)));addRenderableWidget(row);
        }
        int w=(mainW-8)/3,y=top+panelH-32;
        button(mainX,y,w,"prev",()->{logPage--;rebuildWidgets();},false).active=logPage>0;
        button(mainX+w+4,y,w,selectedLogs?"logs_selected":"logs_all",()->{selectedLogs=!selectedLogs;logPage=0;rebuildWidgets();},selectedLogs).active=!str(data,"selected").isEmpty();
        button(mainX+2*(w+4),y,w,"next",()->{logPage++;rebuildWidgets();},false).active=logPage+1<pages;
    }
    private void text(GuiGraphicsExtractor g,Component value,int x,int y,int color){g.text(font,font.plainSubstrByWidth(value.getString(),mainW),x,y,color,false);}
    private void rule(GuiGraphicsExtractor g,int y){g.fill(mainX,y,mainX+mainW,y+1,0xff34434d);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        g.fill(0,0,width,height,0xb5081016);
        g.fillGradient(left,top,left+panelW,top+panelH,0xff202e38,INK);g.outline(left,top,panelW,panelH,0xff8c7751);
        g.fill(left,top,left+panelW,top+2,GOLD);g.fill(left+8,top+39,mainX-10,top+panelH-8,0xff131e27);
        g.text(font,font.plainSubstrByWidth(tr("title").getString(),panelW-78),left+13,top+10,GOLD,false);
        g.text(font,font.plainSubstrByWidth(tr("day",number(data,"day"),number(data,"queued")).getString(),panelW-78),left+13,top+25,MUTED,false);
        var d=detail();
        g.enableScissor(mainX,top+64,mainX+mainW,top+panelH-(tab==5?10:66));
        if(confirm==null&&(tab==0||tab==4)){g.pose().pushMatrix();g.pose().translate(0,-scroll);}
        if(confirm!=null)g.textWithWordWrap(font,tr("confirm_"+confirm),mainX,top+76,mainW,GOLD);
        else if(tab==5){
            boolean halted=data.has("healthy")&&!data.get("healthy").getAsBoolean();
            if(adminPage!=1)text(g,halted?tr("halted"):tr(adminPage==2?"log_status":"admin_status",adminPage==2?logPage+1:number(data,"queued")),mainX,top+101,halted?0xffef8585:TEAL);
            if(adminPage==0&&panelH>275){text(g,tr("settlement_target",number(data,"settlementTarget")),mainX,top+246,GOLD);text(g,tr("ai_limits",number(data,"maxDetailed"),number(data,"maxPopulation")),mainX,top+264,MUTED);}
            if(adminPage!=2&&panelH>290)g.textWithWordWrap(font,tr("command_help"),mainX,top+286,mainW,MUTED);
        }
        else if(!data.has("detail"))g.textWithWordWrap(font,tr("empty"),mainX,top+84,mainW,PAPER);
        else switch(tab){
            case 0->overview(g,d);
            case 1->map(g);
            case 2->{text(g,tr("relations",number(d,"relation"),name("treaty",str(d,"treaty"))),mainX,top+76,GOLD);text(g,tr("reputation",number(d,"reputation")),mainX,top+96,TEAL);g.textWithWordWrap(font,tr("diplomacy_help"),mainX,top+211,mainW,MUTED);}
            case 3->people(g,d);
            case 4->research(g,d);
        }
        if(confirm==null&&(tab==0||tab==4))g.pose().popMatrix();
        g.disableScissor();
        super.extractRenderState(g,mx,my,delta);
    }
    private void overview(GuiGraphicsExtractor g,JsonObject d){
        text(g,tr("civilization_stage",Component.translatable("species."+str(d,"species").replace(':','.')),name("stage",str(d,"stage"))),mainX,top+74,GOLD);
        text(g,tr("population",number(d,"population"),number(d,"housing")),mainX,top+93,PAPER);
        text(g,tr("vital",number(d,"births"),number(d,"losses")),mainX,top+113,MUTED);
        text(g,name("growth",str(d,"growth")),mainX,top+132,TEAL);
        text(g,tr("growth_days",number(d,"foodDays"),number(d,"growthDays")),mainX,top+151,PAPER);
        text(g,tr("workers",number(d,"activeWorkers"),number(d,"loadedWorkers"),number(d,"pendingBirths")),mainX,top+170,MUTED);
        rule(g,top+185);
        text(g,tr("objective",name("building",str(d,"objective"))),mainX,top+195,TEAL);
        int progress=number(d,"progress"),total=Math.max(1,number(d,"total"));
        g.fill(mainX,top+213,mainX+mainW,top+219,0xff0b1218);g.fill(mainX,top+213,mainX+(int)((long)mainW*progress/total),top+219,TEAL);
        text(g,tr("physical_progress",number(d,"placed"),progress,total),mainX,top+226,MUTED);
        text(g,name("obstacle",str(d,"obstacle")),mainX,top+243,GOLD);rule(g,top+259);
        text(g,tr("construction_phase",name("phase",str(d,"phase")),number(d,"prepared"),number(d,"preparation")),mainX,top+269,TEAL);
        var stock=d.getAsJsonObject("stock");int columns=Math.max(1,mainW/135),i=0;
        for(var entry:stock.entrySet()){
            int x=mainX+(i%columns)*(mainW/columns),y=top+290+(i/columns)*24;if(i>=24)break;
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
        int count=Math.max(1,(panelH-160)/(data.get("admin").getAsBoolean()?56:40)),start=peoplePage*count;var people=d.getAsJsonArray("people");
        for(int i=start;i<Math.min(start+count,people.size());i++){
            var p=people.get(i).getAsJsonObject();int y=top+77+(i-start)*(data.get("admin").getAsBoolean()?56:40);
            var label=tr("named_citizen",str(p,"name"),name("rank",str(p,"socialRank")));
            g.text(font,font.plainSubstrByWidth(label.getString(),Math.max(10,mainW-96)),mainX,y,PAPER,false);
            g.text(font,font.plainSubstrByWidth(tr("citizen_work",name("role",str(p,"role")),number(p,"rank"),Component.translatable("goal.mobrealms."+str(p,"goal"))).getString(),Math.max(10,mainW-96)),mainX,y+15,MUTED,false);
            if(data.get("admin").getAsBoolean()){
                var diagnostic=workDiagnostic(p);
                g.text(font,font.plainSubstrByWidth(diagnostic.getString(),mainW),mainX,y+30,TEAL,false);rule(g,y+49);
            }else rule(g,y+34);
        }
    }
    private Component workDiagnostic(JsonObject p){
        String material=str(p,"wanted");var materialName=material.isEmpty()?tr("no_material"):new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(material))).getHoverName();
        var age=number(p,"progressAge")<0?tr("no_progress"):tr("progress_age",number(p,"progressAge"));
        return tr("work_diagnostic",materialName,str(p,"target"),age,number(p,"retries"),name("obstacle",str(p,"blockedBy")),number(p,"scanned"));
    }
    private Component residentTooltip(JsonObject person,String citizen){
        var label=tr("resident_detail",str(person,"name"),name("rank",str(person,"socialRank")),name("role",str(person,"role")),number(person,"rank"),citizen).copy();
        if(person.has("wanted"))label.append("\n").append(workDiagnostic(person));return label;
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
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){if((tab==0||tab==4)&&x>=mainX){scroll=Math.max(0,Math.min(650,scroll-(int)(vertical*20)));return true;}return super.mouseScrolled(x,y,horizontal,vertical);}
    @Override public boolean isPauseScreen(){return false;}
}
