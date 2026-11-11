package pe.jhon.solofutbol;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * La guía de canales es una página (assets/index.html) dentro de un WebView.
 * Esta clase le da lo que la página sola no puede hacer: abrir el reproductor
 * nativo y comprobar si la señal de un canal responde.
 */
public class MainActivity extends Activity {

    static final String NAVEGADOR = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36";
    private static final int PEDIR_VER = 5201;

    private WebView web;
    private String canalActual;
    private final ExecutorService hilos = Executors.newFixedThreadPool(6);

    @Override
    protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        web = new WebView(this);
        web.setBackgroundColor(0xFF0B1410);
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setTextZoom(100);
        web.setWebViewClient(new WebViewClient());
        web.addJavascriptInterface(new Puente(), "Android");
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
    }

    /** Lo que la página puede pedirle al celular (window.Android). */
    private class Puente {

        /** Abre el reproductor con las señales del canal, en el orden en que se deben probar. */
        @JavascriptInterface
        public void reproducir(final String id, final String nombre, final String urlsJson) {
            runOnUiThread(() -> {
                try {
                    JSONArray lista = new JSONArray(urlsJson);
                    String[] urls = new String[lista.length()];
                    for (int i = 0; i < urls.length; i++) urls[i] = lista.getString(i);
                    if (urls.length == 0) return;
                    canalActual = id;
                    Intent i = new Intent(MainActivity.this, PlayerActivity.class);
                    i.putExtra("nombre", nombre);
                    i.putExtra("urls", urls);
                    startActivityForResult(i, PEDIR_VER);
                } catch (Exception e) {
                    // Datos mal formados: no se abre nada.
                }
            });
        }

        /** Comprueba en segundo plano si una señal responde y avisa a la página. */
        @JavascriptInterface
        public void probar(final String id, final String url) {
            hilos.execute(() -> {
                final boolean ok = responde(url);
                runOnUiThread(() -> web.evaluateJavascript("window.alProbar&&window.alProbar("
                        + JSONObject.quote(id) + "," + JSONObject.quote(url) + "," + ok + ")", null));
            });
        }
    }

    /** True si la dirección entrega una lista de reproducción HLS válida. */
    static boolean responde(String direccion) {
        HttpURLConnection c = null;
        try {
            URL u = new URL(direccion);
            for (int salto = 0; salto < 6; salto++) {
                c = (HttpURLConnection) u.openConnection();
                c.setConnectTimeout(8000);
                c.setReadTimeout(8000);
                c.setInstanceFollowRedirects(false);
                c.setRequestProperty("User-Agent", NAVEGADOR);
                int codigo = c.getResponseCode();
                if (codigo >= 300 && codigo < 400) {
                    String destino = c.getHeaderField("Location");
                    c.disconnect();
                    if (destino == null) return false;
                    u = new URL(u, destino);
                    continue;
                }
                if (codigo != 200) return false;
                try (InputStream in = c.getInputStream()) {
                    byte[] b = new byte[2048];
                    int n = 0, leidos;
                    while (n < b.length && (leidos = in.read(b, n, b.length - n)) > 0) n += leidos;
                    return new String(b, 0, n, StandardCharsets.UTF_8).contains("#EXTM3U");
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    @Override
    protected void onActivityResult(int pedido, int resultado, Intent datos) {
        super.onActivityResult(pedido, resultado, datos);
        if (pedido == PEDIR_VER && resultado == PlayerActivity.SIN_SENAL && canalActual != null) {
            web.evaluateJavascript("window.alFallar&&window.alFallar("
                    + JSONObject.quote(canalActual) + ")", null);
        }
    }

    @Override
    protected void onDestroy() {
        hilos.shutdownNow();
        super.onDestroy();
    }
}
