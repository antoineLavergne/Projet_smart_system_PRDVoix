package fr.polytech.larynxapp.controller.jitter;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import fr.polytech.larynxapp.R;
import fr.polytech.larynxapp.model.Record;
import fr.polytech.larynxapp.model.database.DBManager;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class JitterFragment extends Fragment {

    /**
     * The line chart where the jitter values will be shown
     */
    private LineChart jitterMpLineChart;

    /**
     * The list of record datas
     */
    private List<Record> records;

    /**
     * The startDate Button
     */
    private ImageButton startDateButton;


    private ImageButton resetButton;

    /**
     * The startDate
     */
    private int startDateDay;
    private int startDateMonth;
    private int startDateYear;

    /**
     * The endDate
     */
    private int endDateDay;
    private int endDateMonth;
    private int endDateYear;

    /**
     * The datePicker
     */
    private DatePicker datePicker;

    /**
     * @param inflater Used to load the xml layout file as View
     * @param container A container component
     * @param savedInstanceState Used to save activity
     * @return Return a Jitter's view object
     */
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_jitter,container, false);    //Sets the view the the fragment


        records = new DBManager(getContext()).query();

        startDateButton = root.findViewById(R.id.startDate);
        resetButton = root.findViewById(R.id.resetDate);
        initDateButton();
        //******************************Creation of the jitter's chart******************************/
        final TextView jitterTextView = root.findViewById(R.id.jitter_text_view);
        jitterTextView.setText("Jitter");
        jitterTextView.setTextSize(20f);

        jitterMpLineChart = root.findViewById(R.id.jitter_line_chart);
        setJitterChart(jitterMpLineChart);
        setJitterChartData();
        return root;
    }

    /**
     * set the jitter's data to the chart
     */
    public void setJitterChartData(){
        LineDataSet jitterLineSet = new LineDataSet(jitterDataValues(), "Jitter");
        jitterLineSet.setColor(Color.BLACK);
        jitterLineSet.setLineWidth(2f);
        jitterLineSet.setCircleColor(Color.BLACK);
        jitterLineSet.setCircleRadius(5f);
        jitterLineSet.setCircleHoleRadius(2.5f);
        jitterLineSet.setValueTextSize(0f);
        ArrayList<ILineDataSet> jitterDataSets = new ArrayList<>();
        jitterDataSets.add((jitterLineSet));

        LimitLine jitterLl = new LimitLine(2.04f);
        jitterLl.setLabel("Limite jitter");
        jitterLl.setLineColor(Color.RED);
        jitterMpLineChart.getAxisLeft().addLimitLine(jitterLl);

        XAxis jitterXAxis = jitterMpLineChart.getXAxis();
        jitterXAxis.setGranularity(1f);
        jitterXAxis.setSpaceMax(0.1f);
        jitterXAxis.setSpaceMin(0.1f);
        jitterXAxis.setValueFormatter(new com.github.mikephil.charting.formatter.IndexAxisValueFormatter(dateValues()));

        LineData jitterData = new LineData(jitterDataSets);
        jitterMpLineChart.setData(jitterData);
        jitterMpLineChart.invalidate();
        jitterMpLineChart.setDrawGridBackground(false);
    }

    /**
     * Initialisation of the jitter data's arraylist
     * @return the jitter data's arraylist
     */
    private ArrayList<Entry> jitterDataValues(){
        ArrayList<Entry> dataVals = new ArrayList<>();
        for(int i = 0; i < records.size(); i++) {
            dataVals.add(new Entry(i, (float) records.get(i).getJitter()));
        }
        return dataVals;
    }

    /**
     * Initialisation of the dates arraylist
     * @return the dates arraylist
     */
    private String[] dateValues(){
        ArrayList<String> dates = new ArrayList<>();
        for(int i = 0; i < records.size(); i++)
        {
            String strippedName = records.get(i).getName().replace("-", " ");
            String[] dateTimes = strippedName.split(" ");
            dates.add(i ,dateTimes[0] + "-" + dateTimes[1] + "-" + dateTimes[2]);
        }
        return dates.toArray(new String[0]);
    }


    /**
     * Set the graphic feature of the line charts for the jitter
     * @param chart the chart to be set
     */
    private void setJitterChart(LineChart chart){

        YAxis yAxis = chart.getAxisLeft();                  //The line chart's y axis
        XAxis xAxis = chart.getXAxis();                     //The line chart's x axis

        chart.getAxisRight().setEnabled(false);             //Disable the right axis

        //Set the y axis property
        yAxis.setAxisLineWidth(2f);
        yAxis.setAxisLineColor(Color.BLACK);
        yAxis.setAxisMinimum(0f);
        yAxis.setAxisMaximum(3f);
        yAxis.setTextSize(12f);
        PercentFormatter percentFormatter = new PercentFormatter();
        yAxis.setValueFormatter(percentFormatter);

        //Set the x axis property
        xAxis.setAxisLineWidth(2f);
        xAxis.setAxisLineColor(Color.BLACK);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(12f);

        chart.getLegend().setEnabled(false);
        chart.getDescription().setEnabled(false);

        chart.setScaleEnabled(true);
        chart.setTouchEnabled(false);
    }

    /**
     * init the select date buttons
     */
    private void initDateButton() {
        startDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                records = new DBManager(getContext()).query();
                Calendar calendar = Calendar.getInstance();
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);
                int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
                DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                                startDateYear = year;
                                startDateMonth = month + 1;
                                startDateDay = day;

                                String dateDay = String.valueOf(day);
                                String dateMonth = String.valueOf(month+1);
                                if(day < 10){
                                    dateDay = "0" + dateDay;
                                }
                                if(month < 10){
                                    dateMonth = "0" + dateMonth;
                                }

                                String test = dateDay +"-" + dateMonth + "-" + year;

                                for(int i = 0; i < records.size(); i++){
                                    String tmp = records.get(i).getName().split(" ")[0];
                                    if(!test.equals(tmp)){
                                        records.remove(i);
                                        i--;
                                    }
                                }
                            }
                        }, year, month, dayOfMonth);
                datePickerDialog.show();
            }
        });

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setJitterChartData();
            }
        });

    }
}

