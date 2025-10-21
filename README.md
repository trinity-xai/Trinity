# Trinity

[![Maven Build](https://github.com/Birdasaur/Trinity/actions/workflows/maven.yml/badge.svg)](https://github.com/Birdasaur/Trinity/actions/workflows/maven.yml)
[![Gradle Build](https://github.com/Birdasaur/Trinity/actions/workflows/gradle.yml/badge.svg)](https://github.com/Birdasaur/Trinity/actions/workflows/gradle.yml)

[Trinity YouTube Playlist](https://youtube.com/playlist?list=PLrMR7Y6k6mkDVfYpqrqvaxoti4E2tJXhW&feature=shared)

[Developer Quick Start](/DEVQUICKSTART.md)

## Explainable AI (XAI) Analysis and Visualization tool

![Trinity City](/media/TrinityCity.png)

Trinity provides performance analysis and XAI tools ideal for Generative AI and
Deep Learning systems performing complex classification or decoding. Trinity
does this through a combination of different interactive 3D projections that are
hyper-dimensional aware. (Vectors of Vectors)

### Example Use Cases

Trinity has been applied to a wide range of problems which can be framed as
vectors of vectors:

**Deep Fake Image Detection**
![TrinityDeepFake-HypersurfacePixelSelection](/media/TrinityDeepFake-HypersurfacePixelSelection.png)

**Deep Fake Audio Detection**
![Trinity-HypersurfaceAudioFFT](/media/Trinity-HypersurfaceAudioFFT.png)

**Large Language Model (ChatGPT etc) Embeddings Analysis**
![TrinityChatGPT-Manifold](/media/TrinityChatGPT-Manifold.png)

**Deep Learning Object detection models**
![Trinity-Competency-UMAP](/media/Trinity-Competency-UMAP.png)

**COVID gene/tissue classification**
![TrinityCOVIDTissueGeneSequenceUMAP-Yule](/media/TrinityCOVIDTissueGeneSequenceUMAP-Yule-000.png)

**Brain Computer Interface decoders**
![TrinityBCI-Hyperspace](/media/TrinityBCI-Hyperspace.png)

### Hyperspace

![Hyperspace Projection](/media/TrinityChatGPT_Text_Embeddings.png) Trinity's
Hyperspace view provides a 3D scatter plot with automatic 2D projections of
feature/factor data. The dimensional combinations can be instantly switched to
rapidly search through hyper-dimensional space. The user can pan, rotate and
zoom either the 3D camera or the points themselves. The scatter points are
interactive allowing the user to select individual points to bring up the
associated data/imagery with that feature.

### Hypersurface

#### Feature Collection Mode

![TrinityBCI-Hypersurface](/media/TrinityBCI-Hypersurface.png)

Trinity can visualize higher dimensional inputs (before decoding) as a 3D
surface. Hypersurface view provides analyst insight into what inputs correlate
strongest with a decoding/classification result. This view is synchronized with
the same FeatureVectors and Timeline as the Hyperspace viewpoint.

#### 3D Imagery

![Trinity-HypersurfaceOnyx](/media/Trinity-HypersurfaceOnyx.png)

The Hypersurface can also be utilized to visualize 2D imagery in 3D. Each pixel
of an input image is used to create Vertices and Faces of an underlying
TriangleMesh. The surface height and colormap can be determined by multiple
variants including luminosity (default), aligned FeatureVector or Shapley Value.

### Projections and Manifolds

#### UMAP and Dimension Reduction

![TrinityBCI-UMAP-Yule](/media/TrinityBCI-UMAP-Yule.png)

Trinity provides a fast parallelized UMAP tool with a simple to use GUI to
project the hyper-dimensional embeddings down to an arbitrary lower dimensional
space. This allows analysts to project approximate manifolds as 3D clusters.

#### Manifolds and Clustering

Integrated in the Projections view are tools to automatically perform
clustering.
![Trinity-ProjectionsKmeansClustering](/media/Trinity-ProjectionsKmeansClustering.png)

Clustering algorithm selection and parameters allow for full control by the
user. Discovered Clusters are automatically converted to 3D TriangleMeshes using
a ConvexHull algorithm.
![Trinity-ProjectionsHDDBSCANClustering](/media/Trinity-ProjectionsHDDBSCANClustering.png)

**_Special Shoutout and Acknowledgement to clust4j_**

Most Clustering Algorithms (with the exception of the Multivariate Gaussian
Mixture Models) used by Trinity XAI are derived from
[the brilliant clust4j project](https://github.com/tgsmith61591/clust4j). At the
time of writing clust4j was not available as 3rd party library nor was it module
compatible. The Trinity XAI org decided to import the Apache 2.0 version of
clust4j, make some changes to be module compatible and fixed a few deprecated
calls (mostly in unit tests). It has functioned perfectly and we thank the
author.

### Data

Trinity primarily speaks JSON and has a collection of serializable JSON message
objects that can be imported. The primary message that most applications will
leverage is the FeatureVector.

#### FeatureVector

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

#### FeatureCollection

FeatureVector objects can be sent to Trinity as a stream using ZeroMQ. The
ZeroMQ connection is configured from the Data UI panel. Large collections of
FeatureVector objects can be loaded at once as a file drag and drop using the
FeatureCollection JSON object. The FeatureCollection object is simply an array
of FeatureVector objects with a type field that Trinity uses to detect file type
at Drag and Drop

```json
{   "type": "FeatureCollection",
    "features": [
        ...boat load of FeatureVector objects
    ]
}
```

#### LabelConfig

Trinity will auto colorize the data in both the Hyperspace and Projections views
by the label field. It uses a rotational color map that has 12 predefined
colors. Colors can be reassigned to color gradients using score, layer, pFa or
even raw coordinate position via the GUI. The user can create custom color maps
by label using the LabelConfig message. Labels can be explicitly colored by RGBA
hex code. The LabelConfig also supports Java compatible regular expression
wildcards. A LabelConfig json file can be simply dragged and dropped onto the
Trinity application and it will automatically update the views.

```json
{
  "messageType": "label_config",
  "wildcards": {
    "human_.*": "#0000FFFF",
    "human_Original.*": "#FFFF00FF",
    "chatGPT_.*": "#FF0000FF",
    "chatGPT_Original.*": "#00FF00FF"
  },
  "clearAll": "false"
}
```

#### Hyperdrive Imports

File based imports require some process to generate the JSON. Alternatively the
Hyperdrive tool can perform bulk imports of imagery and/or text, providing
functions to convert to embedding vectors, assign labels and other metadata.
![TrinityHyperdriveImport](/media/TrinityHyperdriveImport.png)

Labels can be set manually by the user or auto-assigned. Auto-assignment has two
methods:

- Prompted Vision Model (Image only)
- Landmark Similarity (Image or Text)

If you have access to a Vision model via the Services client, Trinity provides
simple prompts to request a single English language noun as a label, as well as
explanation/description fields. These prompts are located in the services
directory described below.

Landmark Similarity is a powerful and efficient way to automatically assign
labels. Trinity allows the user to add text or images as landmarks and assign
labels to these. Trinity can then vectorize the landmarks using a multimodal
embedding model (via the REST based functions in Hyperdrive) From there the user
can auto-assign labels to all imported items based on their vector distance to
the landmarks. This distance is computed in the hyperdimensional space and the
distance metric is selectable by the user.

![Trinity-HyperdriveLabelByLandmark](/media/Trinity-HyperdriveLabelByLandmark.png)

Hyperdrive accomplishes this by allowing for a REST based exchange with
embedding and vision models hosted via an OpenAI API compatible structure. The
Hyperdrive Services tab provides support for configuring your connection
including the base URL hosting models, endpoints for both Embeddings
(Image/Text) and Chat (Completion/Vision/etc).

Users can even select which models they wish to use based on the results of
standard 'isAlive' requests.

![TrinityHyperdriveServices](/media/TrinityHyperdriveServices.png)

These services are intended to provide a loose coupling to your REST host. The
base URL, endpoints and default model names are all configurable via the
defaultAccessLayer.json file. Trinity will search for this file by default in a
directory called services which should be located relative to the execution (ie
current working directory). In development mode this will simply be a directory
relative to your projection workspace. When executing from a JAR, this will
likely be the same directory as the JAR. When executing from a native package,
it will vary but ultimately be located somewhere inside the package. This
services directory also contains default Prompts, all of which can be
dynamically reloaded at runtime.

## 2D Helper Tools

3D is cool and all but 2D is the OG. Trinity uses a transparent overlay system
of 2D panes to provide extra helper tools. These overlays are extensions of the
[totally amazing LitFX Project](https://github.com/Birdasaur/LitFX). The genius
of the author of LitFX is rivaled only possibly by the author's stunning
lumberjack good looks. A few of these helper tools are shown below:

### Probability Density and Cumulative Distribution Functions

![Trinity-PDFCDF-Generator](/media/Trinity-PDFCDF-Generator.png)

### Joint Probability Density Grid

![Trinity-JointPDFGenerator](/media/Trinity-JointPDFGenerator.png)

This grid of Joint PDFs becomes a diagnostic dashboard for understanding and 
improving scoring systems. Each plot thumbnail shows how two of the dimensions 
of a vector system behave together across many samples. The color pattern 
indicates a density of occurrence and can indicate strength of presence over time.

A Pearson correlation coefficient for each pair of variables/dimensions is computed.
This provides a correlation score between -1 and 1:

+1: Perfect positive relationship (metrics move together).

0: No relationship.

-1: Perfect negative relationship (as one goes up, the other goes down).

This allows Trinity to order the combinations by correlation and establish a 
ranking. Correlation ranking illuminates which metrics overlap in meaning versus 
which ones bring new perspective.

Ranking:

Descending (high → low correlation):

Pairs at the top are most similar — possibly redundant metrics.
Pairs at the bottom are least related — they provide unique information.

Ascending (low → high correlation):

Pairs at the top are most distinct — potentially the most valuable for diversifying how we measure similarity.
Pairs at the bottom are redundant — maybe candidates for pruning or simplifying the model.


### Similarity and Divergence Matrix

![Trinity-SimilarityMatrix](/media/Trinity-SimilarityMatrix.png)
 

### Natural Language Query

There is a command terminal that you can enter natural language queries to using
the following syntax:

FIND your-natural-language-query

The Terminal vectorizes the statement using the currently selected embeddings
model in the Hyperdrive services tab. Trinity will automatically search through
the Hyperspace data finding the top ten closest results based on distance. The
distance calculation uses the currently selected distance metric in the Services
tab.

![Trinity-TerminalNaturalLanguageQuery](/media/Trinity-TerminalNaturalLanguageQuery.png)

Any points outside the top ten closest matches in terms of vector distance are
blacked out. The number one closest match is highlighted with a Callout.
Entering the CLEAR_FILTERS command into the terminal restores the colorations.

### Content Navigator

When viewing dense scatterplots linked to feature vectors often visual groupings
of data points become evident. This is especially true after applying dimension
reduction and clustering algorithms. While the anchored callouts are a good way
for the user to maintain precise drill down insight for specific points of
interest, it is cumbersome to click on all the points of a dense cluster. To
assist the user in visually identifying the similarities in images that are
projected within a local cluster/neighborhood, Trinity provides a Content
Navigator 2D overlay pane.

![Trinity-ProjectionsContentNavigator](/media/Trinity-ProjectionsContentNavigator.png)

The Content Navigator provides a standard 2D rendering of an image or text
content (depending on media type) overlaid on the 3D view currently in place.
When enabled, the Content Navigator will instantly update its content view with
whatever image/text is linked to any point the user hovers the mouse over.

From this overlay other image and text manipulation tools can be injected with
the content currently loaded in the Navigator.

### COCO Annotations Viewer

Common with perception based AI workflows is to perform segmentation and
annotation marking of images. Bounding Boxes and segmentation Polygons are
usually generated and inteneded to be overlaid on the base image. A common
standard used for this is COCO. Trinity provides a COCO Viewer tool:

![Trinity-CocoViewer](/media/Trinity-CocoViewer.png)

The COCO Viewer tool is smart enough to load multiple images from a single COCO
JSON file and allow the user to mix and match annotation overlays in-situ.

### Image Inspection

To assist with fine grain examination of imagery and to identify artifacts left
by deep fake generators, Trinity provides an Image based FFT tool with frequency
filter. This inspection tool helps perform actions like edge detection and
high/low frequency changes that are often signs of a deep fake generator.

![Trinity-FFT-Filter-ImageInspector](/media/Trinity-FFT-Filter-ImageInspector.png)

RGB content from the FFT workflow, either original image, spectral image or
inverse FFT, can be tessellated into the Hypersurface on demand.

## Project contributors:

![airplanelaugh](/media/airplanelaugh.jpg)

- Sean M Phillips
- Melanie Lockhart
- Samuel Matos
- David Penn
- Gene Whipps
- Griffin Milsap
- David Newcomer
- Luis Puche Rondon

## Building and Running

You can build with either `Maven` or `Gradle` with a modern version of Java
(>=17). There's already a set of scripts for building and running if you use a
Jetbrains IDE or Netbeans to facilitate a cold start on the project. To run the
project from a jar after building, you can take a look at the `scripts`
directory to get you started.

Setting the JavaFX parameter `-Dprism.maxvram=2G` when starting it up will
increase the VRAM that JavaFX allocates from your video card to 2GB. This is
necessary for very large datasets or 4k displays, as by default JavaFX only
pre-allocates 512mb. This value can be tuned to your system and needs. For
JLink/JPackage builds those JVM args are baked in already into the packages.

## Troubleshooting

**Weird/scary <local9> Prism errors** These are exceptions being thrown WAY down
in the weeds due to memory failures in the native Prism code. There are
currently two known causes for this:

- Weird Scaling settings: JavaFX is optimized for 50/100/150/200 percent scaling
  and can throw the above error when using anything in between
- High Resolution Displays: JavaFX supports texture sizes of 4k and even 8k, but
  you need to allocate enough VRAM to allow for it. The default VRAM allocation
  is only 512mb. This can be updated using `-Dprism.maxvram=2G` as a runtime
  parameter. See above for more details.

**Execution Permissions** might need to be needed to run the `JPackage`,
`JLink`, or `Native` builds depending on which system you're running from. For
example on OSX systems you might get `Unknown error: 111` or launch errors,
hence you need to allow the app through GateKeeper via
`xattr -r -d com.apple.quarantine /path/to/Trinity.app`. You might also need to
add execution permissions in some cases via
`chmod +x /path/to/Trinity.app/Contents/MacOS/Trinity` when using the `JPackage`
build.

**Enabling 3D Rendering on Ubuntu Systems** Sometimes when running JavaFX
applications with 3D scenes on an Ubuntu machine you get the
`Scene3D.conditionalfeature` error at runtime. The application and all 2D
components will continue to function but any 3D subscenes and nodes will simply
not render, while the log will be getting crushed by the above error.

The following config tweak for Ubuntu to enable 3D rendering for JavaFX comes
from this post:
https://stackoverflow.com/questions/30288837/warning-system-cant-support-conditionalfeature-scene3d-vmware-ubuntu

When running from a jar file the cmdline arguments that help linux ubuntu render
3D scenes in trinity: `-Dprism.forceGPU=true`

For the JPackage native executable you can update a cfg file under the
trinity/app folder called Trinity.cfg It is here you can add the forceGPU flag
as another option on its own line. This basically forces Ubuntu to do the GPU
voodoo. After making this change simply run trinity and 3D scenes should work.
