package com.ghelius.narodmon;

import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.achartengine.util.MathHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MultiGraphActivity extends SherlockFragmentActivity {
	private SensorLogGetter logGetter;
	private int offset = 0;
	private LogPeriod period = LogPeriod.day;
	private final static String TAG = "narodmon-multGactivity";
	MultiGraph graph;
	private ArrayList<Point> logData = new ArrayList<Point>();
	private LogPeriod oldPeriod;


	private GraphicalView mChart;
	private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
	private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
	private TimeSeries timeSeries;
	private XYSeriesRenderer mCurrentRenderer;


	public void onCreate(Bundle savedInstanceState) {
		setTheme(com.actionbarsherlock.R.style.Theme_Sherlock);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.multi_graph_activity);

		Bundle extras = getIntent().getExtras();
		if (extras == null)
			finish();

		graph = (MultiGraph)extras.getSerializable("Graph");
		logGetter = new SensorLogGetter();


		findViewById(R.id.bt_graph_prev).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				offset += 1;
				updateGraph();
			}
		});
		findViewById(R.id.bt_graph_day).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				period = LogPeriod.day;
				offset = 0;
				updateGraph();
			}
		});
		findViewById(R.id.bt_graph_week).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				period = LogPeriod.week;
				offset = 0;
				updateGraph();
			}
		});
		findViewById(R.id.bt_graph_month).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				period = LogPeriod.month;
				offset = 0;
				updateGraph();
			}
		});
		findViewById(R.id.bt_graph_next).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (offset > 0) {
					offset -= 1;
					updateGraph();
				}
			}
		});
	}

	private void initChart() {
		timeSeries = new TimeSeries ("");
		mDataset.addSeries(timeSeries);
		mCurrentRenderer = new XYSeriesRenderer();

		mRenderer.addSeriesRenderer(mCurrentRenderer);
		mRenderer.setShowLabels(true);
		mRenderer.setShowGrid(true);
		mRenderer.setGridColor(0xFF505050);

		mRenderer.setXTitle(getString(R.string.text_today));
		mRenderer.setYLabels(10);
		mRenderer.setPointSize(2f);
		mRenderer.setAxisTitleTextSize(20);
		mRenderer.setChartTitleTextSize(20);
		mRenderer.setLabelsTextSize(15);
		mRenderer.setLegendTextSize(10);
		mRenderer.setYLabelsPadding(-20);
		mRenderer.setXLabelsAlign(Paint.Align.CENTER);
		mRenderer.setXLabels(10);

		mCurrentRenderer.setColor(0xFF00FF00);
		mCurrentRenderer.setPointStyle(PointStyle.CIRCLE);
		mCurrentRenderer.setFillPoints(true);
		mCurrentRenderer.setChartValuesTextSize(15);
	}

	private void updateGraph() {
		findViewById(R.id.marker_progress).setVisibility(View.VISIBLE);
		logGetter.getLog(graph.ids.get(0), period, offset);
		String title = "";
		switch (period) {
			case day:
				if (offset == 0)
					title = getString(R.string.text_today);
				else
					title = String.valueOf(offset) + " " + getString(R.string.text_days_ago);
				break;
			case week:
				if (offset == 0)
					title = getString(R.string.text_this_week);
				else
					title = String.valueOf(offset) + " " + getString(R.string.text_weeks_ago);
				break;
			case month:
				if (offset == 0)
					title = getString(R.string.text_this_month);
				else
					title = String.valueOf(offset) + " " + getString(R.string.text_month_ago);
				break;
			case year:
				if (offset == 0)
					title = getString(R.string.text_this_year);
				else
					title = String.valueOf(offset) + " " + getString(R.string.text_yars_ago);
				break;
		}
		if (offset == 0) {

		}
		mRenderer.setXTitle(title);
	}


	private void addSampleData() {
		if (mChart == null) {
			LinearLayout layout = (LinearLayout) findViewById(R.id.sensorInfoChart);
			initChart();
			mChart = ChartFactory.getTimeChartView(this, mDataset, mRenderer, "H:mm");
			layout.addView(mChart);
			oldPeriod = period;
		}
		int max_gap = 1000*60;
		if (period == LogPeriod.day) {
			max_gap = 60*60; // hour
		} else if (period == LogPeriod.week) {
			max_gap = 100*60;
		} else if (period == LogPeriod.month) {
			max_gap = 24*60*60; //day
		}

		if (oldPeriod != period) { // period was change, we need to create new mChart with other date-time format
			LinearLayout layout = (LinearLayout) findViewById(R.id.sensorInfoChart);
			layout.removeAllViews();
			if (period == LogPeriod.day) {
				mChart = ChartFactory.getTimeChartView(this, mDataset, mRenderer, "H:mm");
			} else if (period == LogPeriod.week) {
				mChart = ChartFactory.getTimeChartView(this, mDataset, mRenderer, "E");
			} else if (period == LogPeriod.month) {
				mChart = ChartFactory.getTimeChartView(this, mDataset, mRenderer, "d");
			} else if (period == LogPeriod.year) {
				mChart = ChartFactory.getTimeChartView(this, mDataset, mRenderer, "M.d");
			}
			oldPeriod = period;
			layout.addView(mChart);
		}
		timeSeries.clear();
		if (!logData.isEmpty()) {
			long prevTime = logData.get(0).time;
			float max = logData.get(0).value;
			float min = logData.get(0).value;
			for (Point data : logData) {
				if (data.value > max) max = data.value;
				if (data.value < min) min = data.value;
				timeSeries.add((data.time * 1000), data.value);
				Log.d(TAG,"cur:"+data.time + " prev:" + prevTime + " diff:" + (data.time-prevTime));
				if ((data.time - prevTime) > max_gap) {
					timeSeries.add(((data.time - 1) * 1000), MathHelper.NULL_VALUE);
				}
				prevTime = data.time;
			}
			mRenderer.initAxesRange(1);
			mRenderer.setYAxisMin(min - (max-min)/10);
			mRenderer.setYAxisMax(max + (max-min)/10);
		}
		mChart.repaint();
		findViewById(R.id.marker_progress).setVisibility(View.INVISIBLE);
	}

	enum LogPeriod {day,week,month,year}
	private class SensorLogGetter implements ServerDataGetter.OnResultListener {
		ServerDataGetter getter;
		void getLog (int id, LogPeriod period, int offset) {
			if (getter != null) {
				getter.cancel(true);
			}
			Log.d(TAG, "Getting log for id:" + id + " period:" + period.name() + " offset:" + offset);
			getter = new ServerDataGetter();
			getter.setOnListChangeListener(this);
			String sPeriod = "";
			switch (period) {
				case day:
					sPeriod = "day";
					break;
				case week:
					sPeriod = "week";
					break;
				case month:
					sPeriod = "month";
					break;
				default:
					break;
			}
			getter.execute(NarodmonApi.apiUrl, ConfigHolder.getInstance(getApplicationContext()).getApiHeader() + "\"cmd\":\"sensorLog\"," +
					"\"id\":\""+id+"\",\"period\":\"" + sPeriod + "\",\"offset\":\""+ offset +"\"}");
		}


		@Override
		public void onResultReceived(String result) {
			logData.clear();
			Log.d(TAG,"sensorLog received: " + result);
			try {
				JSONObject jsonObject = new JSONObject(result);
				JSONArray arr = jsonObject.getJSONArray("data");
				for (int i = 0; i < arr.length(); i++) {
					logData.add(new Point(Long.valueOf(arr.getJSONObject(i).getString("time")),
							Float.valueOf(arr.getJSONObject(i).getString("value"))));
				}
			} catch (JSONException e) {
				Log.e(TAG,"SensorLog: Wrong JSON " + e.getMessage());
			}
			// now we have full data, paint graph
			Log.d(TAG,"add log data to graph, items count: " + logData.size());
			addSampleData();
			getter = null;
		}

		@Override
		public void onNoResult() {
			Log.e(TAG, "getLog: no data");
		}
	}

	class Point {
		public long time;
		public float value;

		public Point(Long time, Float value) {
			this.time = time;
			this.value = value;
		}
	}
}