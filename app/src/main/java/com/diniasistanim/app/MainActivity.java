package com.diniasistanim.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int BG=0xFF0B1B20, PANEL=0xFF183238, GOLD=0xFFD9B878, WHITE=0xFFF6F1E8, MUTE=0xFFBAC6C7;
    private SharedPreferences prefs;
    private JSONObject index;
    private int page=1, bookmark=0, section=0;
    private boolean arabic=true;
    private final String reader="Ali el-Huzeyfi (Hafs)";

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        prefs=getSharedPreferences("reader", MODE_PRIVATE);
        page=Math.max(1,Math.min(604,prefs.getInt("page",1)));
        bookmark=prefs.getInt("bookmark",0);
        arabic=prefs.getBoolean("arabic",true);
        try (InputStream in=getAssets().open("quran-index.json")) {
            java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();
            byte[] buffer=new byte[8192]; int n;
            while ((n=in.read(buffer))!=-1) out.write(buffer,0,n);
            index=new JSONObject(out.toString(StandardCharsets.UTF_8.name()));
        } catch (Exception ignored) { index=null; }
        render();
    }
    private int dp(int n) { return Math.round(n*getResources().getDisplayMetrics().density); }
    private LinearLayout column() { LinearLayout l=new LinearLayout(this); l.setOrientation(1); return l; }
    private LinearLayout row() { LinearLayout l=new LinearLayout(this); l.setOrientation(0); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private GradientDrawable shape(int c) {
        GradientDrawable d=new GradientDrawable(); d.setColor(c); d.setCornerRadius(dp(14)); return d;
    }
    private TextView label(String value,int size,int color,boolean bold) {
        TextView v=new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color);
        v.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return v;
    }
    private TextView button(String value,Runnable handler,boolean gold) {
        TextView v=label(value,14,gold?BG:WHITE,true);
        v.setGravity(Gravity.CENTER); v.setPadding(dp(12),dp(14),dp(12),dp(14));
        v.setBackground(shape(gold?GOLD:PANEL)); v.setOnClickListener(w->handler.run()); return v;
    }
    private void space(LinearLayout p,int h) { p.addView(new View(this),new LinearLayout.LayoutParams(1,dp(h))); }
    private void item(LinearLayout list,String heading,String detail,Runnable action) {
        LinearLayout card=column();card.setPadding(dp(18),dp(18),dp(18),dp(18));card.setBackground(shape(PANEL));
        card.addView(label(heading,19,WHITE,true));space(card,6);card.addView(label(detail,13,MUTE,false));
        card.setOnClickListener(v->action.run());list.addView(card);space(list,12);
    }
    private void tab(int choice) { section=choice; render(); }
    private void render() {
        LinearLayout root=column();root.setBackgroundColor(BG);
        LinearLayout title=column();title.setPadding(dp(20),dp(18),dp(20),dp(12));
        title.addView(label("DİNİ ASİSTANIM",25,GOLD,true));title.addView(label("Kur’an-ı Kerim",14,MUTE,false));root.addView(title);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        LinearLayout content=column();content.setPadding(dp(16),dp(12),dp(16),dp(22));scroll.addView(content);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
        if(section==0)home(content); else if(section==1)categories(content); else quran(content);
        LinearLayout nav=row();nav.setBackgroundColor(PANEL);
        String[] names={"Başlıklar","Kategoriler","Kur’an"};
        for(int i=0;i<3;i++){ final int id=i; TextView t=label(names[i],14,i==section?GOLD:MUTE,i==section);
            t.setGravity(Gravity.CENTER);t.setOnClickListener(v->tab(id));nav.addView(t,new LinearLayout.LayoutParams(0,dp(58),1)); }
        root.addView(nav);setContentView(root);
    }
    private void home(LinearLayout content) {
        content.addView(label("Esselâmü aleyküm",25,WHITE,true));space(content,10);
        content.addView(label("Kur’an sayfalarını internet olmadan okuyun.",15,MUTE,false));space(content,26);
        item(content,"📖 Kur’an-ı Kerim","604 sayfalık Medine Mushafı",()->tab(2));
        item(content,"🔎 Sure / Ayet Ara","Doğru sayfaya geç",this::searchAyah);
        item(content,"🔖 Kaldığım Yer",bookmark>0?"Sayfa "+bookmark:"Kayıt bulunmuyor",()->{
            if(bookmark>0)goPage(bookmark);else toast("Önce yer imi ekleyin.");
        });
        item(content,"📜 Sureler","114 sure listesi",this::showSurahs);
        content.addView(label("Seslendirme, 5 meal ve video paylaşımı bu ilk sürümde henüz bulunmuyor.",13,MUTE,false));
    }
    private void categories(LinearLayout content) {
        content.addView(label("Kategoriler",24,WHITE,true));space(content,16);
        item(content,"114 Sure","Sure seçerek oku",this::showSurahs);
        item(content,"30 Cüz","Cüz başlangıcına git",this::showJuz);
        item(content,"604 Sayfa","Sayfa numarasıyla aç",this::searchPage);
    }
    private void quran(LinearLayout content) {
        LinearLayout tabs=row();
        tabs.addView(button("ARAPÇA",()->mode(true),arabic),new LinearLayout.LayoutParams(0,-2,1));
        tabs.addView(button("TÜRKÇE MEAL",()->mode(false),!arabic),new LinearLayout.LayoutParams(0,-2,1));
        content.addView(tabs);space(content,10);
        LinearLayout find=row();
        find.addView(button("Sure / Ayet",this::searchAyah,false),new LinearLayout.LayoutParams(0,-2,1));
        find.addView(button("Sayfaya Git",this::searchPage,false),new LinearLayout.LayoutParams(0,-2,1));
        content.addView(find);space(content,14);
        if(arabic) {
            TextView pageLabel=label("Mushaf • Sayfa "+page+" / 604",17,GOLD,true);
            pageLabel.setGravity(Gravity.CENTER);content.addView(pageLabel);space(content,10);
            Bitmap image=pageBitmap(page);
            if(image!=null) {
                ZoomImage viewer=new ZoomImage(this);content.addView(viewer,new LinearLayout.LayoutParams(-1,dp(510)));viewer.setPage(image);
            } else {
                TextView pending=label("Mushaf görselleri bu pakette bulunamadı. İçeriği derleme sırasında ekleyin.",16,MUTE,false);
                pending.setBackground(shape(PANEL));pending.setPadding(dp(20),dp(32),dp(20),dp(32));
                content.addView(pending,new LinearLayout.LayoutParams(-1,dp(320)));
            }
            space(content,10);
            LinearLayout pageControls=row();
            pageControls.addView(button("‹ Önceki",()->goPage(page-1),false),new LinearLayout.LayoutParams(0,-2,1));
            pageControls.addView(button("Sonraki ›",()->goPage(page+1),false),new LinearLayout.LayoutParams(0,-2,1));
            content.addView(pageControls);space(content,8);
            content.addView(button("🔖 Bu sayfayı kaydet",()->{
                bookmark=page;prefs.edit().putInt("bookmark",page).apply();toast("Sayfa kaydedildi.");
            },false));space(content,8);
            content.addView(label("Yakınlaştırmak için iki parmağınızı kullanın.",12,MUTE,false));
        } else {
            item(content,"Türkçe Mealler","5 mealin doğrulanmış metinleri ve dağıtım izinleri henüz eklenmedi.",()->{});
        }
        space(content,18);
        content.addView(label("Arapça okuyucu • tek okuyucu",14,MUTE,true));space(content,8);
        content.addView(button(reader,()->toast("Resmî ses kaydı henüz kurulum paketine eklenmedi."),false));
        space(content,8);
        content.addView(label("Tilavet kaynağı: Kral Fahd Kur’an Basım Kompleksi. " +
                "Yalnızca resmî kaydın 114 suresi doğrulanınca çevrimdışı ses etkinleştirilecek.",
                12,MUTE,false));
    }
    private Bitmap pageBitmap(int number) {
        String path=String.format(Locale.ROOT,"mushaf/pages/%03d.png",number);
        try(InputStream in=getAssets().open(path)) {
            BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeStream(in,null,o);
            int limit=Math.max(1200,getResources().getDisplayMetrics().widthPixels*2);
            int sample=1;while(o.outWidth/(sample*2)>=limit)sample*=2;
            o.inJustDecodeBounds=false;o.inSampleSize=sample;o.inPreferredConfig=Bitmap.Config.RGB_565;
            try(InputStream second=getAssets().open(path)){return BitmapFactory.decodeStream(second,null,o);}
        } catch(Exception ex){return null;}
    }
    private void mode(boolean value){arabic=value;prefs.edit().putBoolean("arabic",value).apply();render();}
    private void goPage(int target){
        if(target<1||target>604){toast("1 ile 604 arasında sayfa seçin.");return;}
        page=target;prefs.edit().putInt("page",page).apply();tab(2);
    }
    private void searchPage(){
        EditText input=new EditText(this);input.setInputType(InputType.TYPE_CLASS_NUMBER);input.setText(""+page);
        new AlertDialog.Builder(this).setTitle("Sayfaya Git").setView(input)
            .setPositiveButton("Aç",(d,w)->{try{goPage(Integer.parseInt(input.getText().toString().trim()));}
            catch(Exception ex){toast("Geçersiz sayfa.");}}).setNegativeButton("İptal",null).show();
    }
    private void searchAyah(){
        if(index==null){toast("Ayet indeksi henüz eklenmedi.");return;}
        LinearLayout fields=column();fields.setPadding(dp(22),0,dp(22),0);
        EditText s=new EditText(this);s.setInputType(InputType.TYPE_CLASS_NUMBER);s.setHint("Sure numarası (1-114)");
        EditText a=new EditText(this);a.setInputType(InputType.TYPE_CLASS_NUMBER);a.setHint("Ayet numarası");
        fields.addView(s);fields.addView(a);
        new AlertDialog.Builder(this).setTitle("Sure / Ayet Ara").setView(fields)
            .setPositiveButton("Bul",(d,w)->{try{
                int surah=Integer.parseInt(s.getText().toString().trim()),ayah=Integer.parseInt(a.getText().toString().trim());
                JSONArray surahs=index.getJSONArray("ayah_pages");
                if(surah<1||surah>114)throw new IllegalArgumentException();
                JSONArray ayahs=surahs.getJSONArray(surah-1);
                if(ayah<1||ayah>ayahs.length())throw new IllegalArgumentException();
                arabic=true;goPage(ayahs.getInt(ayah-1));
            }catch(Exception ex){toast("Geçersiz sure veya ayet.");}})
            .setNegativeButton("İptal",null).show();
    }
    private void showSurahs(){
        if(index==null){toast("Sure indeksi henüz eklenmedi.");return;}
        try{
            JSONArray surahs=index.getJSONArray("surahs");ArrayList<String> labels=new ArrayList<>();
            for(int i=0;i<surahs.length();i++){JSONObject s=surahs.getJSONObject(i);
                labels.add(s.getInt("number")+". "+s.getString("name")+" • "+s.getInt("ayahs")+" ayet");}
            new AlertDialog.Builder(this).setTitle("114 Sure")
                .setItems(labels.toArray(new String[0]),(d,w)->{
                    try{arabic=true;goPage(surahs.getJSONObject(w).getInt("page"));}
                    catch(Exception ex){toast("Sure açılamadı.");}
                }).setNegativeButton("Kapat",null).show();
        }catch(Exception ex){toast("Sure listesi okunamadı.");}
    }
    private void showJuz(){
        if(index==null){toast("Cüz indeksi henüz eklenmedi.");return;}
        try{
            JSONArray pages=index.getJSONArray("juz_pages");
            String[] names=new String[pages.length()];
            for(int i=0;i<names.length;i++)names[i]=(i+1)+". Cüz • Sayfa "+pages.getInt(i);
            new AlertDialog.Builder(this).setTitle("30 Cüz").setItems(names,(d,w)->{
                try{arabic=true;goPage(pages.getInt(w));}catch(Exception ex){toast("Cüz açılamadı.");}
            }).setNegativeButton("Kapat",null).show();
        }catch(Exception ex){toast("Cüz listesi okunamadı.");}
    }
    private void toast(String message){Toast.makeText(this,message,Toast.LENGTH_SHORT).show();}
}
