package surajama.droiddrop;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Map;

public class SenderActivity extends ConnectionsActivity{

    private ListView m_ConnectionsListView;
    private Button m_ChangeFileNameButton;
    private Button m_sendFileButton;
    private TextView m_sendErrorText;
    private TextView m_connectionStatus;
    private ProgressBar m_progressBar;
    private ProgressBar m_sendProgressBar;

    private File m_fileToSend;
    private Payload m_filePayload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);
        Intent intent = getIntent();
        final Uri m_uriPathToSend = Uri.parse(intent.getStringExtra("FileUri"));
        System.out.println(m_uriPathToSend);
        m_progressBar = (ProgressBar) findViewById(R.id.send_search_progress);
        m_sendProgressBar = (ProgressBar) findViewById(R.id.send_progress);
        m_sendErrorText = (TextView) findViewById(R.id.send_error_message);
        m_connectionStatus = (TextView) findViewById(R.id.connection_status_send);
        m_ConnectionsListView = (ListView) findViewById(R.id.connections_list);
        m_ChangeFileNameButton = (Button) findViewById(R.id.change_file_button);
        m_sendFileButton = (Button) findViewById(R.id.send_file_button);
        m_sendFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (mEstablishedConnections.size() == 0){
                        m_sendErrorText.setText("No users connected, cannot send your file");
                        return;
                    }
                    m_sendFileButton.setText("Sending...");
                    m_sendFileButton.setClickable(false);
                    m_sendProgressBar.setVisibility(View.VISIBLE);
                    // Open the ParcelFileDescriptor for this URI with read access.
                    ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(m_uriPathToSend, "r");
                    Payload filePayload = Payload.fromFile(pfd);
                    m_filePayload = filePayload;
                    // Construct a simple message mapping the ID of the file payload to the desired filename.
                    String fileName = getFileName(m_uriPathToSend);
                    //String filenameMessage = filePayload.getId() + ":" + m_uriPathToSend.getLastPathSegment();
                    Payload filenamePayload = Payload.fromBytes(fileName.getBytes("UTF-8"));

                    // First send filename payload
                    send(filenamePayload);
                    // Next send actual file payload
                    send(filePayload);
                } catch (FileNotFoundException e) {
                    logE("Sending the file failed", e);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }});

    }
    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    @Override
    protected void onPayloadTransferSuccess(String endpointId, PayloadTransferUpdate update) {
        if (update.getPayloadId() == m_filePayload.getId()){
            // then file has been successfully receieved on the other end
            m_sendProgressBar.setVisibility(View.INVISIBLE);
            m_sendProgressBar.setProgress(m_sendProgressBar.getMax());
            m_sendFileButton.setText("File sent");
        }

    }


    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onConnectionInitiated(Endpoint endpoint, ConnectionInfo connectionInfo){
        m_connectionStatus.setText("Connected Devices:");
        acceptConnection(endpoint);
        m_sendFileButton.setVisibility(View.VISIBLE);
        m_progressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onEndpointDiscovered(Endpoint endpoint)
    {
        super.onEndpointDiscovered(endpoint);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        startAdvertising();
    }

    @Override
    protected void onConnectionFailed(Endpoint endpoint) {
        super.onConnectionFailed(endpoint);
    }

    @Override
    protected void onEndpointConnected(Endpoint endpoint) {
        super.onEndpointConnected(endpoint);
        ArrayList<String> connections = new ArrayList<String>(mConnectionNames.values());
        m_ConnectionsListView.setAdapter(new ConnectionAdapter(this, connections));
        m_sendErrorText.setText("");
    }

    @Override
    protected void onEndpointDisconnected(Endpoint endpoint) {
        super.onEndpointDisconnected(endpoint);
        ArrayList<String> connections = new ArrayList<String>(mConnectionNames.values());
        m_ConnectionsListView.setAdapter(new ConnectionAdapter(this, connections));
    }

    public void endConnection(String name){
        String deviceID = null;
        for (Map.Entry<String, String> entry : mConnectionNames.entrySet()){
            if (entry.getValue() == name){
                deviceID = entry.getKey();
                break;
            }
        }
        if (deviceID != null){
            disconnect(mEstablishedConnections.get(deviceID));
        }
        ArrayList<String> connections = new ArrayList<String>(mConnectionNames.values());
        m_ConnectionsListView.setAdapter(new ConnectionAdapter(this, connections));
    }

    @Override
    protected String getName() {
        return Constants.DEVICE_NAME;
    }

    @Override
    protected String getServiceId() {
        return Constants.SERVICE_ID;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finish();
    }

}
