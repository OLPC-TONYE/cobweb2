package org.cobweb.cobweb2.ui.swing;


import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.cobweb.cobweb2.Simulation;
import org.cobweb.cobweb2.ui.SimulationRunner;
import org.cobweb.cobweb2.ui.StatsTracker;
import org.cobweb.cobweb2.ui.UpdatableUI;
import org.cobweb.cobweb2.ui.ViewerClosedCallback;
import org.cobweb.cobweb2.ui.ViewerPlugin;
import org.cobweb.swingutil.JComponentWaiter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

// Contributed by Charles-Okhide Tonye

public class DiseaseMonitor implements UpdatableUI, ViewerPlugin{

	private JFrame graph = new JFrame("Diseases");

	private JComponentWaiter graphSynchronizer = new JComponentWaiter(graph);

	private int frame = 0;


	private static final int frameskip = 50;


	private XYSeries agentData = new XYSeries("Healthy Agents");
	private XYSeries infectedAgentData = new XYSeries("Infected Agents");
	private XYSeries vaccinatedAgentData = new XYSeries("Vacinated Agents");

	private XYSeriesCollection data = new XYSeriesCollection();
	private JFreeChart plot = ChartFactory.createXYLineChart(
			"Disease Tracker"
			, "Time"
			, "Count"
			, data
			, PlotOrientation.VERTICAL
			, true
			, false
			, false);

	private JPanel statsPanel = new JPanel();

	private SimulationRunner scheduler;

	private StatsTracker statsTracker;

	private final JTabbedPane tabbedPane;

	public DiseaseMonitor(SimulationRunner scheduler) {
		this.scheduler = scheduler;

		tabbedPane = new JTabbedPane();

		graph.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentHidden(ComponentEvent e) {
				if (onClosed != null)
					onClosed.viewerClosed();
			}
		});

		graph.setSize(500, 500);
		ChartPanel cp = new ChartPanel(plot, true);



		tabbedPane.addTab("Stats", statsPanel);
		tabbedPane.addTab("Graph", cp);

		graph.add(tabbedPane);
		plot.setAntiAlias(true);
		plot.setNotify(false);

		data.addSeries(agentData);
		data.addSeries(infectedAgentData);
		data.addSeries(vaccinatedAgentData);

		statsPanel.add(healthyAgentsLabel);
		statsPanel.add(infectedAgentsLabel);
		statsPanel.add(vaccinatedAgentsLabel);

		statsTracker = new StatsTracker((Simulation) this.scheduler.getSimulation());

		scheduler.addUIComponent(this);
	}

	@Override
	public void dispose() {
		off();
		scheduler.removeUIComponent(this);
		graph.dispose();
		graph = null;
	}

	JLabel healthyAgentsLabel = new JLabel();
	JLabel infectedAgentsLabel = new JLabel();
	JLabel vaccinatedAgentsLabel = new JLabel();

	@Override
	public void update(boolean sync) {
		long time = statsTracker.getTime();
		long agentCount = statsTracker.countHealthyAgents();
		long infectedCount = statsTracker.countDiseasedAgents();
		long vacinatedCount = statsTracker.countVaccinatedAgents();

		agentData.add(time, agentCount);
		infectedAgentData.add(time, infectedCount);
		vaccinatedAgentData.add(time, vacinatedCount);

		healthyAgentsLabel.setText("Healthy Agents: " + agentCount);
		infectedAgentsLabel.setText("Infected Agents: " + infectedCount);
		vaccinatedAgentsLabel.setText("Vacinated Agents: " + vacinatedCount);

		if (frame++ >= frameskip) {
			frame = 0;
			plot.setNotify(true);
			plot.setNotify(false);
		}

		graphSynchronizer.refresh(sync);
	}

	@Override
	public boolean isReadyToUpdate() {
		return true;
	}

	public void toggleGraphVisible() {
		graph.setVisible(!graph.isVisible());
	}

	@Override
	public String getName() {
		return "Track Disease";
	}

	@Override
	public void on() {
		graph.setVisible(true);
	}

	@Override
	public void off() {
		graph.setVisible(false);
	}

	private ViewerClosedCallback onClosed;
	@Override
	public void setClosedCallback(ViewerClosedCallback onClosed) {
		this.onClosed = onClosed;
	}

	@Override
	public void onStopped() {
		plot.setNotify(true);
		plot.setNotify(false);
	}

	@Override
	public void onStarted() {
		// Nothing
	}

}
