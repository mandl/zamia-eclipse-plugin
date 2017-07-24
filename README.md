# Zamiacad

http://zamiacad.sourceforge.net/web/

# System requirements

## Java

Zamiacad is based on the Java programming language and requires Java 8 or higher. A Java Runtime Environment (JRE) must be installed on your computer.

Installation of Java is beyond the scope of this document. 

You can find more information on Java and download it for free from Oracle Corporation at https://www.java.com/.

Before downloading Java, make sure you are picking the variant with the correct bit size, i. e. either 32 or 64 bit, whichever is appropriate for your computer.


## Eclipse

Internally, Zamiacad Tools are based on Eclipse, an integrated development environment (IDE). Zamiacad Tools are running inside the Eclipse environment. 

If you are new to Eclipse, you might find the Eclipse Help useful, especially the “Workbench User Guide”.


## Install Jython


Zaniacad uses Python scripting. So we need a Jython interpreter.
 
Download Jython 2.7 **jython-installer-2.7.0.jar**

[Jython 2.7](http://www.jython.org)

Install this version.

[Install Jython 2.7](https://wiki.python.org/jython/InstallationInstructions#id2)

	java -jar jython_installer-2.7.0.jar


![](https://raw.githubusercontent.com/mandl/zamia-eclipse-plugin/master/img/JythonInstallation_01.png)


Select Standard.

![](https://raw.githubusercontent.com/mandl/zamia-eclipse-plugin/master/img/JythonInstallation_02.png)

Select your install path. 

![](https://raw.githubusercontent.com/mandl/zamia-eclipse-plugin/master/img/JythonInstallation_03.png)






## Setup Jython path


Open Eclipse Preferences and set your Jython path.

![](https://raw.githubusercontent.com/mandl/zamia-eclipse-plugin/master/img/PreferencesZamiaCAD.png)


## Zamiacad

Start Zamiacad


![](https://raw.githubusercontent.com/mandl/zamia-eclipse-plugin/master/img/zamiaCAD.png)




    Design entry support
        Syntax Highlighting
        Auto-Completion
        Outlines
    Design navigation
        Instantiation graph (on fully elaborated model)
        Open design unit through instantiation graph
        Open design unit by hierarchical path
        Open signal declaration by hierarchical path
    Simulation
        Waveform Viewer
        Source back-annotation of simulation values
        Waveform file import
            VCD
        Built-in reference simulator (not yet complete)
    Design Analysis
        Reference Search
            for instatiated modules
            for signals across hierarchy levels in the design
        Declaration Search
        File caching to mitigate latencies
        Many features which the eclipse framework provides


## Usinge the internal Simulator

### Starting the Simulator

![](https://raw.githubusercontent.com/mandl/zamia-eclipse-plugin/master/img/DebugConfigurations _020.png)



### Add the signals

![](https://raw.githubusercontent.com/mandl/zamia-eclipse-plugin/master/img/addSignal.png)


### Run the simulator

![](https://raw.githubusercontent.com/mandl/zamia-eclipse-plugin/master/img/runSim.png)


### Add markers and measure time


![](https://raw.githubusercontent.com/mandl/zamia-eclipse-plugin/master/img/addMarker.png)


![](https://raw.githubusercontent.com/mandl/zamia-eclipse-plugin/master/img/MarkerLineLabel _025.png)



![](https://raw.githubusercontent.com/mandl/zamia-eclipse-plugin/master/img/addMarker_2.png)



![](https://raw.githubusercontent.com/mandl/zamia-eclipse-plugin/master/img/addMarker_3.png)












