package org.zamia.plugin.marker;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.zamia.plugin.views.sim.TraceLineMarker;

public class MarkerViewerComparator extends ViewerComparator {
        private int propertyIndex;
        private static final int DESCENDING = 1;
        private int direction = DESCENDING;

        public MarkerViewerComparator() {
                this.propertyIndex = 0;
                direction = DESCENDING;
        }

        public int getDirection() {
                return direction == 1 ? SWT.DOWN : SWT.UP;
        }

        public void setColumn(int column) {
                if (column == this.propertyIndex) {
                        // Same column as last sort; toggle the direction
                        direction = 1 - direction;
                } else {
                        // New column; do an ascending sort
                        this.propertyIndex = column;
                        direction = DESCENDING;
                }
        }

        @Override
        public int compare(Viewer viewer, Object e1, Object e2) {
        	TraceLineMarker p1 = (TraceLineMarker) e1;
        	TraceLineMarker p2 = (TraceLineMarker) e2;
                int rc = 0;
                switch (propertyIndex) {
                case 0:
                        rc = p1.getLabel().compareTo(p2.getLabel());
                        break;
                case 1:
                        rc = p1.getTime().compareTo(p2.getTime());
                        break;
                
                default:
                        rc = 0;
                }
                // If descending order, flip the direction
                if (direction == DESCENDING) {
                        rc = -rc;
                }
                return rc;
        }

}