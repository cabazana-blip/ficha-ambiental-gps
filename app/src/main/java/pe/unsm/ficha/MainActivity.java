package pe.unsm.ficha;
import android.app.Activity;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.content.Context;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.net.Uri;
import android.content.Intent;
import android.util.Base64;
import android.widget.Toast;
import android.print.PrintManager;
import android.provider.Settings;
import java.io.OutputStream;
import org.json.JSONObject;
public class MainActivity extends Activity {
 WebView web; LocationManager lm; LocationListener listener; ValueCallback<Uri[]> photos; byte[] file;
 static final int GPS=10,PHOTO=11,SAVE=12;
 public void onCreate(Bundle b){super.onCreate(b);web=new WebView(this);setContentView(web);web.getSettings().setJavaScriptEnabled(true);web.getSettings().setDomStorageEnabled(true);web.getSettings().setAllowFileAccess(true);web.addJavascriptInterface(new Bridge(),"AndroidBridge");web.setWebViewClient(new WebViewClient());web.setWebChromeClient(new WebChromeClient(){@Override public boolean onShowFileChooser(WebView w,ValueCallback<Uri[]> c,FileChooserParams p){photos=c;Intent i=new Intent(Intent.ACTION_GET_CONTENT);i.setType("image/*");startActivityForResult(i,PHOTO);return true;}});web.loadUrl("file:///android_asset/index.html");lm=(LocationManager)getSystemService(LOCATION_SERVICE);}
 void js(String s){runOnUiThread(()->web.evaluateJavascript(s,null));}
 void gps(){if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},GPS);return;}if(!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)&&!lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));return;}stop();listener=new LocationListener(){public void onLocationChanged(Location l){js("onGPS("+l.getLatitude()+","+l.getLongitude()+","+l.getAccuracy()+","+l.getTime()+","+JSONObject.quote(l.getProvider())+")");}};try{if(lm.isProviderEnabled(LocationManager.GPS_PROVIDER))lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,1500,0,listener);if(lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER))lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,2000,0,listener);}catch(SecurityException e){}}
 void stop(){if(listener!=null){lm.removeUpdates(listener);listener=null;}}
 @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==GPS&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)gps();}
 @Override protected void onPause(){stop();super.onPause();}
 @Override protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(r==PHOTO&&photos!=null){photos.onReceiveValue(c==RESULT_OK&&d!=null?new Uri[]{d.getData()}:null);photos=null;}if(r==SAVE&&c==RESULT_OK&&d!=null&&file!=null){try(OutputStream o=getContentResolver().openOutputStream(d.getData())){o.write(file);Toast.makeText(this,"Archivo guardado",Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(this,"Error al guardar",Toast.LENGTH_SHORT).show();}file=null;}}
 public class Bridge {
 @JavascriptInterface public void startGPS(){runOnUiThread(()->gps());}
 @JavascriptInterface public void stopGPS(){runOnUiThread(()->stop());}
 @JavascriptInterface public void saveFile(String name,String data,String mime){runOnUiThread(()->{file=Base64.decode(data,Base64.DEFAULT);Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType(mime);i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_TITLE,name);startActivityForResult(i,SAVE);});}
 @JavascriptInterface public void printReport(String html){runOnUiThread(()->{WebView p=new WebView(MainActivity.this);p.setWebViewClient(new WebViewClient(){@Override public void onPageFinished(WebView w,String url){((PrintManager)getSystemService(Context.PRINT_SERVICE)).print("Ficha Ambiental",w.createPrintDocumentAdapter("Ficha Ambiental"),null);}});p.loadDataWithBaseURL(null,html,"text/html","UTF-8",null);});}
 }
}