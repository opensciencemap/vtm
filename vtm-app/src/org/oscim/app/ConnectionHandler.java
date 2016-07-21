package org.oscim.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

public class ConnectionHandler extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        //        NetworkInfo mobNetInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE );
        if (activeNetInfo != null) {
            if (activeNetInfo.isConnected()) {
                Toast.makeText(context, "Active Network Type : " + activeNetInfo.getTypeName(),
                        Toast.LENGTH_SHORT).show();
                //if (App.map != null)
                //    App.map.redrawMap();
            }
            //Toast.makeText( context, "Active Network Type : " + activeNetInfo.getTypeName(), Toast.LENGTH_SHORT ).show();
        }
        //        if( mobNetInfo != null )
        //        {
        //          Toast.makeText( context, "Mobile Network Type : " + mobNetInfo.getTypeName(), Toast.LENGTH_SHORT ).show();
        //        }
    }
}
