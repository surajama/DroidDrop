package surajama.droiddrop;

import android.app.DownloadManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.util.SimpleArrayMap;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import java.io.File;
import java.io.UnsupportedEncodingException;

/**
 * Created by suraj on 2017-09-11.
 */

public class ReceiverActivity extends ConnectionsActivity {

    private  TextView m_ConnectionStatus;
    private  TextView m_ReceivedFile;
    private Button m_openFolderButton;
    private ProgressBar m_searchProgressBar;
    private ProgressBar m_downloadProgressBar;

    private Payload m_FilePayload;
    private String m_PayloadFilename;
    private File m_PayloadFile;
    private long m_filePayloadId;

    private SimpleArrayMap<Long, Payload> incomingPayloads = new SimpleArrayMap<>();

    @Override
    public void onReceive(Endpoint endpoint, Payload payload) {
        try {
            if (payload.getType() == Payload.Type.BYTES) {
                String payloadFilenameMessage = null;
                payloadFilenameMessage = new String(payload.asBytes(), "UTF-8");
                m_PayloadFilename = payloadFilenameMessage;
                m_ReceivedFile.setVisibility(View.VISIBLE);
                m_ReceivedFile.setText("Receiving file " + payloadFilenameMessage + "...");
                System.out.println(m_PayloadFilename);
                m_downloadProgressBar.setVisibility(View.VISIBLE);

            } else if (payload.getType() == Payload.Type.FILE) {
                m_FilePayload = payload;
                m_filePayloadId = payload.getId();
            }
        }

        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    protected void onPayloadTransferSuccess(String endpointId, PayloadTransferUpdate update) {
        if (update.getPayloadId() == m_filePayloadId) { // check if this is the update for the completed file transfer
            m_ReceivedFile.setText("Received " + m_PayloadFilename);
            m_openFolderButton.setVisibility(View.VISIBLE);
            m_downloadProgressBar.setVisibility(View.INVISIBLE);
            File payloadFile = m_FilePayload.asFile().asJavaFile();
            // Rename the file.
            payloadFile.renameTo(new File(payloadFile.getParentFile(), m_PayloadFilename));
            m_PayloadFile = payloadFile.getAbsoluteFile();
            String filepath = m_PayloadFile.getParentFile() + "/" + m_PayloadFilename;
            File downloadedFile = new File(filepath);
            DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            downloadManager.addCompletedDownload(downloadedFile.getName(), downloadedFile.getName(), true, getMimeType(filepath), downloadedFile.getAbsolutePath(), downloadedFile.length(), true);
            System.out.println(m_PayloadFilename);
            System.out.println(m_PayloadFile.getPath());
        }



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
    protected void onEndpointDisconnected(Endpoint endpoint) {
        super.onEndpointDisconnected(endpoint);
        this.recreate();
    }

    @Override
    protected void onEndpointDiscovered(Endpoint endpoint) {
        // We found an advertiser!
        connectToEndpoint(endpoint);
        m_ConnectionStatus.setText("Connecting to " + endpoint.getName());
    }

    @Override
    protected void onConnectionInitiated(Endpoint endpoint, ConnectionInfo connectionInfo) {
        // A connection to another device has been initiated! We'll accept the connection immediately.
        acceptConnection(endpoint);
        m_ConnectionStatus.setText("Connected to " + endpoint.getName());
        m_ReceivedFile.setText("Waiting for file to be sent...");
        m_ReceivedFile.setVisibility(View.VISIBLE);
        m_searchProgressBar.setVisibility(View.INVISIBLE);

    }
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //  Called once the google API client is ready
        startDiscovering();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive);
        m_searchProgressBar = (ProgressBar) findViewById(R.id.receiving_search_progress);
        m_downloadProgressBar = (ProgressBar) findViewById(R.id.receiving_progress);

        m_ConnectionStatus = (TextView) findViewById(R.id.connection_status);
        m_ReceivedFile = (TextView) findViewById(R.id.received_file);
        m_openFolderButton = (Button) findViewById(R.id.open_folder_button);
        m_openFolderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ReceiverActivity.this.finish();
                startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));

            }
        });
    }

    public static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }

    @Override
    protected void onStop(){
        super.onStop();
        this.finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finish();
    }
}



