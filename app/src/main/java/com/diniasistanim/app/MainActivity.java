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
    private static final int BG=0xFF031B16, PANEL=0xFF0A3329, GOLD=0xFFEBC983, WHITE=0xFFFFF6E4, MUTE=0xFFC2CFBF;
    private SharedPreferences prefs;
    private JSONObject index;
    private JSONObject meal;
    private int page=1, bookmark=0, section=8;
    private boolean arabic=true;
    private final String reader="Ali el-Huzeyfi (Hafs)";
    private TilavetPlayer tilavet;
    private int selectedTilavetSurah=1;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        prefs=getSharedPreferences("reader", MODE_PRIVATE);
        page=Math.max(1,Math.min(604,prefs.getInt("page",1)));
        bookmark=prefs.getInt("bookmark",0);
        arabic=prefs.getBoolean("arabic",true);
        selectedTilavetSurah=Math.max(1,Math.min(114,prefs.getInt("tilavet_surah",1)));
        tilavet=new TilavetPlayer(this,()->{if(section==8 || section==2)render();});
        try (InputStream in=getAssets().open("quran-index.json")) {
            java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();
            byte[] buffer=new byte[8192]; int n;
            while ((n=in.read(buffer))!=-1) out.write(buffer,0,n);
            index=new JSONObject(out.toString(StandardCharsets.UTF_8.name()));
        } catch (Exception ignored) { index=null; }
        try (InputStream in=getAssets().open("meal-rowad.json")) {
            java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();
            byte[] buffer=new byte[8192]; int n;
            while ((n=in.read(buffer))!=-1) out.write(buffer,0,n);
            meal=new JSONObject(out.toString(StandardCharsets.UTF_8.name()));
        } catch (Exception ignored) { meal=null; }
        render();
    }
    @Override protected void onDestroy() {
        if(tilavet!=null)tilavet.release();
        super.onDestroy();
    }
    private int dp(int n) { return Math.round(n*getResources().getDisplayMetrics().density); }
    private LinearLayout column() { LinearLayout l=new LinearLayout(this); l.setOrientation(1); return l; }
    private LinearLayout row() { LinearLayout l=new LinearLayout(this); l.setOrientation(0); l.setGravity(Gravity.CENTER_VERTICAL); return l; }

    private GradientDrawable shape(int c) {
        GradientDrawable d=new GradientDrawable();
        d.setColor(c);
        d.setCornerRadius(dp(18));
        d.setStroke(dp(1),0x77D5AD63);
        return d;
    }
    private GradientDrawable luxury(boolean highlighted) {
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            highlighted
                ? new int[]{0xFFEED49A,0xFFC99C4B,0xFFFAE4AC}
                : new int[]{0xFF0F4235,0xFF05241E,0xFF031A17});
        d.setCornerRadius(dp(20));
        d.setStroke(dp(1),highlighted?0xFFFFF0C3:GOLD);
        return d;
    }
    private TextView label(String value,int size,int color,boolean bold) {
        TextView v=new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color);
        v.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return v;
    }

    private TextView button(String value,Runnable handler,boolean gold) {
        TextView v=label(value,14,gold?BG:WHITE,true);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(12),dp(14),dp(12),dp(14));
        v.setBackground(luxury(gold));
        v.setOnClickListener(w->handler.run());
        return v;
    }
    private void space(LinearLayout p,int h) { p.addView(new View(this),new LinearLayout.LayoutParams(1,dp(h))); }

    private void item(LinearLayout list,String heading,String detail,Runnable action) {
        LinearLayout card=column();
        card.setPadding(dp(18),dp(18),dp(18),dp(18));
        card.setBackground(luxury(false));
        card.addView(label(heading,18,GOLD,true));
        space(card,6);
        card.addView(label(detail,13,MUTE,false));
        card.setOnClickListener(v->action.run());
        list.addView(card);
        space(list,12);
    }
    private void tab(int choice) { section=choice; render(); }

    @Override public void onBackPressed() {
        if(section==8)super.onBackPressed();
        else tab(8);
    }
    private String currentSurahName() {
        if(index==null)return "Kur’an-ı Kerim";
        try{
            JSONArray surahs=index.getJSONArray("surahs");
            int number=surahAtPage(page);
            return surahs.getJSONObject(number-1).optString("name","Kur’an-ı Kerim")+" Suresi";
        }catch(Exception ignored){return "Kur’an-ı Kerim";}
    }
    private int currentJuz() {
        if(index==null)return 1;
        try{
            JSONArray starts=index.getJSONArray("juz_pages");
            int juz=1;
            for(int i=0;i<starts.length();i++){
                if(starts.getInt(i)<=page)juz=i+1;
                else break;
            }
            return juz;
        }catch(Exception ignored){return 1;}
    }
    private void navAction(int target) {
        if(target==0){arabic=true;prefs.edit().putBoolean("arabic",true).apply();tab(8);}
        else if(target==1)showSurahs();
        else if(target==2)showJuz();
        else if(target==3){arabic=false;prefs.edit().putBoolean("arabic",false).apply();tab(8);}
        else if(target==4)notReady("Tefsir");
        else tab(9);
    }
    private void render() {
        LinearLayout root=column();
        root.setBackgroundColor(BG);
        LinearLayout top=column();
        top.setPadding(dp(18),dp(10),dp(18),dp(8));
        top.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{0xFF083E31,0xFF031B16}));
        LinearLayout bar=row();
        TextView logo=label("☾  DİNİ ASİSTANIM",14,GOLD,true);
        bar.addView(logo,new LinearLayout.LayoutParams(0,dp(44),1));
        TextView search=button("⌕",this::searchAyah,false);
        bar.addView(search,new LinearLayout.LayoutParams(dp(48),dp(44)));
        bar.addView(new View(this),new LinearLayout.LayoutParams(dp(8),1));
        TextView settings=button("⚙",()->tab(6),false);
        bar.addView(settings,new LinearLayout.LayoutParams(dp(48),dp(44)));
        top.addView(bar);
        TextView ornament=label("✧ ───────── ۞ ───────── ✧",12,GOLD,false);
        ornament.setGravity(Gravity.CENTER);
        top.addView(ornament);
        space(top,5);
        String title=section==8?(arabic?currentSurahName():"Türkçe Meal"):
            section==9?"Diğer Özellikler":section==6?"Ayarlar":
            section==2?"Kur’an-ı Kerim":section==0?"Dini Asistanım":"Kur’an ve İbadet";
        TextView titleLabel=label(title,27,GOLD,true);
        titleLabel.setTypeface(Typeface.SERIF,Typeface.BOLD);
        titleLabel.setGravity(Gravity.CENTER);
        top.addView(titleLabel);
        String subtitle=section==8?currentJuz()+". Cüz  •  Sayfa "+page+" / 604":
            section==9?"Kur’an ile ilgili tüm bölümler":"Zümrüt ve altın koleksiyonu";
        TextView subtitleLabel=label(subtitle,13,WHITE,false);
        subtitleLabel.setGravity(Gravity.CENTER);
        top.addView(subtitleLabel);
        space(top,8);
        root.addView(top);

        ScrollView scroller=new ScrollView(this);
        scroller.setFillViewport(true);
        scroller.setClipToPadding(false);
        LinearLayout area=column();
        area.setPadding(dp(13),dp(8),dp(13),dp(20));
        scroller.addView(area);
        root.addView(scroller,new LinearLayout.LayoutParams(-1,0,1f));
        if(section==0)home(area);
        else if(section==1)categories(area);
        else if(section==2)quran(area);
        else if(section==3)tasbih(area);
        else if(section==4)notes(area);
        else if(section==5)worship(area);
        else if(section==6)settings(area);
        else if(section==7)myBook(area);
        else if(section==8)mushafReader(area);
        else if(section==9)moreQuran(area);

        LinearLayout nav=row();
        nav.setPadding(dp(6),dp(7),dp(6),dp(8));
        nav.setBackgroundColor(0xFF082A22);
        String[] icons={"▣","☷","▦","▤","✧","⊞"};
        String[] names={"Kur’an","Sureler","Cüzler","Meal","Tefsir","Diğer"};
        for(int i=0;i<names.length;i++){
            final int selection=i;
            boolean active=(section==8 && (i==(arabic?0:3)))||(section==9&&i==5);
            LinearLayout tile=column();
            tile.setGravity(Gravity.CENTER);
            tile.setBackground(active?luxury(false):shape(0xFF082A22));
            TextView symbol=label(icons[i],21,active?GOLD:MUTE,true);
            symbol.setGravity(Gravity.CENTER);
            tile.addView(symbol);
            TextView text=label(names[i],11,active?GOLD:MUTE,active);
            text.setGravity(Gravity.CENTER);
            tile.addView(text);
            tile.setOnClickListener(v->navAction(selection));
            nav.addView(tile,new LinearLayout.LayoutParams(0,dp(60),1));
        }
        root.addView(nav);
        setContentView(root);
    }
    private void sectionTitle(LinearLayout parent,String title,String subtitle) {
        parent.addView(label(title,24,WHITE,true));
        space(parent,5);
        parent.addView(label(subtitle,14,MUTE,false));
        space(parent,18);
    }
    private void hero(LinearLayout parent) {
        GradientDrawable backdrop=new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,new int[]{0xFF2F6263,0xFF16363B,0xFF11282F});
        backdrop.setCornerRadius(dp(22));
        LinearLayout card=column();
        card.setPadding(dp(20),dp(24),dp(20),dp(22));
        card.setBackground(backdrop);
        card.addView(label("GÜNLÜK YOL ARKADAŞIN",11,GOLD,true));
        space(card,12);
        card.addView(label("Huzurla, adım adım.",25,WHITE,true));
        space(card,8);
        card.addView(label("Kur’an oku, tesbihatını takip et ve notlarını sakla.",14,WHITE,false));
        space(card,20);
        card.addView(button("📖  Kur’an okumaya devam et   ›",()->goPage(page),true));
        parent.addView(card);
    }
    private void categoryTile(LinearLayout parent,String icon,String name,
                              String detail,boolean ready,Runnable open) {
        LinearLayout tile=column();
        tile.setPadding(dp(14),dp(16),dp(12),dp(15));
        tile.setMinimumHeight(dp(146));
        tile.setBackground(shape(PANEL));
        tile.addView(label(icon,29,GOLD,false));
        space(tile,9);
        tile.addView(label(name,16,WHITE,true));
        space(tile,4);
        tile.addView(label(detail,12,MUTE,false));
        space(tile,7);
        tile.addView(label(ready?"● Kullanıma hazır":"○ Planlanıyor",11,
            ready?0xFF93D7B1:MUTE,false));
        tile.setOnClickListener(v->{
            if(ready && open!=null)open.run();
            else toast("Bu bölüm henüz hazırlanıyor.");
        });
        parent.addView(tile,new LinearLayout.LayoutParams(0,-2,1));
    }
    private void categoryPair(LinearLayout body,String icon1,String name1,
        String detail1,boolean ready1,Runnable run1,
        String icon2,String name2,String detail2,boolean ready2,Runnable run2) {
        LinearLayout pair=row();
        categoryTile(pair,icon1,name1,detail1,ready1,run1);
        pair.addView(new View(this),new LinearLayout.LayoutParams(dp(10),1));
        categoryTile(pair,icon2,name2,detail2,ready2,run2);
        body.addView(pair);
        space(body,10);
    }
    private int todayCount() {
        int count=0;
        for(int i=0;i<5;i++)if(prefs.getBoolean(todayKey(i),false))count++;
        return count;
    }
    private String todayKey(int i) {
        return "prayer_"+java.time.LocalDate.now()+"_"+i;
    }
    private void home(LinearLayout content) {
        hero(content);
        space(content,23);
        content.addView(label("Bugün",20,WHITE,true));
        space(content,9);
        item(content,"◷  İbadet Takibim",todayCount()+" / 5 namaz işaretlendi",
             ()->tab(5));
        item(content,"⌑  Kaldığım Sayfa","Mushaf • Sayfa "+page,
             ()->goPage(page));
        space(content,10);
        content.addView(label("Kategorileri keşfet",20,WHITE,true));
        space(content,11);
        categoryPair(content,"📖","Kur’an","Mushaf ve Türkçe meal",true,()->goPage(page),
             "◉","Tesbihat","Dijital zikir sayacı",true,()->tab(3));
        categoryPair(content,"☼","İbadet Takibi","Günlük işaretleme",true,()->tab(5),
             "✎","Notlarım","Kişisel kayıtlar",true,()->tab(4));
        content.addView(button("Tüm kategorileri gör   ›",()->tab(1),false));
    }
    private void categories(LinearLayout content) {
        sectionTitle(content,"Kategoriler","Dini Asistanım’ın bölümleri");
        content.addView(label("OKUMA VE İBADET",12,GOLD,true));
        space(content,10);
        categoryPair(content,"📖","Kur’an-ı Kerim","604 sayfa • Türkçe meal",true,()->goPage(page),
             "◉","Tesbihat","Çevrimdışı sayaç",true,()->tab(3));
        categoryPair(content,"☼","İbadet Takibi","Günlük namaz kaydı",true,()->tab(5),
             "✎","Manevi Notlar","Kişisel not defteri",true,()->tab(4));
        space(content,14);
        item(content,"📖  Kur’an bölümünü aç",
             "Sureler, 30 cüz, Mushaf ve meal tek sayfada",()->goPage(page));
        space(content,8);
        content.addView(label("GELECEK BÖLÜMLER",12,GOLD,true));
        space(content,10);
        categoryPair(content,"☾","Dua Kitaplığı","Doğrulanmış dualar",false,null,
             "⌖","Kıble Bulucu","Pusula desteği",false,null);
        categoryPair(content,"◷","Namaz Vakitleri","Konuma göre hesaplama",false,null,
             "✦","Esmaül Hüsna","Kaynaklı isim listesi",false,null);
    }

    private void tasbih(LinearLayout content) {
        sectionTitle(content,"Tesbihat","İnternetsiz dijital zikir sayacı");
        final String[] options={"Sübhânallah","Elhamdülillah","Allahu ekber"};
        final int active=Math.max(0,Math.min(2,prefs.getInt("dhikr_mode",0)));
        LinearLayout selectors=row();
        for(int i=0;i<3;i++){
            final int chosen=i;
            TextView b=button(options[i],()->{
                prefs.edit().putInt("dhikr_mode",chosen).apply();render();
            },i==active);
            b.setTextSize(11);
            selectors.addView(b,new LinearLayout.LayoutParams(0,-2,1));
        }
        content.addView(selectors);
        space(content,20);
        LinearLayout panel=column();
        panel.setPadding(dp(18),dp(24),dp(18),dp(24));
        panel.setGravity(Gravity.CENTER);
        panel.setBackground(shape(PANEL));
        panel.addView(label(options[active],20,GOLD,true));
        space(panel,18);
        TextView number=label(""+prefs.getInt("dhikr_"+active,0),62,WHITE,true);
        number.setGravity(Gravity.CENTER);
        panel.addView(number);
        space(panel,8);
        panel.addView(label("Hedef: 33 • 99 • serbest",13,MUTE,false));
        space(panel,22);
        TextView add=button("＋  Zikir Ekle",()->{
            int next=prefs.getInt("dhikr_"+active,0)+1;
            prefs.edit().putInt("dhikr_"+active,next).apply();
            number.setText(""+next);
        },true);
        panel.addView(add,new LinearLayout.LayoutParams(-1,-2));
        space(panel,12);
        panel.addView(button("Sayacı sıfırla",()->new AlertDialog.Builder(this)
            .setTitle("Sayacı sıfırla")
            .setMessage("Bu tesbih sayısı sıfırlansın mı?")
            .setPositiveButton("Sıfırla",(d,w)->{
                prefs.edit().putInt("dhikr_"+active,0).apply();
                number.setText("0");
            }).setNegativeButton("Vazgeç",null).show(),false));
        content.addView(panel);
        space(content,18);
        content.addView(label("Sayılar bu cihazda saklanır.",13,MUTE,false));
    }
    private void worship(LinearLayout content) {
        sectionTitle(content,"İbadet Takibi","Kişisel günlük işaretleme • Namaz vakti bildirimi değildir");
        content.addView(label("Bugünün kaydı • "+java.time.LocalDate.now(),14,GOLD,true));
        space(content,9);
        item(content,"Tamamlanan",todayCount()+" / 5 namaz",()->{});
        final String[] names={"Sabah","Öğle","İkindi","Akşam","Yatsı"};
        for(int i=0;i<5;i++){
            final int target=i;
            boolean done=prefs.getBoolean(todayKey(i),false);
            String symbol=done?"✓":"○";
            item(content,symbol+"  "+names[i]+" Namazı",done?"İşaretlendi":"Henüz işaretlenmedi",
                ()->{prefs.edit().putBoolean(todayKey(target),!done).apply();render();});
        }
        content.addView(label("Bu takip sadece sizin kaydınızdır. Namazın kılındığını doğrulamaz.",12,MUTE,false));
    }
    private JSONArray storedNotes(){
        try{return new JSONArray(prefs.getString("saved_notes","[]"));}
        catch(Exception error){return new JSONArray();}
    }
    private void editNote(int index) {
        JSONArray saved=storedNotes();
        String original="";
        if(index>=0)original=saved.optJSONObject(index)!=null?
             saved.optJSONObject(index).optString("text",""):"";
        EditText editor=new EditText(this);
        editor.setMinLines(4);editor.setMaxLines(8);
        editor.setGravity(Gravity.TOP);
        editor.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        editor.setHint("Düşüncelerini veya okumak istediğin bir ayeti not et...");
        editor.setText(original);
        LinearLayout wrapper=column();wrapper.setPadding(dp(20),dp(8),dp(20),0);
        wrapper.addView(editor);
        new AlertDialog.Builder(this).setTitle(index<0?"Yeni Not":"Notu Düzenle")
            .setView(wrapper)
            .setPositiveButton("Kaydet",(d,w)->{
                String value=editor.getText().toString().trim();
                if(value.isEmpty()){toast("Boş not kaydedilmez.");return;}
                try {
                    JSONObject object=new JSONObject();
                    object.put("text",value);
                    object.put("date",java.time.LocalDate.now().toString());
                    if(index>=0)saved.put(index,object);
                    else saved.put(object);
                    prefs.edit().putString("saved_notes",saved.toString()).apply();
                    tab(4);
                }catch(Exception e){toast("Not kaydedilemedi.");}
            }).setNegativeButton("İptal",null).show();
    }
    private void noteOptions(int index) {
        new AlertDialog.Builder(this).setTitle("Not işlemleri")
          .setItems(new String[]{"Düzenle","Sil"},(dialog,selected)->{
              if(selected==0)editNote(index);
              else new AlertDialog.Builder(this).setTitle("Notu sil")
                  .setMessage("Bu not silinsin mi?")
                  .setPositiveButton("Sil",(d,w)->{
                      JSONArray old=storedNotes();
                      JSONArray newer=new JSONArray();
                      for(int i=0;i<old.length();i++)if(i!=index)newer.put(old.opt(i));
                      prefs.edit().putString("saved_notes",newer.toString()).apply();
                      tab(4);
                  }).setNegativeButton("Vazgeç",null).show();
          }).show();
    }
    private void notes(LinearLayout content) {
        sectionTitle(content,"Manevi Notlarım","Yazdıkların bu cihazda saklanır");
        content.addView(button("＋  Yeni Not Oluştur",()->editNote(-1),true));
        space(content,18);
        JSONArray saved=storedNotes();
        if(saved.length()==0){
            item(content,"Henüz not yok","Yeni Not Oluştur seçeneğiyle başlayabilirsin.",()->editNote(-1));
            return;
        }
        for(int i=saved.length()-1;i>=0;i--){
            final int selected=i;
            JSONObject note=saved.optJSONObject(i);
            if(note==null)continue;
            String value=note.optString("text","");
            String display=value.length()>160?value.substring(0,160)+"…":value;
            item(content,"✎  "+display,note.optString("date","")+"   •   Düzenle / Sil",
                 ()->noteOptions(selected));
        }
    }
    private void myBook(LinearLayout content) {
        sectionTitle(content,"Defterim","Okumaların ve kişisel kayıtların");
        item(content,"📖  Son Okuduğum Sayfa","Sayfa "+page,()->tab(2));
        item(content,"⚑  Yer İmim",bookmark>0?"Sayfa "+bookmark:"Henüz kaydedilmedi",
             ()->{if(bookmark>0)goPage(bookmark);else tab(2);});
        item(content,"✎  Manevi Notlarım",storedNotes().length()+" kayıt",
             ()->tab(4));
        item(content,"◉  Tesbihat","Kayıtlı zikir sayaçları",()->tab(3));
        item(content,"☼  Bugünkü İbadet","Namaz takibi: "+todayCount()+" / 5",
             ()->tab(5));
    }
    private void settings(LinearLayout content) {
        sectionTitle(content,"Ayarlar ve Bilgiler","Dini Asistanım • Android");
        item(content,"◎  Çevrimdışı Kullanım","Kur’an ve Türkçe meal bu cihazda saklanır.",()->{});
        item(content,"✦  Mushaf Görünümü","Özgün Arapça Mushaf renkleri korunur.",()->{});
        item(content,"ℹ  İçerik Kaynakları","Rowad Tercüme Merkezi • QuranEnc.com • v1.0.4",
             ()->new AlertDialog.Builder(this).setTitle("İçerik Bilgisi")
               .setMessage("Arapça Mushaf: Medine Mushafı, 604 sayfa.\n\n"+
                   "Türkçe meal: Rowad Tercüme Merkezi, QuranEnc.com, v1.0.4.\n\n"+
                   (tilavet!=null && tilavet.allBundled() ? "Arapça tilavet: Ali el-Huzeyfi (Hafs). Kaynak: MP3Quran.net. 114 surenin ses kaydı çevrimdışı kullanılabilir." : "Arapça tilavet: Ali el-Huzeyfi (Hafs). Kaynak: MP3Quran.net. Ses arşivi henüz kurulum paketine eklenmedi."))
               .setPositiveButton("Kapat",null).show());
        item(content,"▤  Notlarımı Aç","Kişisel notlarına ulaş",()->tab(4));
        space(content,18);
        content.addView(label("Henüz yapılmayan özellikler: "+
            (tilavet!=null && tilavet.allBundled() ? "" : "çevrimdışı ses arşivi, ")+
            "video paylaşımı, kıble ve namaz vakitleri.",12,MUTE,false));
    }

    private void quran(LinearLayout content) {
        GradientDrawable banner=new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{0xFF194C46,0xFF0D3937,0xFF0D292F});
        banner.setCornerRadius(dp(22));
        banner.setStroke(dp(1),0xFFA7894E);
        LinearLayout heading=column();
        heading.setPadding(dp(22),dp(24),dp(22),dp(24));
        heading.setBackground(banner);
        heading.addView(label("DİNİ ASİSTANIM  /  KUR’AN",12,GOLD,true));
        space(heading,15);
        heading.addView(label("Kur’an-ı Kerim",30,WHITE,true));
        space(heading,10);
        heading.addView(label("604 sayfalık Medine Mushafı",15,WHITE,false));
        space(heading,3);
        heading.addView(label("Arapça Mushaf • Türkçe meal • İnternetsiz",13,MUTE,false));
        space(heading,15);
        heading.addView(label("۞",30,GOLD,false));
        content.addView(heading);

        space(content,18);
        TextView continueCard=button("📖  Kaldığın yerden devam et   •   Sayfa "+page+"    ›",
            ()->goPage(page),true);
        continueCard.setTextSize(15);
        content.addView(continueCard);
        space(content,17);
        content.addView(label("OKUMAYA BAŞLA",12,GOLD,true));
        space(content,10);

        item(content,"📖  Mushafı 1. sayfadan aç",
            "Özgün Arapça Mushaf sayfaları",
            ()->{arabic=true;prefs.edit().putBoolean("arabic",true).apply();goPage(1);});
        item(content,"⌕  Sayfa numarasına git",
            "1 ile 604 arasında bir sayfa seç",this::searchPage);
        item(content,"☷  Sureler",
            "114 sure arasından birini aç",this::showSurahs);
        item(content,"▦  Cüzler",
            "30 cüzün başlangıç sayfaları",this::showJuz);

        space(content,8);
        content.addView(label("MEAL VE ARAÇLAR",12,GOLD,true));
        space(content,10);
        item(content,"📜  Türkçe Meal",
            "Rowad Tercüme Merkezi • QuranEnc.com • v1.0.4",
            ()->{arabic=false;prefs.edit().putBoolean("arabic",false).apply();goPage(page);});
        item(content,"⌕  Sure / Ayet Ara",
            "Sure ve ayet numarasıyla Mushaf’ta bul",this::searchAyah);
        item(content,"⚑  Yer İmim",
            bookmark>0?"Kayıtlı sayfa: "+bookmark:"Henüz yer imi eklenmedi",
            ()->{if(bookmark>0)goPage(bookmark);else toast("Mushaf ekranından yer imi ekleyin.");});

        space(content,8);
        content.addView(label("TİLAVET",12,GOLD,true));
        space(content,10);
        item(content,"♫  Arapça Okuyucu",
            tilavet!=null && tilavet.allBundled() ? "Ali el-Huzeyfi (Hafs) • 114 sure çevrimdışı hazır" : "Ali el-Huzeyfi (Hafs) • ses paketi bekleniyor",
            ()->new AlertDialog.Builder(this)
                .setTitle("Arapça Tilavet")
                .setMessage("Okuyucu: Ali el-Huzeyfi (Hafs).\n\n"
                    +(tilavet!=null && tilavet.allBundled() ? "114 sure çevrimdışı dinlenebilir. Ayet bazlı takip henüz doğrulanmadı." : "MP3Quran.net ses dosyaları doğrulanıp kurulum paketine eklenince çevrimdışı dinleme açılacak."))
                .setPositiveButton("Anladım",null).show());

        content.addView(label("Kur’an ve meal sayfaları çevrimdışıdır. "
            +(tilavet!=null && tilavet.allBundled() ? "Arapça tilavet hazır. " : "Arapça ses arşivi henüz eksik. ")+
            "Video paylaşımı henüz hazır değil.",12,MUTE,false));
    }


    private void mushafReader(LinearLayout content) {
        LinearLayout tabs=row();
        tabs.addView(button("ARAPÇA MUSHAF",()->mode(true),arabic),
            new LinearLayout.LayoutParams(0,dp(48),1));
        tabs.addView(new View(this),new LinearLayout.LayoutParams(dp(9),1));
        tabs.addView(button("TÜRKÇE MEAL",()->mode(false),!arabic),
            new LinearLayout.LayoutParams(0,dp(48),1));
        content.addView(tabs);
        space(content,12);
        if(arabic){
            LinearLayout frame=column();
            frame.setPadding(dp(5),dp(5),dp(5),dp(5));
            frame.setBackground(luxury(true));
            Bitmap image=pageBitmap(page);
            if(image!=null){
                int width=Math.round(getResources().getDisplayMetrics().widthPixels/
                    getResources().getDisplayMetrics().density)-36;
                int proportional=Math.round(width*(float)image.getHeight()/image.getWidth());
                int height=Math.max(390,Math.min(650,proportional));
                ZoomImage viewer=new ZoomImage(this);
                frame.addView(viewer,new LinearLayout.LayoutParams(-1,dp(height)));
                viewer.setPage(image);
            }else{
                TextView pending=label("Bu Mushaf sayfasının görseli pakete eklenmemiş.",16,BG,true);
                pending.setGravity(Gravity.CENTER);
                frame.addView(pending,new LinearLayout.LayoutParams(-1,dp(400)));
            }
            content.addView(frame);
            space(content,12);
            LinearLayout pages=row();
            pages.addView(button("‹  Önceki",()->goPage(page-1),false),
                new LinearLayout.LayoutParams(0,dp(50),1));
            TextView number=label(page+" / 604",14,GOLD,true);
            number.setGravity(Gravity.CENTER);
            pages.addView(number,new LinearLayout.LayoutParams(dp(86),dp(50)));
            pages.addView(button("Sonraki  ›",()->goPage(page+1),false),
                new LinearLayout.LayoutParams(0,dp(50),1));
            content.addView(pages);
            space(content,10);
            tilavetControls(content);
            space(content,10);
            content.addView(label("Mushafı iki parmakla yakınlaştırabilirsiniz.",12,MUTE,false));
        }else{
            showTurkishMeal(content);
        }
        space(content,12);
        LinearLayout tools=row();
        tools.addView(button("⌕  Ayet Ara",this::searchAyah,false),
            new LinearLayout.LayoutParams(0,-2,1));
        tools.addView(new View(this),new LinearLayout.LayoutParams(dp(8),1));
        tools.addView(button("☷  Sayfa Git",this::searchPage,false),
            new LinearLayout.LayoutParams(0,-2,1));
        content.addView(tools);
    }
    /** Only verified, packaged offline recordings are offered for playback. */

    private void tilavetControls(LinearLayout content){
        LinearLayout panel=column();
        panel.setPadding(dp(13),dp(15),dp(13),dp(15));
        panel.setBackground(luxury(false));
        TextView heading=label("♫  ARAPÇA KUR’AN OKUYUCULARI",14,GOLD,true);
        panel.addView(heading);
        space(panel,10);
        LinearLayout choices=row();
        choices.addView(button("♙  Erkek Okuyucu",this::chooseTilavetSurah,true),
            new LinearLayout.LayoutParams(0,dp(52),1));
        choices.addView(new View(this),new LinearLayout.LayoutParams(dp(8),1));
        choices.addView(button("♧  Bayan Okuyucu",()->new AlertDialog.Builder(this)
                .setTitle("Bayan Okuyucular")
                .setMessage("Bu sürümde doğrulanmış bayan Arapça tilavet ses dosyaları bulunmuyor.")
                .setPositiveButton("Kapat",null).show(),false),
            new LinearLayout.LayoutParams(0,dp(52),1));
        panel.addView(choices);
        space(panel,9);
        String surah="Sure "+selectedTilavetSurah;
        if(index!=null)try{
            JSONArray surahs=index.getJSONArray("surahs");
            surah=surahs.getJSONObject(selectedTilavetSurah-1).optString("name",surah);
        }catch(Exception ignored){}
        panel.addView(label("Ali el-Huzeyfi (Hafs)  •  "+surah,13,WHITE,true));
        space(panel,6);
        boolean available=tilavet!=null && tilavet.bundled(selectedTilavetSurah);
        if(tilavet!=null && tilavet.loading())
            panel.addView(label("Ses kaydı hazırlanıyor...",12,MUTE,false));
        else if(!available)
            panel.addView(label("Bu sure için çevrimdışı ses kaydı yok.",12,MUTE,false));
        else if(tilavet.playing())
            panel.addView(label("Tilavet çalıyor",12,GOLD,false));
        if(tilavet!=null && !tilavet.error().isEmpty())
            panel.addView(label(tilavet.error(),12,MUTE,false));
        space(panel,10);
        LinearLayout controls=row();
        boolean same=tilavet!=null && tilavet.surah()==selectedTilavetSurah;
        String caption=tilavet!=null && tilavet.loading()?"Hazırlanıyor":
            same && tilavet.playing()?"❚❚ Duraklat":same?"▶ Devam":"▶ Sureyi Dinle";
        controls.addView(button(caption,()->{
            if(tilavet!=null)tilavet.toggle(selectedTilavetSurah);
        },available && tilavet!=null && !tilavet.loading()),
            new LinearLayout.LayoutParams(0,dp(48),1));
        controls.addView(new View(this),new LinearLayout.LayoutParams(dp(8),1));
        controls.addView(button("■ Durdur",()->{
            if(tilavet!=null)tilavet.stop();
        },false),new LinearLayout.LayoutParams(0,dp(48),1));
        panel.addView(controls);
        space(panel,7);
        panel.addView(label("Ses kaydı sure bazlıdır. Ayet bazlı takip henüz mevcut değildir.",
            11,MUTE,false));
        content.addView(panel);
    }
    private void chooseTilavetSurah(){
        if(index==null){toast("Sure listesi bu pakette bulunamadı.");return;}
        try{
            JSONArray surahs=index.getJSONArray("surahs");
            String[] labels=new String[surahs.length()];
            for(int i=0;i<labels.length;i++){
                JSONObject entry=surahs.getJSONObject(i);
                labels[i]=(i+1)+". "+entry.optString("name","Sure "+(i+1));
            }
            new AlertDialog.Builder(this).setTitle("Dinlenecek sure")
                .setSingleChoiceItems(labels,selectedTilavetSurah-1,(d,which)->{
                    selectedTilavetSurah=which+1;
                    prefs.edit().putInt("tilavet_surah",selectedTilavetSurah).apply();
                    d.dismiss();
                    try{goPage(surahs.getJSONObject(which).getInt("page"));}
                    catch(Exception ex){render();}
                }).setNegativeButton("İptal",null).show();
        }catch(Exception e){toast("Sure listesi açılamadı.");}
    }
    private void showTurkishMeal(LinearLayout content) {
        content.addView(label("Türkçe Meal • Sayfa "+page+" / 604",19,GOLD,true));
        space(content,7);
        content.addView(label("Rowad Tercüme Merkezi • QuranEnc.com • v1.0.4",12,MUTE,false));
        space(content,12);
        if(index==null || meal==null) {
            content.addView(label("Doğrulanmış Türkçe meal bu APK içine yüklenmedi.",16,MUTE,false));
            return;
        }
        try {
            JSONArray pages=index.getJSONArray("ayah_pages");
            JSONArray translations=meal.getJSONArray("surahs");
            int visible=0;
            for(int s=0;s<pages.length();s++){
                JSONArray positions=pages.getJSONArray(s);
                JSONArray verses=translations.getJSONArray(s);
                for(int a=0;a<positions.length();a++){
                    if(positions.getInt(a)!=page)continue;
                    if(a>=verses.length())throw new IllegalStateException("Ayet eksik");
                    JSONObject entry=verses.getJSONObject(a);
                    LinearLayout card=column();
                    card.setPadding(dp(14),dp(14),dp(14),dp(14));
                    card.setBackground(shape(PANEL));
                    card.addView(label("Sure "+(s+1)+" • Ayet "+(a+1),13,GOLD,true));
                    space(card,7);
                    TextView text=label("",17,WHITE,false);
                    text.setText(android.text.Html.fromHtml(
                        entry.getString("translation"),android.text.Html.FROM_HTML_MODE_LEGACY));
                    card.addView(text);
                    if(!entry.isNull("footnotes")){
                        String note=entry.optString("footnotes","");
                        if(!note.isEmpty()){
                            space(card,6);
                            TextView footnote=label("",12,MUTE,false);
                            footnote.setText(android.text.Html.fromHtml(note,
                                android.text.Html.FROM_HTML_MODE_LEGACY));
                            card.addView(footnote);
                        }
                    }
                    content.addView(card);
                    space(content,10);
                    visible++;
                }
            }
            if(visible==0)content.addView(label("Bu sayfaya karşılık gelen meal bulunamadı.",15,MUTE,false));
        } catch(Exception exception) {
            content.addView(label("Meal eşleştirme hatası. İçerik doğrulanmalı.",15,MUTE,false));
        }
        LinearLayout navigation=row();
        navigation.addView(button("‹ Önceki",()->goPage(page-1),false),
            new LinearLayout.LayoutParams(0,-2,1));
        navigation.addView(button("Sonraki ›",()->goPage(page+1),false),
            new LinearLayout.LayoutParams(0,-2,1));
        content.addView(navigation);
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
    private int surahAtPage(int target){
        if(index==null)return selectedTilavetSurah;
        try{
            JSONArray surahs=index.getJSONArray("surahs");
            int number=1;
            for(int i=0;i<surahs.length();i++){
                int begin=surahs.getJSONObject(i).getInt("page");
                if(begin>target)break;
                number=i+1;
            }
            return number;
        }catch(Exception ignored){return selectedTilavetSurah;}
    }
    private void goPage(int target){
        if(target<1||target>604){toast("1 ile 604 arasında sayfa seçin.");return;}
        int nextSurah=surahAtPage(target);
        if(nextSurah!=selectedTilavetSurah){
            selectedTilavetSurah=nextSurah;
            prefs.edit().putInt("tilavet_surah",nextSurah).apply();
        }
        page=target;prefs.edit().putInt("page",page).apply();tab(8);
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

    private void notReady(String title){
        new AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage("Bu bölümün doğrulanmış içeriği henüz uygulamaya eklenmedi.")
            .setPositiveButton("Kapat",null).show();
    }
    private void readerPicker() {
        new AlertDialog.Builder(this).setTitle("Arapça Okuyucular")
            .setItems(new String[]{"Erkek okuyucu: Ali el-Huzeyfi (Hafs)",
                                   "Bayan okuyucular"},(d,which)->{
                if(which==0){arabic=true;tab(8);chooseTilavetSurah();}
                else new AlertDialog.Builder(this)
                    .setTitle("Bayan Okuyucular")
                    .setMessage("Bayan okuyucu ses dosyaları bu APK içinde henüz bulunmuyor.")
                    .setPositiveButton("Kapat",null).show();
            }).show();
    }
    private void hatimStatus() {
        new AlertDialog.Builder(this).setTitle("Hatim Takibi")
            .setMessage("Son açtığınız Mushaf sayfası: "+page+" / 604.\n\n"+
                "Bu kayıt tamamlanan sayfaları ayrı ayrı doğrulamaz. "+
                "Ayrıntılı hatim takibi hazırlanıyor.")
            .setPositiveButton("Mushafı Aç",(d,w)->tab(8))
            .setNegativeButton("Kapat",null).show();
    }
    private void feature(LinearLayout row,String icon,String name,String description,
                         boolean working,Runnable action) {
        LinearLayout tile=column();
        tile.setGravity(Gravity.CENTER_HORIZONTAL);
        tile.setPadding(dp(9),dp(18),dp(9),dp(16));
        tile.setMinimumHeight(dp(136));
        tile.setBackground(luxury(false));
        TextView emblem=label(icon,30,GOLD,true);
        emblem.setGravity(Gravity.CENTER);
        tile.addView(emblem);
        space(tile,6);
        TextView title=label(name,16,WHITE,true);
        title.setGravity(Gravity.CENTER);
        tile.addView(title);
        space(tile,5);
        TextView detail=label(description,11,MUTE,false);
        detail.setGravity(Gravity.CENTER);
        tile.addView(detail);
        space(tile,7);
        TextView status=label(working?"Kullanıma hazır":"Hazırlanıyor",10,
            working?GOLD:MUTE,false);
        status.setGravity(Gravity.CENTER);
        tile.addView(status);
        tile.setOnClickListener(v->action.run());
        row.addView(tile,new LinearLayout.LayoutParams(0,-2,1f));
    }
    private void moreQuran(LinearLayout content) {
        TextView heading=label("✧  KUR’AN REHBERİ  ✧",13,GOLD,true);
        heading.setGravity(Gravity.CENTER);
        content.addView(heading);
        space(content,12);
        String[] icons={"⌕","♫","۞","◈","◷","☷","✦","▤","آ","♬","☰","⚙"};
        String[] titles={"Ayet Ara","Okuyucular","Tecvid","Ezber","Hatim Takibi",
            "Nüzul Sırası","Secde Ayetleri","Kur’an Fihristi",
            "Kelime Meali","Kıraatler","Tefsir","Ayarlar"};
        String[] details={"Sure ve ayet bul","Erkek & Bayan","Okuma kuralları",
            "Ayet ezberleme","Son okunan sayfa","İniş sıralaması",
            "Tilavet secdesi","Konulara göre ayet","Kelime anlamları",
            "Farklı okuyuşlar","Ayet açıklamaları","Uygulama ayarları"};
        boolean[] ready={true,true,false,false,true,false,false,false,false,false,false,true};
        Runnable[] actions={
            this::searchAyah,this::readerPicker,()->notReady("Tecvid"),
            ()->notReady("Ezber"),this::hatimStatus,
            ()->notReady("Nüzul Sırası"),()->notReady("Secde Ayetleri"),
            ()->notReady("Kur’an Fihristi"),()->notReady("Kelime Meali"),
            ()->notReady("Kıraatler"),()->notReady("Tefsir"),
            ()->tab(6)};
        for(int i=0;i<titles.length;i+=2){
            LinearLayout pair=row();
            feature(pair,icons[i],titles[i],details[i],ready[i],actions[i]);
            pair.addView(new View(this),new LinearLayout.LayoutParams(dp(10),dp(1)));
            feature(pair,icons[i+1],titles[i+1],details[i+1],ready[i+1],actions[i+1]);
            content.addView(pair);
            space(content,10);
        }
        content.addView(label("Hazırlanıyor yazan bölümler henüz kullanılabilir değildir.",
            12,MUTE,false));
    }

    private void toast(String message){Toast.makeText(this,message,Toast.LENGTH_SHORT).show();}
}
