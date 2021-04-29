package fr.polytech.larynxapp.controller.history;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import fr.polytech.larynxapp.R;
import fr.polytech.larynxapp.model.Record;
import fr.polytech.larynxapp.model.database.DBManager;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    /**
     * The UI list of the data
     */
    private ListView listview;

    /**
     * The list of record datas
     */
    private List<Record> records;

    /**
     * @param inflater Used to load the xml layout file as View
     * @param container A container component
     * @param savedInstanceState Used to save activity
     * @return Return a history's view object
     */
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_history, container, false);      //Sets the view for the fragment
        initMap();

        //********************************Creation of the line chart*******************************/
        final ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        if(!records.isEmpty() ){
            LineDataSet lineDataSet = new LineDataSet(dataValues(records.get(0)), records.get(0).getName());
            setLineData(lineDataSet);
            dataSets.add((lineDataSet));
            final LineData data = new LineData(dataSets);

        }

        //***********************************Creation of the list**********************************/
        listview = root.findViewById(R.id.listViewRecords);
        final ArrayAdapter<Record> adapter = new ArrayAdapter<>(getActivity().getApplicationContext(),
                android.R.layout.simple_list_item_1, records);
        listview.setAdapter(adapter);

        //the delete function
        listview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int position, final long id) {
                AlertDialog.Builder adb=new AlertDialog.Builder(getContext());
                adb.setTitle("Supprimer?");
                adb.setMessage("Etes-vous sûr que vous voulez supprimer " + position);
                adb.setNegativeButton("Cancel", null);
                adb.setPositiveButton("Ok", new AlertDialog.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Record record = records.get(position);
                        records.remove(record);
                        adapter.notifyDataSetChanged();
                        if(new DBManager(getContext()).deleteByName(record.getName())){
                            AlertDialog.Builder adb1=new AlertDialog.Builder(getContext());
                            adb1.setMessage("supprimé avec succès! ");
                            adb1.setNegativeButton("OK", null);
                            adb1.show();
                        }

                    }});
                adb.show();

                listview.invalidate();
                return false;
            }
        });
        return root;
    }

    /**
     * Sets the data set's graphical parameters
     * @param lineDataSet the data set to configure
     */
    private void setLineData(LineDataSet lineDataSet){
        lineDataSet.setCircleColor(Color.BLACK);
        lineDataSet.setCircleRadius(5f);
        lineDataSet.setCircleHoleRadius(2.5f);
        lineDataSet.setValueTextSize(0f);
    }
    /**
     * Sets the data that will be shown in the chart
     * @param recordIn the record that contain shimmer and jitter data
     * @return the array list that will be shown
     */
    private ArrayList<Entry> dataValues(Record recordIn){
        ArrayList<Entry> dataVals = new ArrayList<>();
        dataVals.add(new Entry((float)recordIn.getJitter()*100, (float)recordIn.getShimmer()*100));
        return dataVals;
    }

    /**
     *  Initialisation of the data's map
     */
    private void initMap(){
        records = new DBManager(getContext()).query();
    }


}
