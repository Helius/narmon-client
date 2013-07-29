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
	private int offset = 0;
	private LogPeriod period = LogPeriod.day;
	private final static String TAG = "narodmon-multGactivity";
	MultiGraph multiGraph;
	private LogPeriod oldPeriod;
	private GraphicalView mChart = null;
	private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
	private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
	private ArrayList<Graph> graphs = new ArrayList<Graph>();
	private int colors [] = {0xFFFF0000, 0xFF00FF00, 0xFF00FFFF, 0xFF8144E3, 0xFF3EF0E4, 0xFFEAF03E};
	float global_min = 0;
	float global_max = 0;
	private int max_gap = 1000*60;

	void timeSeriesReady (float min, float max) {
		int ready_cnt = 0;

		for (Graph graph : graphs) {
			if (graph.ready) ready_cnt++;
		}

		if (ready_cnt == 1) { // init range with first graph value
			global_max = max;
			global_min = min;
		} else {
			if (global_min > min) global_min = min;
			if (global_max < max) global_max = max;
		}

		if (ready_cnt == graphs.size()) {
			mRenderer.initAxesRange(1);
			mRenderer.setYAxisMin(global_min - (global_max - global_min) / 10);
			mRenderer.setYAxisMax(global_max + (global_max - global_min) / 10);
			findViewById(R.id.marker_progress).setVisibility(View.INVISIBLE);
			mChart.repaint();
		}
	}

	class Graph {
		int id;
		SensorLogParser logGetter;
		String name;
		boolean show = true;
		TimeSeries timeSeries = new TimeSeries("");
		private boolean ready = false;

		public Graph (int id) {
			this.id = id;
			logGetter = new SensorLogParser(id);
		}

		void dataReceived(ArrayList<Point> logData) {
			timeSeries.clear();
			if (logData != null && !logData.isEmpty()) {
				long prevTime = logData.get(0).time;
				float max = logData.get(0).value;
				float min = logData.get(0).value;
				for (Point data : logData) {
					if (data.value > max) max = data.value;
					if (data.value < min) min = data.value;
					timeSeries.add((data.time * 1000), data.value);
					Log.d(TAG, "cur:" + data.time + " prev:" + prevTime + " diff:" + (data.time - prevTime));
					if ((data.time - prevTime) > max_gap) {
						timeSeries.add(((data.time - 1) * 1000), MathHelper.NULL_VALUE);
					}
					prevTime = data.time;
				}
				ready = true;
				timeSeriesReady(min, max);
			} else
				timeSeriesReady(0, 0);
		}

		public void getLog(LogPeriod period, int offset) {
			ready = false;
			logGetter.getLog(period, offset);
		}

		private class SensorLogParser implements ServerDataGetter.OnResultListener {
			private int id;
			private ArrayList<Point> logData = new ArrayList<Point>();

			public SensorLogParser(int id) {
				this.id = id;
			}
			ServerDataGetter getter;
			void getLog (LogPeriod period, int offset) {
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
				dataReceived(logData);
				getter = null;
			}

			@Override
			public void onNoResult() {
				Log.e(TAG, "getLog: no data");
			}
		}
	}

//	class MultiGraphListItemAdapter extends ArrayAdapter<Integer> {
//
//		MultiGraphListItemAdapter(Context context, ArrayList<Integer> items) {
//			super(context, R.layout.bla, items);
//		}
//
//
//	}

	public void onCreate(Bundle savedInstanceState) {
		setTheme(com.actionbarsherlock.R.style.Theme_Sherlock);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.multi_graph_activity);

		Bundle extras = getIntent().getExtras();
		multiGraph = (MultiGraph) (extras != null ? extras.getSerializable("Graph") : null);
		if (multiGraph == null) {
			finish();
		}

		for (int i = 0; i < multiGraph.ids.size(); i++) {
			graphs.add(new Graph(multiGraph.ids.get(i)));
		}

//		ListView graphList = (ListView)findViewById(R.id.graphsListOnChart);
//		graphList.setAdapter();


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

	private void updateGraph() {

		max_gap = 1000*60;
		if (period == LogPeriod.day) {
			max_gap = 60*60; // hour
		} else if (period == LogPeriod.week) {
			max_gap = 100*60;
		} else if (period == LogPeriod.month) {
			max_gap = 24*60*60; //day
		}

		findViewById(R.id.marker_progress).setVisibility(View.VISIBLE);
		for (Graph graph : graphs) {
			graph.getLog(period, offset);
		}
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
		mRenderer.setXTitle(title);

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
	}

	private void initChart() {
		for (int i = 0; i < graphs.size(); i++) {
			Graph graph = graphs.get(i);
			mDataset.addSeries(graph.timeSeries);
			XYSeriesRenderer mCurrentRenderer = new XYSeriesRenderer();
			mCurrentRenderer.setColor(colors[i%6]);
			mCurrentRenderer.setPointStyle(PointStyle.CIRCLE);
			mCurrentRenderer.setFillPoints(true);
			mCurrentRenderer.setChartValuesTextSize(15);
			mRenderer.addSeriesRenderer(mCurrentRenderer);
		}

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
		mRenderer.setShowLegend(true);
		mRenderer.setYLabelsPadding(-20);
		mRenderer.setXLabelsAlign(Paint.Align.CENTER);
		mRenderer.setXLabels(10);

	}

	@Override
	public void onResume () {
		super.onResume();
		if (mChart == null) {
			LinearLayout layout = (LinearLayout) findViewById(R.id.sensorInfoChart);
			initChart();
			mChart = ChartFactory.getTimeChartView(this, mDataset, mRenderer, "H:mm");
			layout.addView(mChart);
			oldPeriod = period;
			updateGraph();
		}
	}

	enum LogPeriod {day,week,month,year}

	class Point {
		public long time;
		public float value;

		public Point(Long time, Float value) {
			this.time = time;
			this.value = value;
		}
	}
}