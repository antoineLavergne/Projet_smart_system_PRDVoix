package fr.polytech.larynxapp.controller.Shimmer;

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
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ShimmerFragment extends Fragment {

    /**
     * The line chart where the shimmer values will be shown
     */
    private LineChart shimmerMpLineChart;


    /**
     * The list of record datas
     */
    private List<Record> records;

    /**
     * The startDate Button
     */
    private ImageButton startDateButton;


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
     * to store the data
     */
    private String[] dateValues;

    /**
     * the reset Button
     */
    private ImageButton resetButton;

    /**
     * @param inflater Used to load the xml layout file as View
     * @param container A container component
     * @param savedInstanceState Used to save activity
     * @return Return a evolution'sview object
     */
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        //Sets the view the the fragment
        View root = inflater.inflate(R.layout.fragment_shimmer, container, false);

        //Read data from the database
        records = new DBManager(getContext()).query();

        //Initialization module
        startDateButton = root.findViewById(R.id.startDate);
        resetButton = root.findViewById(R.id.resetDate);
        dateValues = dateValues();
        initDateButton();

        //******************************Creation of the shimmer's chart*****************************/
        final TextView shimmerTextView = root.findViewById(R.id.shimmer_text_view);
        shimmerTextView.setText("Shimmer");
        shimmerTextView.setTextSize(20f);
        shimmerMpLineChart = root.findViewById(R.id.shimmer_line_chart);
        setShimmerChart(shimmerMpLineChart);
        setShimmerChartData();
        return root;
    }

    /**
     * Set the data into the chart.
     */
    public void setShimmerChartData(){
        LineDataSet shimmerLineSet = new LineDataSet(shimmerDataValues(), "Shimmer");
        shimmerLineSet.setColor(Color.BLACK);
        shimmerLineSet.setLineWidth(2f);
        shimmerLineSet.setCircleColor(Color.BLACK);
        shimmerLineSet.setCircleRadius(5f);
        shimmerLineSet.setCircleHoleRadius(2.5f);
        shimmerLineSet.setValueTextSize(0f);
        ArrayList<ILineDataSet> shimmerDataSets = new ArrayList<>();
        shimmerDataSets.add((shimmerLineSet));

        LimitLine shimmerLl = new LimitLine(0.35f);
        shimmerLl.setLabel("Limite shimmer");
        shimmerLl.setLineColor(Color.RED);
        shimmerMpLineChart.getAxisLeft().addLimitLine(shimmerLl);

        XAxis shimmerXAxis = shimmerMpLineChart.getXAxis();
        shimmerXAxis.setGranularity(1f);
        shimmerXAxis.setSpaceMax(0.1f);
        shimmerXAxis.setSpaceMin(0.1f);
        shimmerXAxis.setValueFormatter(new com.github.mikephil.charting.formatter.IndexAxisValueFormatter(dateValues()));


        LineData shimmerData = new LineData(shimmerDataSets);
        shimmerMpLineChart.setData(shimmerData);
        shimmerMpLineChart.invalidate();
        shimmerMpLineChart.setDrawGridBackground(false);
    }

    /**
     * Initialisation of the shimmer data's arraylist
     * @return the shimmer data's arraylist
     */
    private ArrayList<Entry> shimmerDataValues(){
        ArrayList<Entry> dataVals = new ArrayList<>();
        for(int i = 0; i < records.size(); i++) {
            dataVals.add(new Entry(i, (float) records.get(i).getShimmer()));
        }
        return dataVals;
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
     * Set the graphic feature of the line charts for the shimmer
     * @param chart the chart to be set
     */
    private void setShimmerChart(LineChart chart){

        YAxis yAxis = chart.getAxisLeft();                  //The line chart's y axis
        XAxis xAxis = chart.getXAxis();                     //The line chart's x axis
        chart.getAxisLeft().setEnabled(true);

        chart.getAxisRight().setEnabled(false);             //Disable the right axis

        //Set the y axis property
        yAxis.setAxisLineWidth(0.2f);
        yAxis.setAxisLineColor(Color.BLACK);
        yAxis.setAxisMinimum(0f);
        yAxis.setAxisMaximum(1f);
        yAxis.setTextSize(12f);

        //Set the x axis property
        xAxis.setAxisLineWidth(1f);
        xAxis.setAxisLineColor(Color.BLACK);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(12f);

        chart.getLegend().setEnabled(false);
        chart.getDescription().setEnabled(false);
        chart.setScaleEnabled(true);
        chart.setTouchEnabled(false);
    }

    /**
     * Button initialization
     */
    public void initDateButton() {
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
                                setStartDateYear(year);
                                setStartDateMonth(month+1);
                                setStartDateDay(day);
                                String dateDay = String.valueOf(day);
                                String dateMonth = String.valueOf(month+1);
                                if(day < 10){
                                    dateDay = "0" + dateDay;
                                }
                                if(month < 10){
                                    dateMonth = "0" + dateMonth;
                                }

                                //Process the file name
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

        //Add an event to the button
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setShimmerChartData();
            }
        });

    }

    /**
     * Set year
     * @param startDateYear The year you want to set.
     */
    public void setStartDateYear(int startDateYear) {
        this.startDateYear = startDateYear;
    }

    /**
     * Set month
     * @param startDateMonth The month you want to set.
     */
    public void setStartDateMonth(int startDateMonth) {
        this.startDateMonth = startDateMonth;
    }

    /**
     * Set day
     * @param startDateDay The day you want to set
     */
    public void setStartDateDay(int startDateDay) {
        this.startDateDay = startDateDay;
    }

}

