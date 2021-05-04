package fr.polytech.larynxapp.controller.history;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import fr.polytech.larynxapp.model.Record;

public class ExpendableListAdapter extends BaseExpandableListAdapter {
    Context context;
    ArrayList<Record> listGroup;
    HashMap<String, ArrayList<String>> listItem;

    public ExpendableListAdapter(Context context, ArrayList<Record> listGroup, HashMap<String, ArrayList<String>> listItem) {
        this.context = context;
        this.listGroup = listGroup;
        this.listItem = listItem;
    }

    @Override
    public int getGroupCount() {
        return listGroup.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return this.listItem.get(listGroup.get(groupPosition).getName()).size();
    }

    @Override
    public Record getGroup(int groupPosition) {
        return this.listGroup.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return this.listItem.get(this.listGroup.get(groupPosition).getName()).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        convertView=LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_multiple_choice,parent,false);
        TextView textView = convertView.findViewById(android.R.id.text1);
        String sGroup= String.valueOf(getGroup(groupPosition));
        textView.setText(sGroup);
        textView.setTypeface(null, Typeface.BOLD);
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        convertView=LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1,parent,false);
        TextView textView = convertView.findViewById(android.R.id.text1);
        String sChild= String.valueOf(getChild(groupPosition,childPosition));
        textView.setText(sChild);
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public void remove(Record record){
        listItem.remove(record.getName());
        listGroup.remove(record);
        notifyDataSetChanged();
    }

}
