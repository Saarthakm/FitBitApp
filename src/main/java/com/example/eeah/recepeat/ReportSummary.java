package com.example.eeah.recepeat;


import android.app.Activity;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Goal;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.request.GoalsReadRequest;
import com.google.android.gms.fitness.result.DailyTotalResult;
import com.google.android.gms.fitness.result.DataReadResult;
import com.google.android.gms.fitness.result.GoalsResult;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class ReportSummary extends Fragment implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
//    public InterfaceCalorieCommunicator interfaceCalorieCommunicator;
    public static GoogleApiClient mClient = null;
    //Charts
    private LineChart chart_1;
    private PieChart chart_2;
    private PieChart chart_3;
    //Chart entries
    private List<Entry> entries = new ArrayList<Entry>();
    private List<PieEntry> pie_stepEntries = new ArrayList<>();
    private List<PieEntry> pie_calorieEntries = new ArrayList<>();

    private boolean isWeek = false;
    private boolean isDay = false;
    private boolean isCalorie = false;
    private int goalSteps, goalCalorie;
    private long  calorieTotal, stepTotal;
    List<Goal> goals;
    private int count = 0;
    private ArrayList<Integer> dateList = new ArrayList<Integer>();

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
//        interfaceCalorieCommunicator.getCalories();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_report_summary, container, false);

        //To connect to Google's Fit API
        mClient = new GoogleApiClient.Builder(this.getContext())
                .addApi(Fitness.HISTORY_API)
                .addApi(Fitness.GOALS_API)
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ))
                .addScope(new Scope(Scopes.FITNESS_LOCATION_READ))
                .addConnectionCallbacks(this)
                .enableAutoManage(this.getActivity(), 0, this)
                .build();

        // LineChart to show data.
        chart_1 = (LineChart) view.findViewById(R.id.chart);
        chart_2 = (PieChart) view.findViewById(R.id.chart_pie);
        chart_3 = (PieChart) view.findViewById(R.id.chart_pie_2);

        new ViewWeekStepCountTask().execute();
        return view;
    }

    public void onConnected(@Nullable Bundle bundle) {
        Log.e("HistoryAPI", "onConnected");
    }

    //In use, call this every 30 seconds in active mode, 60 in ambient on watch faces
    private void displayStepDataForToday() {
        DailyTotalResult result = Fitness.HistoryApi.readDailyTotal( mClient, DataType.TYPE_STEP_COUNT_DELTA ).await(1, TimeUnit.MINUTES);
        isDay = true;
        isWeek = false;
        showDataSet(result.getTotal());

        //To get calorie data.
        new ViewTodaysCalorie().execute();
    }

    //Background Methods for Google fit API.
    private void displayLastWeeksData() {
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        long endTime = cal.getTimeInMillis();
        cal.add(Calendar.DAY_OF_YEAR, -7);
        long startTime = cal.getTimeInMillis();

        java.text.DateFormat dateFormat = DateFormat.getDateInstance();
        Log.e("History", "Range Start: " + dateFormat.format(startTime));
        Log.e("History", "Range End: " + dateFormat.format(endTime));

        DataSource ESTIMATED_STEP_DELTAS = new DataSource.Builder()
                .setAppPackageName("com.google.android.gms")
                .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                .setType(DataSource.TYPE_DERIVED)
                .setStreamName("estimated_steps")
                .build();

        //Check how many steps were walked and recorded in the last 7 days
        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(ESTIMATED_STEP_DELTAS, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();

        DataReadResult dataReadResult = Fitness.HistoryApi.readData(mClient, readRequest).await(1, TimeUnit.MINUTES);

        //Used for aggregated data
        if (dataReadResult.getBuckets().size() > 0) {
            Log.e("History", "Number of buckets: " + dataReadResult.getBuckets().size());
            for (Bucket bucket : dataReadResult.getBuckets()) {
                List<DataSet> dataSets = bucket.getDataSets();
                isWeek = true;
                for (DataSet dataSet : dataSets) {
                    showDataSet(dataSet);
                }
            }

        }
        //Used for non-aggregated data
        else if (dataReadResult.getDataSets().size() > 0) {
                Log.e("History", "Number of returned DataSets: " + dataReadResult.getDataSets().size());
                isWeek = true;
                for (DataSet dataSet : dataReadResult.getDataSets()) {
                    showDataSet(dataSet);
                }
            }
        //To show today's step data.
        new ViewTodaysStepCountTask().execute();
    }

    private void showDataSet(DataSet dataSet) {
        String str = "";
        str+="History" + "Data returned for Data type: " + dataSet.getDataType().getName();
        DateFormat dateFormat = DateFormat.getDateInstance();
        DateFormat timeFormat = DateFormat.getTimeInstance();

        for (DataPoint dp : dataSet.getDataPoints()) {
            str+= " History Data point:\n";
            str+= "History \tType: " + dp.getDataType().getName() + "\n";
            str+= "History \tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)) + " " + timeFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS))+"\n";
            str+= "History \tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)) + " " + timeFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS))+"\n";
            for(Field field : dp.getDataType().getFields()) {
               if(isWeek) {
                   entries.add(new Entry(count++, (float) dp.getValue(field).asInt()));
                   dateList.add(dateFormat.getCalendar().get(Calendar.DAY_OF_MONTH));
               }
               if(isDay)
                  stepTotal =  dp.getValue(field).asInt();


                Log.e("History", entries + " " + pie_stepEntries);
            }
        }
        if(isWeek && entries.size()==6)
            makeLineChart(chart_1);
        else if(isDay)
            makeStepPieChart(chart_2);

    }


    /////Callback///////

    public long calorieExecute(){
        PendingResult<DailyTotalResult> result = Fitness.HistoryApi.readDailyTotal(mClient, DataType. TYPE_CALORIES_EXPENDED);
        DailyTotalResult totalResult = result.await(30, TimeUnit.SECONDS);
        if (totalResult.getStatus().isSuccess()) {
            DataSet totalSet = totalResult.getTotal();
            if (totalSet != null) {
                calorieTotal = totalSet.isEmpty()
                        ? 0
                        : (int)totalSet.getDataPoints().get(0).getValue(Field.FIELD_CALORIES).asFloat();
            }
        } else {
            Log.w("", "There was a problem getting the calories.");
        }
        return calorieTotal;
    }

//    @Override
//    public void onAttach(Activity activity){
//        super.onAttach(activity);
//        try{
//            interfaceCalorieCommunicator = (InterfaceCalorieCommunicator) activity;
//        }catch (ClassCastException e){
//            throw new ClassCastException(activity.toString()+ "Must implement");
//        }
//    }

    ////End callback ////////
    private long displayTodaysCalories(){

        long calorieTotal = calorieExecute();
        makeCaloriePieChart(chart_3);
        return calorieTotal;
    }
    private void googleGoalAPI(){
        PendingResult<GoalsResult> pendingResult =
                Fitness.GoalsApi.readCurrentGoals(
                        mClient,
                        new GoalsReadRequest.Builder()
                                .addDataType(DataType.TYPE_STEP_COUNT_DELTA)
                                .addDataType(DataType.TYPE_CALORIES_EXPENDED)
                                .build());

        GoalsResult readDataResult = pendingResult.await();
        List<Goal> goals = readDataResult.getGoals();
        goalSteps = (int)goals.get(0).getMetricObjective().getValue();
        goalCalorie = (int)goals.get(1).getMetricObjective().getValue();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e("HistoryAPI", "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e("HistoryAPI", "onConnectionFailed");
    }

    private class ViewWeekStepCountTask extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {
            displayLastWeeksData();
            return null;
        }
    }

    private class ViewTodaysStepCountTask extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {
            displayStepDataForToday();
            return null;
        }
    }
    private class ViewTodaysCalorie extends AsyncTask<Object, Object, Void> {
        protected Void doInBackground(Object... params) {
            calorieTotal = displayTodaysCalories();;
            return null;
        }
    }

    ///////////////Charts ////////////////////////////////////////////////////////
    private void makeLineChart(LineChart chart){
        LineDataSet setComp1 = new LineDataSet(entries, "Steps");
        setComp1.setAxisDependency(YAxis.AxisDependency.LEFT);
        setComp1.setColor(Color.rgb(248,203,67));
        setComp1.setCircleColor(Color.rgb(248,203,67));
        setComp1.setValueTextSize(12f);
        setComp1.setFormSize(12f);

        // use the interface ILineDataSet
        List<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
        dataSets.add(setComp1);
        LineData data = new LineData(dataSets);


        // XAxis properties
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(12f);
        xAxis.setDrawAxisLine(true);
        xAxis.setDrawGridLines(false);
        // set a custom value formatter for integer
        xAxis.setValueFormatter(new axisValueFormatter());
        xAxis.setGranularity(1f);
        xAxis.setDrawLabels(true);
        // YAxis properties
        YAxis left = chart.getAxisLeft();
        left.setDrawLabels(false); // no axis labels
        left.setDrawAxisLine(false); // no axis line
        left.setDrawZeroLine(true); // draw a zero line

        chart.getAxisRight().setEnabled(false); // no right axis
        Description desc = new Description();
        desc.setText("");
        chart.setDescription(desc);

        chart.setData(data);
        invalidate(chart, null);
    }
    private void makeStepPieChart(PieChart chart){
        googleGoalAPI();


        if((float)goalSteps-stepTotal<0)
            pie_stepEntries.add(new PieEntry((float) stepTotal, "Step Goal Reached"));
        else {
            pie_stepEntries.add(new PieEntry((float) stepTotal, "Today's Steps"));
            pie_stepEntries.add(new PieEntry((float) goalSteps-stepTotal, "Step Left to Goal"));
        }

        PieDataSet set = new PieDataSet(pie_stepEntries, "");
        set.setValueTextSize(10f);
        set.setColors(Color.rgb(248, 203, 67),Color.rgb(33, 194, 147));
        PieData data = new PieData(set);
        chart.setData(data);

        Description desc = new Description();
        desc.setText("");
        chart.setDescription(desc);
        chart.setEntryLabelTextSize(10f);
        chart.setEntryLabelColor(Color.BLACK);
        chart.setClickable(false);
        chart.setTouchEnabled(false);
        chart.setHoleRadius(40f);
        invalidate(null, chart);
    }
    private void makeCaloriePieChart(PieChart chart){


        if(goalCalorie-calorieTotal<0){
            pie_calorieEntries.add(new PieEntry((float) calorieTotal, "Goal Achieved!"));
        }

        else{
            pie_calorieEntries.add(new PieEntry((float) calorieTotal, "Today's Calories Burned"));
            pie_calorieEntries.add(new PieEntry((float) goalCalorie-calorieTotal, "Calories Left to Goal"));
        }


        PieDataSet set = new PieDataSet(pie_calorieEntries, "");
        set.setColors(Color.rgb(248, 203, 67),Color.rgb(33, 194, 147));
        PieData data = new PieData(set);
        set.setValueTextSize(10f);
        chart.setData(data);

        Description desc = new Description();
        desc.setText("");
        chart.setDescription(desc);
        chart.setEntryLabelTextSize(10f);
        chart.setEntryLabelColor(Color.BLACK);
        chart.setClickable(false);
        chart.setTouchEnabled(false);
        chart.setHoleRadius(40f);
        invalidate(null, chart);
    }

    public class axisValueFormatter implements IAxisValueFormatter {

        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            return dateList.get((int)value).toString();
        }
    }

    private void invalidate(final LineChart chart1, final PieChart chart2){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(chart1==null)
                    chart2.invalidate();
                else
                    chart1.invalidate();
            }
        });

    }
}
