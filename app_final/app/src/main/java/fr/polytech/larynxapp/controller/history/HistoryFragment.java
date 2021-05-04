package fr.polytech.larynxapp.controller.history;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ExpandableListView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import fr.polytech.larynxapp.R;
import fr.polytech.larynxapp.model.Record;
import fr.polytech.larynxapp.model.database.DBManager;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.HashMap;

public class HistoryFragment extends Fragment {
    private SparseBooleanArray mSelectedItemsIds;


    /**
     * The UI list of the data
     */
    private ExpandableListView expandableListView;

    /**
     * The list of record datas
     */
    private ArrayList<Record> listGroup;
    private HashMap<String, ArrayList<String>> listItem;

    /*
    Adapter
     */
    private ExpendableListAdapter adapter;

    /**
     * @param inflater           Used to load the xml layout file as Viewstr
     * @param container          A container component
     * @param savedInstanceState Used to save activity
     * @return Return a history's view object
     */
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_history, container, false);   //Sets the view for the fragment
        expandableListView = root.findViewById(R.id.listViewRecords);
        listGroup = new ArrayList<>();
        listItem = new HashMap<String, ArrayList<String>>();
        mSelectedItemsIds = new  SparseBooleanArray();
        initMap();
        adapter = new ExpendableListAdapter(this.getContext(), listGroup,listItem);
        expandableListView.setAdapter(adapter);





        //***********************************Creation of the list**********************************/


//        final ArrayAdapter<Record> adapter = new ArrayAdapter<>(getActivity().getApplicationContext(),
//                android.R.layout.simple_list_item_multiple_choice, records);

//        expandableListView.setAdapter(adapter);

        expandableListView.setChoiceMode(expandableListView.CHOICE_MODE_MULTIPLE_MODAL);
        expandableListView.setItemsCanFocus(false);
        expandableListView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {


            @Override

            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {}

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mode.getMenuInflater().inflate(R.menu.menu_multi_select_history, menu);
                return true;
            }

            @Override
            public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.selectAll:

                        final int checkedCount = listGroup.size();
                        removeSelection(adapter);

                        for (int i = 0; i < checkedCount; i++) {
                            expandableListView.setItemChecked(i, true);
                        }
                        mode.setTitle(checkedCount + "  Séléctionné");
                        return true;

                    case R.id.delete:
                        // Add  dialog for confirmation to delete selected item
                        // record.
                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        builder.setMessage("Voulez-vous supprimer le ou les enregistrements sélectionnés ?");
                        builder.setNegativeButton("Non", new AlertDialog.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });

                        builder.setPositiveButton("Oui", new AlertDialog.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                SparseBooleanArray selected = mSelectedItemsIds;
                                for (int i = (selected.size() - 1); i >= 0; i--) {
                                    if (selected.valueAt(i)) {
                                        Record selecteditem = adapter.getGroup(selected.keyAt(i));
                                        // Remove  selected items following the ids
                                        listItem.remove(selecteditem.getName());
                                        listGroup.remove(selecteditem);
                                        adapter.remove(selecteditem);
                                        new DBManager(getContext()).deleteByName(selecteditem.getName());
                                    }
                                }
                                // Close CAB
                                mode.finish();
                                selected.clear();
                            }

                        });

                        AlertDialog alert = builder.create();
                        alert.setTitle("Confirmation"); // dialog  Title
                        alert.show();

                        return true;

                    default:
                        return false;

                }
            }

            @Override

            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {

                final int checkedCount = expandableListView.getCheckedItemCount();
                // Set the  CAB title according to total checked items
                mode.setTitle(checkedCount + "  Séléctionné");
                // Calls  toggleSelection method from ListViewAdapter Class
                toggleSelection(position,adapter);
            }
        });
        return root;
    }

    /**
     * Sets the data set's graphical parameters
     *
     * @param lineDataSet the data set to configure
     */
    private void setLineData(LineDataSet lineDataSet) {
        lineDataSet.setCircleColor(Color.BLACK);
        lineDataSet.setCircleRadius(5f);
        lineDataSet.setCircleHoleRadius(2.5f);
        lineDataSet.setValueTextSize(0f);
    }

    /**
     * Sets the data that will be shown in the chart
     *
     * @param recordIn the record that contain shimmer and jitter data
     * @return the array list that will be shown
     */
    private ArrayList<Entry> dataValues(Record recordIn) {
        ArrayList<Entry> dataVals = new ArrayList<>();
        dataVals.add(new Entry((float) recordIn.getJitter() * 100, (float) recordIn.getShimmer() * 100));
        return dataVals;
    }

    /**
     * Initialisation of the data's map
     */
    private void initMap() {
        listGroup = new DBManager(getContext()).query();
        for(Record record : listGroup){
            ArrayList<String> items = new ArrayList<>();
            items.add("Jitter : "+Double.toString(record.getJitter()));
            items.add("Shimmer : "+Double.toString(record.getShimmer()));
            listItem.put(record.getName(),items);
        }

    }

    public void removeSelection(ExpendableListAdapter adapter) {

        mSelectedItemsIds = new SparseBooleanArray();

        adapter.notifyDataSetChanged();

    }

    // Item checked on selection
    public void selectView(int position, boolean value,ExpendableListAdapter adapter ) {
        if (value)
            mSelectedItemsIds.put(position, value);
        else
            mSelectedItemsIds.delete(position);

        adapter.notifyDataSetChanged();
    }

    public void  toggleSelection(int position,ExpendableListAdapter adapter) {
        selectView(position, !mSelectedItemsIds.get(position),adapter);
    }

}
