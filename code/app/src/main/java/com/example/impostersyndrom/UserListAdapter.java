package com.example.impostersyndrom;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.List;

public class UserListAdapter extends ArrayAdapter<String> {
    public UserListAdapter(Context context, List<String> users) {
        super(context, 0, users);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.user_list_item, parent, false);
        }

        String username = getItem(position);
        TextView userNameTextView = convertView.findViewById(R.id.usernameTextView);
        userNameTextView.setText(username);

        return convertView;
    }
}
