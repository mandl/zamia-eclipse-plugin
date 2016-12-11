package org.zamia.plugin.marker;

import java.util.ArrayList;

import org.eclipse.ui.PlatformUI;
import org.zamia.plugin.views.sim.SimulatorView;
import org.zamia.plugin.views.sim.TraceLineMarker;

public enum ModelProvider {
	INSTANCE;

	private ArrayList<TraceLineMarker> my;

	private ModelProvider() {
		my = new ArrayList<TraceLineMarker>();

		Update();

	}

	public void Update() {
		SimulatorView simview = (SimulatorView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.findView("org.zamia.plugin.views.sim.SimulatorView");
		my.clear();
		simview.getMarkers(my);

	}

	public ArrayList<TraceLineMarker> getPersons() {
		return my;
	}

}