# Trinity

[![Maven Build](https://github.com/Birdasaur/Trinity/actions/workflows/maven.yml/badge.svg)](https://github.com/Birdasaur/Trinity/actions/workflows/maven.yml)
[![Gradle Build](https://github.com/Birdasaur/Trinity/actions/workflows/gradle.yml/badge.svg)](https://github.com/Birdasaur/Trinity/actions/workflows/gradle.yml)

## Explainable AI (XAI) Analysis and Visualization tool ##

![Trinity City](/media/TrinityCity.png)

Trinity provides performance analysis and XAI tools ideal for Deep Learning systems or other models performing complex
classification or decoding.
Trinity does this through a combination of different interactive 3D projections that are hyper-dimensional aware. (Vectors of Vectors)

### Hyperspace ###
![Hyperspace Projection](/media/TrinityChatGPT_Text_Embeddings.png)
Trinity's Hyperspace view provides a 3D scatter plot with automatic 2D projections of feature/factor data.
The dimensional combinations can be instantly switched to rapidly search through hyper-dimensional space.
The user can pan, rotate and zoom either the 3D camera or the points themselves.
The scatter points are interactive allowing the user to select individual points to bring up the associated data/imagery with that feature.

### Hypersurface ###
![TrinityBCI-Hypersurface](/media/TrinityBCI-Hypersurface.png)
Trinity can visualize higher dimensional inputs (before decoding) as a 3D surface.
Hypersurface view provides analyst insight into what inputs correlate strongest with a decoding/classification result.
This view is synchronized with the same FeatureVectors and Timeline as the Hyperspace viewpoint.

### Projections and Manifolds ###
![TrinityBCI-UMAP-Yule](/media/TrinityBCI-UMAP-Yule.png)
Trinity provides a fast parallelized UMAP tool with a simple to use GUI to project the hyper-dimensional embeddings
down to an arbitrary lower dimensional space. This allows analysts to project approximate manifolds as 3D clusters.

### Data Formats ###
Trinity primarily speaks JSON and has a collection of serializable JSON message objects that can be imported.
The primary message that most applications will leverage is the FeatureVector.
Example:

```json
{
    "messageType": "feature_vector",
    "messageId": 0,  #optional long value for order or id of data point
    "data": [-0.4232191175986961, -0.5031443592838056, 0.30497221256679125,
        0.4157550505277444, -0.46174460409303325, -0.12950797668733202,
        0.6323170694189965, 0.20112482321095512, -0.0770502704073328,
            #... some arbitrarily long vector of embeddings...#
        -0.021781132983331605, 0.2855062868586593, -0.11389146262348109,
        -0.4338320677142379, 0.14545007041168245, 0.34325194689681915
    ],
    "score": -2.753245759396493, #Typically the classification score provide by model but could be any floating point value you choose
    "pfa": 0.0008605957637858228, #Auxiliary floating point between 0 and 1 typically associated with a probability
    "label": "some_object",  #human readable string that is categorical
    "bbox": [0.0, 0, 16.0, 0], #optional Coordinate set typically used for Bounding box identification but could be used for anything
    "imageURL": "/media/images/video_frame_9001.jpg", #image associated with this data point. Supports *.png or *.jpg files. Also supports http based urls
    "layer": 3, #Typically the layer of the model the embeddings were taken from but can represent any Integer based hierarchal info
    "metaData" : { # Totally optional info hash map
        "some name" : "some value",
        "optional" : "string name/value pairs that provide additional info"
    }
}
```

FeatureVector objects can be sent to Trinity as a stream using ZeroMQ. The ZeroMQ connection is configured from the Data UI panel.
Large collections of FeatureVector objects can be loaded at once as a file drag and drop using the FeatureCollection JSON object.
The FeatureCollection object is simply an array of FeatureVector objects with a type field that Trinity uses to detect file type at Drag and Drop

```json
{   "type": "FeatureCollection",
    "features": [
        ...boat load of FeatureVector objects
    ]
}
```

Trinity will auto colorize the data in both the Hyperspace and Projections views by the label field. It uses a rotational color map that has 12 predefined colors.
Colors can be reassigned to color gradients using score, layer, pFa or even raw coordinate position via the GUI.
The user can create custom color maps by label using the LabelConfig message.
Labels can be explicitly colored by RGBA hex code.
The LabelConfig also supports Java compatible regular expression wildcards.
A LabelConfig json file can be simply dragged and dropped onto the Trinity application and it will automatically update the views.

```json
{
    "messageType": "label_config",
    "wildcards" : {
        "human_.*":"#0000FFFF",
        "human_Original.*":"#FFFF00FF",
        "chatGPT_.*":"#FF0000FF",
        "chatGPT_Original.*":"#00FF00FF"
    },
    "clearAll" : "false"
}
```

### Example Use Cases ###
Trinity has been applied to a series of use cases including:

**Deep Learning Object detection models**
![Trinity-Competency-UMAP](/media/Trinity-Competency-UMAP.png)


**COVID gene/tissue classification**
![TrinityCOVIDTissueGeneSequenceUMAP-Yule](/media/TrinityCOVIDTissueGeneSequenceUMAP-Yule-000.png)


**Brain Computer Interface decoders**
![TrinityBCI-Hyperspace](/media/TrinityBCI-Hyperspace.png)

**Large Language Model (ChatGPT) Embeddings Analysis**
![TrinityHumanVsChatGPTEmbeddings-UMAP-Yule](/media/TrinityHumanVsChatGPTEmbeddings-UMAP-Yule.png)


## Project contributors: ##
![airplanelaugh](/media/airplanelaugh.jpg)
- Sean M Phillips
- Melanie Lockhart
- Samuel Matos
- Gene Whipps
- Griffin Milsap
- David Newcomer
- Luis Puche Rondon

## Building and Running

You can build with either `Maven` or `Gradle` with a modern version of Java (>=17).
There's already a set of scripts for building and running if you use a Jetbrains IDE or Netbeans to facilitate a cold start on the project.
To run the project from a jar after building, you can take a look at the `scripts` directory to get you started.
Otherwise, make sure to use at least `-Dprism.maxvram=2G` on your JVM parameters when starting it up.
For JLink/JPackage builds those JVM args are baked in already into the packages.
