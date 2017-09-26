package surajama.droiddrop;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by suraj on 2017-09-08.
 */



public class ConnectionAdapter extends BaseAdapter {

    private SenderActivity m_Context;
    private LayoutInflater m_Inflater;
    private ArrayList<String> m_Connections;

    public ConnectionAdapter(SenderActivity context, ArrayList<String> items) {
        m_Context = context;
        m_Connections = items;
        m_Inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return m_Connections.size();
    }

    @Override
    public Object getItem(int i) {
        return m_Connections.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(final int i, View view, ViewGroup parent) {
        View rowView = m_Inflater.inflate(R.layout.connections_item, parent, false);
        TextView connectionName = (TextView) rowView.findViewById(R.id.connection_name);
        connectionName.setText((String) getItem(i));
        ImageButton cancelConnection = (ImageButton) rowView.findViewById(R.id.connection_cancel_button);
        cancelConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String connectionName = (String) getItem(i);
                m_Context.endConnection(connectionName);
            }
        });
        return rowView;
    }
}
