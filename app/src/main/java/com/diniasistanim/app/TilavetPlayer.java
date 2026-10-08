package com.diniasistanim.app;

import android.app.Activity;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/** Offline, surah-level playback of the bundled Hafs recording. */
public final class TilavetPlayer {
    private final Activity activity;
    private final Runnable changed;
    private MediaPlayer player;
    private int surah=0;
    private int generation=0;
    private boolean loading=false;
    private String error="";

    public TilavetPlayer(Activity activity, Runnable changed) {
        this.activity=activity;
        this.changed=changed;
    }
    public int surah(){return surah;}
    public boolean loading(){return loading;}
    public boolean playing(){try{return player!=null && player.isPlaying();}catch(Exception ex){return false;}}
    public String error(){return error;}
    public boolean bundled(int n){
        if(n<1||n>114)return false;
        String name=String.format(java.util.Locale.ROOT,"audio-ar/huzayfi-hafs/%03d.ogg",n);
        try(InputStream in=activity.getAssets().open(name)){return in.read()!=-1;}
        catch(Exception e){return false;}
    }
    private void notifyChanged(){activity.runOnUiThread(changed);}
    public void toggle(int number){
        if(number<1||number>114)return;
        if(player!=null && surah==number && !loading){
            try {
                if(player.isPlaying())player.pause(); else player.start();
                error="";notifyChanged();return;
            }catch(Exception e){/* release and re-open below */}
        }
        stopInternal();
        if(!bundled(number)){
            error="Bu surenin ses kaydı kurulum paketinde yok.";
            notifyChanged();return;
        }
        surah=number;loading=true;error="";int task=++generation;
        notifyChanged();
        new Thread(()->{
            File target=new File(activity.getCacheDir(),String.format(
                java.util.Locale.ROOT,"huzayfi-%03d.ogg",number));
            String asset=String.format(java.util.Locale.ROOT,"audio-ar/huzayfi-hafs/%03d.ogg",number);
            try(InputStream in=activity.getAssets().open(asset);
                FileOutputStream out=new FileOutputStream(target)){
                byte[] bytes=new byte[65536];int read;
                while((read=in.read(bytes))!=-1){
                    if(task!=generation)return;
                    out.write(bytes,0,read);
                }
                out.flush();
                if(target.length()<4096)throw new IllegalStateException("Ses dosyası boş");
                activity.runOnUiThread(()->{
                    if(task!=generation)return;
                    try{
                        MediaPlayer mp=new MediaPlayer();
                        mp.setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
                        mp.setDataSource(target.getAbsolutePath());
                        mp.setOnPreparedListener(ready->{
                            if(task!=generation)return;
                            loading=false;
                            ready.start();notifyChanged();
                        });
                        mp.setOnCompletionListener(done->stop());
                        mp.setOnErrorListener((broken,what,extra)->{
                            if(task==generation){stopInternal();error="Ses oynatılamadı.";notifyChanged();}
                            return true;
                        });
                        player=mp;mp.prepareAsync();
                    }catch(Exception e){
                        stopInternal();error="Ses kaydı açılamadı.";notifyChanged();
                    }
                });
            }catch(Exception e){
                activity.runOnUiThread(()->{
                    if(task!=generation)return;
                    stopInternal();error="Ses kaydı hazırlanamadı.";notifyChanged();
                });
            }
        },"huzayfi-audio-prepare").start();
    }
    private void stopInternal(){
        generation++;loading=false;surah=0;
        if(player!=null){
            try{player.reset();player.release();}catch(Exception ignored){}
            player=null;
        }
    }
    public void stop(){stopInternal();error="";notifyChanged();}
    public void release(){stopInternal();}
}
