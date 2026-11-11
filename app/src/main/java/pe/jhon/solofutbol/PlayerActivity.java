package pe.jhon.solofutbol;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;

/**
 * Reproductor a pantalla completa. Recibe varias señales del mismo canal y,
 * si una falla, pasa a la siguiente. Si ninguna funciona, cierra y avisa.
 */
public class PlayerActivity extends Activity {

    static final int SIN_SENAL = RESULT_FIRST_USER;

    private ExoPlayer reproductor;
    private PlayerView vista;
    private String[] urls;
    private int indice = 0;
    private boolean funciono = false;
    private int reintentos = 0;

    @Override
    protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        urls = getIntent().getStringArrayExtra("urls");
        if (urls == null || urls.length == 0) {
            finish();
            return;
        }

        vista = new PlayerView(this);
        vista.setBackgroundColor(Color.BLACK);
        setContentView(vista);

        DefaultHttpDataSource.Factory red = new DefaultHttpDataSource.Factory()
                .setUserAgent(MainActivity.NAVEGADOR)
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(12000)
                .setReadTimeoutMs(12000);

        reproductor = new ExoPlayer.Builder(this)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(this).setDataSourceFactory(red))
                .build();
        vista.setPlayer(reproductor);

        reproductor.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int estadoActual) {
                if (estadoActual == Player.STATE_READY) {
                    funciono = true;
                    reintentos = 0;
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                alFallar();
            }
        });

        cargar();
    }

    private void cargar() {
        String url = urls[indice];
        String sinParametros = url.split("\\?")[0].toLowerCase();
        MediaItem.Builder item = new MediaItem.Builder().setUri(url);
        // Casi todas las señales en vivo son HLS aunque la dirección no lo diga.
        if (!sinParametros.endsWith(".mp4") && !sinParametros.endsWith(".mpd")) {
            item.setMimeType(MimeTypes.APPLICATION_M3U8);
        }
        reproductor.setMediaItem(item.build());
        reproductor.prepare();
        reproductor.setPlayWhenReady(true);
    }

    private void alFallar() {
        // Un corte a mitad de transmisión: se reintenta la misma señal un par de veces.
        if (funciono && reintentos < 2) {
            reintentos++;
            cargar();
            return;
        }
        funciono = false;
        reintentos = 0;
        indice++;
        if (indice < urls.length) {
            cargar();
        } else {
            setResult(SIN_SENAL);
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean conFoco) {
        super.onWindowFocusChanged(conFoco);
        if (conFoco) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (reproductor != null) reproductor.setPlayWhenReady(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (reproductor != null) reproductor.setPlayWhenReady(true);
    }

    @Override
    protected void onDestroy() {
        if (reproductor != null) {
            reproductor.release();
            reproductor = null;
        }
        super.onDestroy();
    }
}
