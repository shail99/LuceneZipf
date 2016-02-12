package edu.neu.cs6200.luceneSimpleAnalyzer;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

@SuppressWarnings("serial")
public class Plot extends JFrame{
	
	public Plot(LinkedHashMap<String,Long> terms)
	{
		gui(terms);
	}
	
	public void gui(LinkedHashMap<String, Long> terms)
	{
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		for(Map.Entry<String, Long> entry : terms.entrySet())
			dataset.setValue(entry.getValue(), "Frequency" ,entry.getKey());
		
		JFreeChart chart = ChartFactory.createLineChart("Zipf's law", "Unique Terms (By decreasing frequency)", "Frequency", dataset, PlotOrientation.VERTICAL, false, true, false);
		CategoryPlot p = chart.getCategoryPlot();
		p.setRangeGridlinePaint(Color.BLACK);
		ChartFrame frame = new ChartFrame("Graph stating the Zipf's Law",chart);
		frame.setVisible(true);
		frame.setSize(1200, 800);
		
	}

}
